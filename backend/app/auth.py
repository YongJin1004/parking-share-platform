from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from passlib.context import CryptContext
from jose import JWTError, jwt
from datetime import datetime, timedelta
import os
from dotenv import load_dotenv

from app.database import get_db
from app.models import User
from app.schemas import (
    UserRegister, UserLogin, Token, UserResponse,
    UserRegisterWithCert, CertificationVerifyRequest, CertificationVerifyResponse
)
from app.portone import get_certification_info, format_phone_number

load_dotenv()

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

SECRET_KEY = os.getenv("SECRET_KEY", "your-secret-key-change-this-in-production")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24  # 24시간


def hash_password(password: str) -> str:
    return pwd_context.hash(password)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)


def create_access_token(data: dict) -> str:
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt


@router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
def register(user_data: UserRegister, db: Session = Depends(get_db)):
    # 이메일 중복 체크
    existing_user = db.query(User).filter(User.email == user_data.email).first()
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email already registered"
        )

    # 새 사용자 생성
    new_user = User(
        email=user_data.email,
        password_hash=hash_password(user_data.password),
        name=user_data.name,
        phone=user_data.phone
    )

    db.add(new_user)
    db.commit()
    db.refresh(new_user)

    return new_user


@router.post("/login", response_model=Token)
def login(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    # OAuth2PasswordRequestForm은 username과 password 필드를 사용합니다
    # 여기서는 username에 email을 사용합니다
    print(f"=== 로그인 시도 ===")
    print(f"입력된 이메일: '{form_data.username}'")

    # DB에 있는 모든 사용자 확인
    all_users = db.query(User).all()
    print(f"DB에 저장된 사용자 수: {len(all_users)}")
    for u in all_users:
        print(f"  - '{u.email}' (id={u.id})")

    user = db.query(User).filter(User.email == form_data.username).first()
    if not user:
        print(f"사용자를 찾을 수 없음: {form_data.username}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
            headers={"WWW-Authenticate": "Bearer"}
        )

    print(f"사용자 찾음: {user.email}, password_hash 존재: {bool(user.password_hash)}")

    # 비밀번호 검증
    password_valid = verify_password(form_data.password, user.password_hash)
    print(f"비밀번호 검증 결과: {password_valid}")

    if not password_valid:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
            headers={"WWW-Authenticate": "Bearer"}
        )

    # 계정 상태 확인
    if user.status != "active":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Account is not active"
        )

    # JWT 토큰 생성
    access_token = create_access_token(data={"sub": user.email, "user_id": user.id})

    return {"access_token": access_token, "token_type": "bearer"}


@router.post("/verify-certification", response_model=CertificationVerifyResponse)
async def verify_certification(request: CertificationVerifyRequest):
    """
    Portone 본인인증 검증
    - Android에서 본인인증 완료 후 imp_uid 전송
    - Portone API로 인증 정보 조회 후 반환
    """
    cert_info = await get_certification_info(request.imp_uid)

    if not cert_info.get("certified"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="본인인증이 완료되지 않았습니다"
        )

    # 전화번호 포맷팅 (01012345678 → 010-1234-5678)
    formatted_phone = format_phone_number(cert_info["phone"])

    return CertificationVerifyResponse(
        name=cert_info["name"],
        phone=formatted_phone,
        certified=True
    )


@router.post("/register-with-cert", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def register_with_certification(user_data: UserRegisterWithCert, db: Session = Depends(get_db)):
    """
    본인인증 후 회원가입
    - imp_uid로 인증 정보 조회
    - 이름/전화번호는 인증 정보에서 자동 입력
    - phone_verified = True로 설정
    """
    # 1. 본인인증 정보 조회
    cert_info = await get_certification_info(user_data.imp_uid)

    if not cert_info.get("certified"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="본인인증이 완료되지 않았습니다"
        )

    # 2. 이메일 중복 체크
    existing_user = db.query(User).filter(User.email == user_data.email).first()
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email already registered"
        )

    # 3. 전화번호 포맷팅
    formatted_phone = format_phone_number(cert_info["phone"])

    # 4. 전화번호 중복 체크
    existing_phone = db.query(User).filter(User.phone == formatted_phone).first()
    if existing_phone:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Phone number already registered"
        )

    # 5. 새 사용자 생성 (인증된 이름/전화번호 사용)
    new_user = User(
        email=user_data.email,
        password_hash=hash_password(user_data.password),
        name=cert_info["name"],
        phone=formatted_phone,
        phone_verified=True  # 본인인증 완료
    )

    db.add(new_user)
    db.commit()
    db.refresh(new_user)

    return new_user

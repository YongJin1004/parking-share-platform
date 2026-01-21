import os
import shutil
import uuid
from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File
from sqlalchemy.orm import Session
from typing import List

from app.database import get_db
from app.models import User, ParkingSpace, Reservation
from app.schemas import ParkingSpaceCreate, ParkingSpaceUpdate, ParkingSpaceResponse, ParkingSpaceSearchRequest
from app.dependencies import get_current_user

router = APIRouter(prefix="/api/v1/parking-spaces", tags=["parking-spaces"])


@router.get("", response_model=List[ParkingSpaceResponse])
def get_my_parking_spaces(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """내 주차 공간 목록 조회 (Host)"""
    spaces = db.query(ParkingSpace).filter(ParkingSpace.host_id == current_user.id).all()
    return spaces


@router.post("", response_model=ParkingSpaceResponse, status_code=status.HTTP_201_CREATED)
def create_parking_space(
    space_data: ParkingSpaceCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """주차 공간 등록 (Host)"""
    # 새 주차 공간 생성
    schedule = [item.model_dump() for item in space_data.available_schedule] if space_data.available_schedule else None
    title = space_data.title if space_data.title else space_data.address[:50]

    if space_data.hourly_rate is not None:
        hourly_rate = space_data.hourly_rate
    elif space_data.available_schedule:
        hourly_rate = min(item.hourly_rate for item in space_data.available_schedule)
    else:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="hourly_rate 또는 운영 스케줄이 필요합니다.")

    new_space = ParkingSpace(
        host_id=current_user.id,
        title=title,
        address=space_data.address,
        hourly_rate=hourly_rate,
        description=space_data.description,
        is_available=space_data.is_available,
        available_schedule=schedule,
        allowed_vehicle_types=space_data.allowed_vehicle_types
    )

    db.add(new_space)
    db.commit()
    db.refresh(new_space)

    return new_space


@router.patch("/{space_id}", response_model=ParkingSpaceResponse)
def update_parking_space(
    space_id: int,
    space_data: ParkingSpaceUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """주차 공간 수정"""
    # 주차 공간 조회 및 권한 확인
    space = db.query(ParkingSpace).filter(ParkingSpace.id == space_id).first()
    if not space:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Parking space not found"
        )
    if space.host_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not authorized to update this parking space"
        )

    # 업데이트
    if space_data.title:
        space.title = space_data.title
    if space_data.address:
        space.address = space_data.address
    if space_data.hourly_rate is not None:
        space.hourly_rate = space_data.hourly_rate
    if space_data.description is not None:
        space.description = space_data.description
    if space_data.is_available is not None:
        space.is_available = space_data.is_available

    db.commit()
    db.refresh(space)

    return space


@router.delete("/{space_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_parking_space(
    space_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """주차 공간 삭제"""
    # 주차 공간 조회 및 권한 확인
    space = db.query(ParkingSpace).filter(ParkingSpace.id == space_id).first()
    if not space:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Parking space not found"
        )
    if space.host_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not authorized to delete this parking space"
        )

    # 진행중인 예약이 있는지 확인
    active_reservation = db.query(Reservation).filter(
        Reservation.parking_space_id == space_id,
        Reservation.status.in_(["confirmed", "pending"])
    ).first()
    if active_reservation:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot delete parking space with active reservations"
        )

    db.delete(space)
    db.commit()

    return None


@router.get("/search", response_model=List[ParkingSpaceResponse])
def search_parking_spaces(
    keyword: str = None,
    min_hourly_rate: int = None,
    max_hourly_rate: int = None,
    is_available: bool = True,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """주차 공간 검색 (Guest - 키워드/가격 필터)"""
    query = db.query(ParkingSpace)

    # 예약 가능 여부 필터
    if is_available is not None:
        query = query.filter(ParkingSpace.is_available == is_available)

    # 키워드 검색 (제목 또는 주소)
    if keyword:
        query = query.filter(
            (ParkingSpace.title.contains(keyword)) |
            (ParkingSpace.address.contains(keyword))
        )

    # 최소 가격 필터
    if min_hourly_rate is not None:
        query = query.filter(ParkingSpace.hourly_rate >= min_hourly_rate)

    # 최대 가격 필터
    if max_hourly_rate is not None:
        query = query.filter(ParkingSpace.hourly_rate <= max_hourly_rate)

    # 가격 오름차순 정렬
    query = query.order_by(ParkingSpace.hourly_rate.asc())

    spaces = query.all()
    return spaces


@router.post("/{space_id}/images", response_model=ParkingSpaceResponse)
def upload_parking_space_images(
    space_id: int,
    images: List[UploadFile] = File(...),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """주차 공간 이미지 업로드"""
    space = db.query(ParkingSpace).filter(ParkingSpace.id == space_id).first()
    if not space:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Parking space not found")
    if space.host_id != current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorized")

    upload_dir = f"uploads/parking_spaces/{space_id}"
    os.makedirs(upload_dir, exist_ok=True)

    saved_paths = list(space.images or [])
    for image in images:
        ext = os.path.splitext(image.filename)[1] if image.filename else ".jpg"
        filename = f"{uuid.uuid4().hex}{ext}"
        file_path = f"{upload_dir}/{filename}"
        with open(file_path, "wb") as f:
            shutil.copyfileobj(image.file, f)
        saved_paths.append(f"/{file_path}")

    space.images = saved_paths
    db.commit()
    db.refresh(space)
    return space

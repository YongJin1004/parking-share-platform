import os
from fastapi import FastAPI, Depends
from fastapi.staticfiles import StaticFiles
from sqlalchemy.orm import Session
from typing import List
from app.auth import router as auth_router
from app.vehicles import router as vehicles_router
from app.parking_spaces import router as parking_spaces_router
from app.reservations import router as reservations_router
from app.database import engine, Base, SessionLocal
from app.models import User
from app.schemas import UserResponse

# DB 테이블 생성
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Parking Share Platform API", version="1.0.0")

# 업로드 파일 정적 서빙
os.makedirs("uploads", exist_ok=True)
app.mount("/uploads", StaticFiles(directory="uploads"), name="uploads")

# 라우터 등록
app.include_router(auth_router)
app.include_router(vehicles_router)
app.include_router(parking_spaces_router)
app.include_router(reservations_router)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@app.get("/users", response_model=List[UserResponse])
def get_users(db: Session = Depends(get_db)):
    return db.query(User).all()


@app.get("/")
async def root():
    return {"message": "Parking Share Platform API", "version": "1.0.0"}

from fastapi import FastAPI
from app.auth import router as auth_router
from app.vehicles import router as vehicles_router
from app.parking_spaces import router as parking_spaces_router
from app.reservations import router as reservations_router
from app.database import engine, Base

# DB 테이블 생성
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Parking Share Platform API", version="1.0.0")

# 라우터 등록
app.include_router(auth_router)
app.include_router(vehicles_router)
app.include_router(parking_spaces_router)
app.include_router(reservations_router)


@app.get("/")
async def root():
    return {"message": "Parking Share Platform API", "version": "1.0.0"}

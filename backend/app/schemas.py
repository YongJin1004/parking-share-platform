from pydantic import BaseModel, EmailStr, Field
from typing import Optional, List
from datetime import datetime
from enum import Enum


# User Schemas
class UserRegister(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=8)
    name: str = Field(..., min_length=2, max_length=100)
    phone: str = Field(..., pattern=r"^010-\d{4}-\d{4}$")


# 본인인증 후 회원가입 (이름/전화번호는 인증 정보에서 자동 입력)
class UserRegisterWithCert(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=8)
    imp_uid: str  # Portone 본인인증 UID (필수)


# 본인인증 검증 요청
class CertificationVerifyRequest(BaseModel):
    imp_uid: str


# 본인인증 검증 응답
class CertificationVerifyResponse(BaseModel):
    name: str
    phone: str
    certified: bool


class UserLogin(BaseModel):
    email: EmailStr
    password: str


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"


class UserResponse(BaseModel):
    id: int
    email: str
    name: str
    phone: str
    phone_verified: bool
    manner_score: int
    total_reviews: int
    status: str

    class Config:
        from_attributes = True


# Vehicle Schemas
class VehicleType(str, Enum):
    SEDAN = "sedan"
    SUV = "suv"
    VAN = "van"
    TRUCK = "truck"
    MOTORCYCLE = "motorcycle"


class VehicleCreate(BaseModel):
    vehicle_number: str = Field(..., min_length=7, max_length=20)
    vehicle_type: VehicleType
    nickname: Optional[str] = Field(None, max_length=50)
    is_default: bool = False


class VehicleUpdate(BaseModel):
    vehicle_number: Optional[str] = Field(None, min_length=7, max_length=20)
    vehicle_type: Optional[VehicleType] = None
    nickname: Optional[str] = Field(None, max_length=50)


class VehicleResponse(BaseModel):
    id: int
    user_id: int
    vehicle_number: str
    vehicle_type: VehicleType
    nickname: Optional[str]
    is_default: bool
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


# ParkingSpace Schemas
class ScheduleItem(BaseModel):
    date: str        # "2026-05-20" (specific date)
    start_time: str  # "09:00"
    end_time: str    # "20:00"
    hourly_rate: int = Field(..., gt=0)


class ParkingSpaceCreate(BaseModel):
    title: Optional[str] = Field(None, max_length=100)
    address: str = Field(..., min_length=5, max_length=255)
    hourly_rate: Optional[int] = Field(None, gt=0)
    description: Optional[str] = Field(None, max_length=500)
    is_available: bool = True
    available_schedule: Optional[List[ScheduleItem]] = None
    allowed_vehicle_types: Optional[List[str]] = None


class ParkingSpaceUpdate(BaseModel):
    title: Optional[str] = Field(None, min_length=2, max_length=100)
    address: Optional[str] = Field(None, min_length=5, max_length=255)
    hourly_rate: Optional[int] = Field(None, gt=0)
    description: Optional[str] = Field(None, max_length=500)
    is_available: Optional[bool] = None
    available_schedule: Optional[List[ScheduleItem]] = None
    allowed_vehicle_types: Optional[List[str]] = None


class ParkingSpaceResponse(BaseModel):
    id: int
    host_id: int
    title: str
    address: str
    latitude: Optional[str]
    longitude: Optional[str]
    hourly_rate: int
    description: Optional[str]
    is_available: bool
    available_schedule: Optional[List[ScheduleItem]] = None
    allowed_vehicle_types: Optional[List[str]] = None
    images: Optional[List[str]] = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class ParkingSpaceSearchRequest(BaseModel):
    keyword: Optional[str] = None  # 주소 또는 제목 검색
    min_hourly_rate: Optional[int] = Field(None, ge=0)
    max_hourly_rate: Optional[int] = Field(None, ge=0)
    is_available: Optional[bool] = True  # 기본: 예약 가능한 것만


# Reservation Schemas
class ReservationStatus(str, Enum):
    PENDING = "pending"
    CONFIRMED = "confirmed"
    CANCELLED = "cancelled"
    COMPLETED = "completed"


class ReservationCreate(BaseModel):
    parking_space_id: int
    vehicle_id: Optional[int] = None


class ReservationResponse(BaseModel):
    id: int
    guest_id: int
    parking_space_id: int
    vehicle_id: Optional[int]
    status: ReservationStatus
    total_amount: int
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True

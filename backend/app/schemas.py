from pydantic import BaseModel, EmailStr, Field
from typing import Optional
from datetime import datetime
from enum import Enum


# User Schemas
class UserRegister(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=8)
    name: str = Field(..., min_length=2, max_length=100)
    phone: str = Field(..., pattern=r"^010-\d{4}-\d{4}$")


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
class ParkingSpaceCreate(BaseModel):
    title: str = Field(..., min_length=2, max_length=100)
    address: str = Field(..., min_length=5, max_length=255)
    hourly_rate: int = Field(..., gt=0)
    description: Optional[str] = Field(None, max_length=500)
    is_available: bool = True


class ParkingSpaceUpdate(BaseModel):
    title: Optional[str] = Field(None, min_length=2, max_length=100)
    address: Optional[str] = Field(None, min_length=5, max_length=255)
    hourly_rate: Optional[int] = Field(None, gt=0)
    description: Optional[str] = Field(None, max_length=500)
    is_available: Optional[bool] = None


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

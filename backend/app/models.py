from sqlalchemy import Column, BigInteger, String, Boolean, Integer, TIMESTAMP, ForeignKey, Enum, func
from sqlalchemy.orm import relationship
from app.database import Base
import enum


class User(Base):
    __tablename__ = "users"

    id = Column(BigInteger, primary_key=True, index=True, autoincrement=True)
    email = Column(String(255), unique=True, index=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    name = Column(String(100), nullable=False)
    phone = Column(String(20), nullable=False)
    phone_verified = Column(Boolean, default=False, nullable=False)
    manner_score = Column(Integer, default=50, nullable=False)
    total_reviews = Column(Integer, default=0, nullable=False)
    status = Column(String(20), default="active", nullable=False)
    created_at = Column(TIMESTAMP, server_default=func.now(), nullable=False)
    updated_at = Column(TIMESTAMP, server_default=func.now(), onupdate=func.now(), nullable=False)

    vehicles = relationship("Vehicle", back_populates="owner")


class VehicleTypeEnum(str, enum.Enum):
    SEDAN = "sedan"
    SUV = "suv"
    VAN = "van"
    TRUCK = "truck"
    MOTORCYCLE = "motorcycle"


class Vehicle(Base):
    __tablename__ = "vehicles"

    id = Column(BigInteger, primary_key=True, index=True, autoincrement=True)
    user_id = Column(BigInteger, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    vehicle_number = Column(String(20), unique=True, index=True, nullable=False)
    vehicle_type = Column(Enum(VehicleTypeEnum), nullable=False)
    nickname = Column(String(50), nullable=True)
    is_default = Column(Boolean, default=False, nullable=False)
    created_at = Column(TIMESTAMP, server_default=func.now(), nullable=False)
    updated_at = Column(TIMESTAMP, server_default=func.now(), onupdate=func.now(), nullable=False)

    owner = relationship("User", back_populates="vehicles")

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
    reservations = relationship("Reservation", back_populates="vehicle")


class ParkingSpace(Base):
    __tablename__ = "parking_spaces"

    id = Column(BigInteger, primary_key=True, index=True, autoincrement=True)
    host_id = Column(BigInteger, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    title = Column(String(100), nullable=False)
    address = Column(String(255), nullable=False)
    latitude = Column(String(20), nullable=True)
    longitude = Column(String(20), nullable=True)
    hourly_rate = Column(Integer, nullable=False)
    description = Column(String(500), nullable=True)
    is_available = Column(Boolean, default=True, nullable=False)
    created_at = Column(TIMESTAMP, server_default=func.now(), nullable=False)
    updated_at = Column(TIMESTAMP, server_default=func.now(), onupdate=func.now(), nullable=False)

    host = relationship("User", backref="parking_spaces")
    reservations = relationship("Reservation", back_populates="parking_space")


class ReservationStatusEnum(str, enum.Enum):
    PENDING = "pending"
    CONFIRMED = "confirmed"
    CANCELLED = "cancelled"
    COMPLETED = "completed"


class Reservation(Base):
    __tablename__ = "reservations"

    id = Column(BigInteger, primary_key=True, index=True, autoincrement=True)
    guest_id = Column(BigInteger, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    parking_space_id = Column(BigInteger, ForeignKey("parking_spaces.id", ondelete="CASCADE"), nullable=False)
    vehicle_id = Column(BigInteger, ForeignKey("vehicles.id", ondelete="SET NULL"), nullable=True)
    total_amount = Column(Integer, nullable=False)
    status = Column(Enum(ReservationStatusEnum), default=ReservationStatusEnum.CONFIRMED, nullable=False)
    created_at = Column(TIMESTAMP, server_default=func.now(), nullable=False)
    updated_at = Column(TIMESTAMP, server_default=func.now(), onupdate=func.now(), nullable=False)

    guest = relationship("User", backref="reservations")
    parking_space = relationship("ParkingSpace", back_populates="reservations")
    vehicle = relationship("Vehicle", back_populates="reservations")

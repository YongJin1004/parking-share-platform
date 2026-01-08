from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from app.database import get_db
from app.models import User, Vehicle, Reservation
from app.schemas import VehicleCreate, VehicleUpdate, VehicleResponse
from app.dependencies import get_current_user

router = APIRouter(prefix="/api/v1/vehicles", tags=["vehicles"])


@router.get("", response_model=List[VehicleResponse])
def get_my_vehicles(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    vehicles = db.query(Vehicle).filter(Vehicle.user_id == current_user.id).all()
    return vehicles


@router.post("", response_model=VehicleResponse, status_code=status.HTTP_201_CREATED)
def create_vehicle(
    vehicle_data: VehicleCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    # 차량번호 중복 체크
    existing_vehicle = db.query(Vehicle).filter(
        Vehicle.vehicle_number == vehicle_data.vehicle_number
    ).first()
    if existing_vehicle:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Vehicle number already registered"
        )

    # 기본 차량 설정 시 기존 기본 차량 해제
    if vehicle_data.is_default:
        db.query(Vehicle).filter(
            Vehicle.user_id == current_user.id,
            Vehicle.is_default == True
        ).update({"is_default": False})

    # 새 차량 생성
    new_vehicle = Vehicle(
        user_id=current_user.id,
        vehicle_number=vehicle_data.vehicle_number,
        vehicle_type=vehicle_data.vehicle_type,
        nickname=vehicle_data.nickname,
        is_default=vehicle_data.is_default
    )

    db.add(new_vehicle)
    db.commit()
    db.refresh(new_vehicle)

    return new_vehicle


@router.patch("/{vehicle_id}", response_model=VehicleResponse)
def update_vehicle(
    vehicle_id: int,
    vehicle_data: VehicleUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    # 차량 조회 및 권한 확인
    vehicle = db.query(Vehicle).filter(Vehicle.id == vehicle_id).first()
    if not vehicle:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Vehicle not found"
        )
    if vehicle.user_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not authorized to update this vehicle"
        )

    # 차량번호 변경 시 중복 체크
    if vehicle_data.vehicle_number and vehicle_data.vehicle_number != vehicle.vehicle_number:
        existing_vehicle = db.query(Vehicle).filter(
            Vehicle.vehicle_number == vehicle_data.vehicle_number
        ).first()
        if existing_vehicle:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Vehicle number already registered"
            )
        vehicle.vehicle_number = vehicle_data.vehicle_number

    # 업데이트
    if vehicle_data.vehicle_type:
        vehicle.vehicle_type = vehicle_data.vehicle_type
    if vehicle_data.nickname is not None:
        vehicle.nickname = vehicle_data.nickname

    db.commit()
    db.refresh(vehicle)

    return vehicle


@router.delete("/{vehicle_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_vehicle(
    vehicle_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    # 차량 조회 및 권한 확인
    vehicle = db.query(Vehicle).filter(Vehicle.id == vehicle_id).first()
    if not vehicle:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Vehicle not found"
        )
    if vehicle.user_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not authorized to delete this vehicle"
        )

    # 진행중인 예약이 있는지 확인
    active_reservation = db.query(Reservation).filter(
        Reservation.vehicle_id == vehicle_id,
        Reservation.status.in_(["confirmed", "pending"])
    ).first()
    if active_reservation:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot delete vehicle with active reservations"
        )

    db.delete(vehicle)
    db.commit()

    return None


@router.patch("/{vehicle_id}/set-default", response_model=VehicleResponse)
def set_default_vehicle(
    vehicle_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    # 차량 조회 및 권한 확인
    vehicle = db.query(Vehicle).filter(Vehicle.id == vehicle_id).first()
    if not vehicle:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Vehicle not found"
        )
    if vehicle.user_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not authorized to set this vehicle as default"
        )

    # 기존 기본 차량 해제
    db.query(Vehicle).filter(
        Vehicle.user_id == current_user.id,
        Vehicle.is_default == True
    ).update({"is_default": False})

    # 새 기본 차량 설정
    vehicle.is_default = True
    db.commit()
    db.refresh(vehicle)

    return vehicle

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from app.database import get_db
from app.models import User, Reservation, ParkingSpace, Vehicle
from app.schemas import ReservationCreate, ReservationResponse
from app.dependencies import get_current_user
from app.utils import update_manner_score

router = APIRouter(prefix="/api/v1/reservations", tags=["reservations"])


@router.get("", response_model=List[ReservationResponse])
def get_my_reservations(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """내 예약 목록 조회 (Guest)"""
    reservations = db.query(Reservation).filter(Reservation.guest_id == current_user.id).all()
    return reservations


@router.post("", response_model=ReservationResponse, status_code=status.HTTP_201_CREATED)
def create_reservation(
    reservation_data: ReservationCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """예약 생성 (Guest)"""
    # 주차 공간 존재 확인
    parking_space = db.query(ParkingSpace).filter(
        ParkingSpace.id == reservation_data.parking_space_id
    ).first()
    if not parking_space:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Parking space not found"
        )

    # 예약 가능 여부 확인
    if not parking_space.is_available:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Parking space is not available"
        )

    # 자기 자신의 주차 공간 예약 방지
    if parking_space.host_id == current_user.id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot reserve your own parking space"
        )

    # 차량 ID 제공 시 소유권 확인
    if reservation_data.vehicle_id:
        vehicle = db.query(Vehicle).filter(
            Vehicle.id == reservation_data.vehicle_id,
            Vehicle.user_id == current_user.id
        ).first()
        if not vehicle:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Vehicle not found or not owned by you"
            )

    # 예약 중복 방지: 동일 주차 공간에 진행중인 예약 확인
    existing_reservation = db.query(Reservation).filter(
        Reservation.parking_space_id == reservation_data.parking_space_id,
        Reservation.status.in_(["confirmed", "pending"])
    ).first()
    if existing_reservation:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="This parking space already has an active reservation"
        )

    # 예약 생성 (최소 버전: 1시간 요금)
    new_reservation = Reservation(
        guest_id=current_user.id,
        parking_space_id=reservation_data.parking_space_id,
        vehicle_id=reservation_data.vehicle_id,
        total_amount=parking_space.hourly_rate,
        status="confirmed"
    )

    db.add(new_reservation)
    db.commit()
    db.refresh(new_reservation)

    return new_reservation


@router.delete("/{reservation_id}", status_code=status.HTTP_204_NO_CONTENT)
def cancel_reservation(
    reservation_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """예약 취소 (Guest)"""
    # 예약 조회 및 권한 확인
    reservation = db.query(Reservation).filter(Reservation.id == reservation_id).first()
    if not reservation:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Reservation not found"
        )
    if reservation.guest_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not authorized to cancel this reservation"
        )

    # 이미 취소된 예약
    if reservation.status == "cancelled":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Reservation already cancelled"
        )

    # 완료된 예약은 취소 불가
    if reservation.status == "completed":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot cancel completed reservation"
        )

    # 예약 취소 (매너 점수 감소)
    reservation.status = "cancelled"

    # 매너 점수 감소 (취소한 Guest)
    update_manner_score(db, current_user.id, -2)

    db.commit()

    return None


@router.patch("/{reservation_id}/complete", response_model=ReservationResponse)
def complete_reservation(
    reservation_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """예약 완료 처리 (Host or Guest)"""
    # 예약 조회
    reservation = db.query(Reservation).filter(Reservation.id == reservation_id).first()
    if not reservation:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Reservation not found"
        )

    # 권한 확인: Host 또는 Guest만 완료 처리 가능
    parking_space = db.query(ParkingSpace).filter(
        ParkingSpace.id == reservation.parking_space_id
    ).first()

    is_host = parking_space.host_id == current_user.id
    is_guest = reservation.guest_id == current_user.id

    if not (is_host or is_guest):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not authorized to complete this reservation"
        )

    # 이미 완료된 예약
    if reservation.status == "completed":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Reservation already completed"
        )

    # 취소된 예약은 완료 불가
    if reservation.status == "cancelled":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot complete cancelled reservation"
        )

    # 예약 완료 처리
    reservation.status = "completed"

    # 매너 점수 증가 (Guest와 Host 모두)
    update_manner_score(db, reservation.guest_id, 1)
    update_manner_score(db, parking_space.host_id, 1)

    db.commit()
    db.refresh(reservation)

    return reservation

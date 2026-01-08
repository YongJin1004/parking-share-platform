# 데이터 검증 규칙

> MSA 환경에서 중요한 비즈니스 검증 로직 정리

## 개요
- **목적**: 데이터 무결성 보장 및 비즈니스 규칙 강제
- **위치**: 백엔드 API 엔드포인트 레벨에서 검증
- **구현**: FastAPI + SQLAlchemy + Pydantic

---

## 1. User (사용자)

### 회원가입 검증
| 필드 | 검증 규칙 | 구현 |
|------|----------|------|
| email | EmailStr (RFC 5322) | Pydantic ✅ |
| password | 최소 8자 이상 | Pydantic Field ✅ |
| name | 2~100자 | Pydantic Field ✅ |
| phone | `010-XXXX-XXXX` 형식 | Pydantic regex ✅ |

### 로그인 검증
- email 존재 여부 확인 (DB 조회)
- password_hash bcrypt 검증

---

## 2. Vehicle (차량)

### 생성 검증 (`POST /vehicles`)
| 검증 항목 | 규칙 | 에러 메시지 | 구현 |
|----------|------|-------------|------|
| vehicle_number 중복 | DB에서 UNIQUE 확인 | "Vehicle number already registered" | ✅ |
| vehicle_number 형식 | 7~20자 | Pydantic Field | ✅ |
| vehicle_type | ENUM 값만 허용 | "Invalid vehicle type" | ✅ |
| is_default=True | 기존 기본 차량 해제 | - | ✅ |

### 수정 검증 (`PATCH /vehicles/{id}`)
| 검증 항목 | 규칙 | 에러 메시지 | 구현 |
|----------|------|-------------|------|
| 소유권 확인 | vehicle.user_id == current_user.id | "Not authorized to update this vehicle" | ✅ |
| vehicle_number 변경 시 중복 | 다른 차량과 중복 확인 | "Vehicle number already registered" | ✅ |

### 삭제 검증 (`DELETE /vehicles/{id}`)
| 검증 항목 | 규칙 | 에러 메시지 | 구현 |
|----------|------|-------------|------|
| 소유권 확인 | vehicle.user_id == current_user.id | "Not authorized to delete this vehicle" | ✅ |
| 진행중인 예약 확인 | status IN (confirmed, pending) | "Cannot delete vehicle with active reservations" | ✅ |

**코드 위치**: `backend/app/vehicles.py:124-133`

---

## 3. ParkingSpace (주차 공간)

### 생성 검증 (`POST /parking-spaces`)
| 검증 항목 | 규칙 | 에러 메시지 | 구현 |
|----------|------|-------------|------|
| title | 2~100자 | Pydantic Field | ✅ |
| address | 5~255자 | Pydantic Field | ✅ |
| hourly_rate | >0 (1원 이상) | "Input should be greater than 0" | ✅ |
| description | 최대 500자 | Pydantic Field | ✅ |

### 수정 검증 (`PATCH /parking-spaces/{id}`)
| 검증 항목 | 규칙 | 에러 메시지 | 구현 |
|----------|------|-------------|------|
| 소유권 확인 | space.host_id == current_user.id | "Not authorized to update this parking space" | ✅ |
| hourly_rate | >0 (변경 시) | Pydantic Field | ✅ |

### 삭제 검증 (`DELETE /parking-spaces/{id}`)
| 검증 항목 | 규칙 | 에러 메시지 | 구현 |
|----------|------|-------------|------|
| 소유권 확인 | space.host_id == current_user.id | "Not authorized to delete this parking space" | ✅ |
| 진행중인 예약 확인 | status IN (confirmed, pending) | "Cannot delete parking space with active reservations" | ✅ |

**코드 위치**: `backend/app/parking_spaces.py:107-116`

---

## 4. Reservation (예약)

### 생성 검증 (`POST /reservations`)
| 검증 항목 | 규칙 | 에러 메시지 | 구현 |
|----------|------|-------------|------|
| ParkingSpace 존재 | DB 조회 | "Parking space not found" | ✅ |
| is_available | parking_space.is_available == True | "Parking space is not available" | ✅ |
| 자기 공간 예약 방지 | parking_space.host_id != current_user.id | "Cannot reserve your own parking space" | ✅ |
| Vehicle 소유권 | vehicle.user_id == current_user.id | "Vehicle not found or not owned by you" | ✅ |
| 예약 중복 방지 | 동일 space에 confirmed/pending 없음 | "This parking space already has an active reservation" | ✅ |

**코드 위치**: `backend/app/reservations.py:66-75`

### 취소 검증 (`DELETE /reservations/{id}`)
| 검증 항목 | 규칙 | 에러 메시지 | 구현 |
|----------|------|-------------|------|
| 소유권 확인 | reservation.guest_id == current_user.id | "Not authorized to cancel this reservation" | ✅ |
| 이미 취소됨 | status != "cancelled" | "Reservation already cancelled" | ✅ |
| 이미 완료됨 | status != "completed" | "Cannot cancel completed reservation" | ✅ |

**부가 로직**: 매너 점수 -2점 (Guest)

### 완료 검증 (`PATCH /reservations/{id}/complete`)
| 검증 항목 | 규칙 | 에러 메시지 | 구현 |
|----------|------|-------------|------|
| 권한 확인 | Host OR Guest | "Not authorized to complete this reservation" | ✅ |
| 이미 완료됨 | status != "completed" | "Reservation already completed" | ✅ |
| 이미 취소됨 | status != "cancelled" | "Cannot complete cancelled reservation" | ✅ |

**부가 로직**: 매너 점수 +1점 (Guest), +1점 (Host)

**코드 위치**: `backend/app/reservations.py:139-192`

---

## 5. 매너 점수 업데이트 로직

### 함수: `update_manner_score()`
**위치**: `backend/app/utils.py:6-18`

| 파라미터 | 설명 |
|---------|------|
| db | 데이터베이스 세션 |
| user_id | 사용자 ID |
| score_change | 점수 변화량 (양수/음수) |

**로직**:
```python
new_score = max(0, min(100, user.manner_score + score_change))
# 범위: 0 ~ 100 자동 클리핑
```

### 적용 시점
| 이벤트 | 점수 변화 | 대상 |
|--------|----------|------|
| 예약 취소 | -2 | Guest |
| 예약 완료 | +1 | Guest, Host 각각 |

---

## 6. 검색 필터 검증

### GET `/parking-spaces/search`
| 파라미터 | 검증 | 기본값 |
|---------|------|--------|
| keyword | Optional[str] | None |
| min_hourly_rate | ge=0 (0 이상) | None |
| max_hourly_rate | ge=0 (0 이상) | None |
| is_available | Optional[bool] | True |

**코드 위치**: `backend/app/parking_spaces.py:153-199`

---

## 7. 인증 검증

### JWT 토큰 검증 (`get_current_user`)
**위치**: `backend/app/dependencies.py:24-52`

| 검증 항목 | 규칙 | 에러 |
|----------|------|------|
| 토큰 디코딩 | jwt.decode() | 401 "Could not validate credentials" |
| payload.sub 존재 | email 필드 필수 | 401 "Could not validate credentials" |
| User 존재 | DB 조회 | 401 "User not found" |

---

## 8. Pydantic 스키마 검증

### UserRegister
```python
email: EmailStr  # RFC 5322
password: str = Field(..., min_length=8)
name: str = Field(..., min_length=2, max_length=100)
phone: str = Field(..., pattern=r"^010-\d{4}-\d{4}$")
```

### VehicleCreate
```python
vehicle_number: str = Field(..., min_length=7, max_length=20)
vehicle_type: VehicleType  # ENUM
nickname: Optional[str] = Field(None, max_length=50)
is_default: bool = False
```

### ParkingSpaceCreate
```python
title: str = Field(..., min_length=2, max_length=100)
address: str = Field(..., min_length=5, max_length=255)
hourly_rate: int = Field(..., gt=0)  # >0 필수
description: Optional[str] = Field(None, max_length=500)
is_available: bool = True
```

### ReservationCreate
```python
parking_space_id: int
vehicle_id: Optional[int] = None
```

---

## 9. 미래 검증 규칙 (future-features.md)

### 날짜/시간 검증
- start_time < end_time
- start_time >= 현재 시간
- 시간 충돌 검증 (같은 주차 공간, 겹치는 시간대)

### 차종 필터링
- allowed_vehicle_types에 vehicle_type 포함 여부

### 10분 이내 취소 검증
- `created_at + 10분 >= 현재 시간`

### 결제 금액 검증
- Portone 결제 금액 == 예상 금액 (위변조 방지)

---

## 참고 문서
- [data-model.md](./data-model.md) - 데이터 모델 및 FK 제약
- [domain-overview.md](./domain-overview.md) - 비즈니스 규칙
- [api-spec.json](./api-spec.json) - API 엔드포인트 명세
- [future-features.md](./future-features.md) - 미래 구현 예정 기능

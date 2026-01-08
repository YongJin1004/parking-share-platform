# 미래 기능 (Future Features)

> 현재는 최소 CRUD만 구현. 프론트엔드 개발 시 또는 실제 사용 시나리오 테스트 시 구현 예정

## 1. 날짜/시간 기반 예약 시스템

### 현재 상태
- `Reservation`: 단순 CRUD (Guest ↔ ParkingSpace 연결만)
- 시간 충돌 검증 없음

### 구현 예정
```python
# ParkingSpace 모델에 추가
available_days = Column(JSON)  # ["mon", "tue", "wed", ...]
available_start_time = Column(Time)  # 09:00
available_end_time = Column(Time)    # 18:00

# Reservation 모델에 추가
start_time = Column(DateTime(timezone=True))
end_time = Column(DateTime(timezone=True))
```

### 비즈니스 로직
- 예약 생성 시 시간 충돌 검증
- Host가 ParkingSpace 등록 시 운영 시간 설정
- Guest 검색 시 날짜/시간 필터

---

## 2. 차종 기반 필터링

### 현재 상태
- `Vehicle.vehicle_type`: ENUM (sedan, suv, van, truck, motorcycle)
- ParkingSpace와 연결 없음

### 구현 예정
```python
# ParkingSpace 모델에 추가
allowed_vehicle_types = Column(JSON)  # ["sedan", "suv"]
max_vehicle_height = Column(Integer)  # 200 (cm)
max_vehicle_width = Column(Integer)   # 180 (cm)
```

### 비즈니스 로직
- Host: "SUV까지만 가능" 설정
- Guest 검색: 내 차량(SUV)이 주차 가능한 곳만 표시
- 예약 생성 시 차종 검증

---

## 3. 카카오맵 통합

### 현재 상태
- `ParkingSpace.latitude/longitude`: NULL 허용
- Kakao API 호출 없음

### 구현 예정
1. **주소 → 좌표 변환**
   - ParkingSpace 생성 시 Kakao Local API 호출
   - latitude/longitude 자동 저장

2. **지도 기반 검색**
   - Guest: 현재 위치 기준 반경 1km 검색
   - 지도에 마커 표시

**참고**: [external-apis.md](./external-apis.md#daum-우편번호-api--kakao-maps)

---

## 4. 결제 시스템 (Portone)

### 현재 상태
- Payment 모델 없음
- Reservation 생성 시 결제 없이 바로 confirmed

### 구현 예정
```python
# Payment 모델 생성
class Payment(Base):
    reservation_id = Column(BigInteger, ForeignKey("reservations.id"))
    imp_uid = Column(String(100))  # Portone 결제 고유번호
    amount = Column(Integer)
    status = Column(Enum("pending", "paid", "cancelled", "refunded"))
```

### 비즈니스 로직
- Reservation 생성 → Portone 결제 사전 등록
- 결제 완료 → Reservation 상태 confirmed
- 10분 이내 취소 → 전액 환불

**참고**: [external-apis.md](./external-apis.md#portone-구-아임포트)

---

## 5. 본인인증 (Portone KG 이니시스)

### 현재 상태
- `User.phone_verified`: 항상 False
- 전화번호 중복 가입 가능

### 구현 예정
- 회원가입 시 Portone 본인인증 필수
- `phone_verified = True` 설정
- 전화번호 중복 방지

---

## 구현 우선순위

1. **Reservation CRUD** ← 현재 작업 중
2. 날짜/시간 기반 예약 (시간 충돌 검증)
3. 차종 필터링
4. 카카오맵 통합
5. 결제 시스템
6. 본인인증

---

## 토큰 절약 전략

- 이 파일은 필요할 때만 읽기
- JSON DSL (api-spec.json, data-model.md) 우선 참조
- 세션 저장 활용 (`/sc:save`)

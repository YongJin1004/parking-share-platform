# 데이터 모델

## 엔티티 관계도

```
User (1) ----< (N) Vehicle
User (1) ----< (N) ParkingSpace (Host)
User (1) ----< (N) Reservation (Guest)

ParkingSpace (1) ----< (N) Reservation
Vehicle (1) ----< (N) Reservation
Reservation (1) ---- (1) Payment
```

## User (사용자)
- **역할**: Host(주차 공간 제공자) + Guest(이용자) 겸용
- **관계**:
  - Vehicle (1:N) - 여러 차량 소유 가능
  - ParkingSpace (1:N) - 여러 주차 공간 등록 가능 (Host)
  - Reservation (1:N) - 여러 예약 생성 가능 (Guest)

### 필드
| 필드 | 타입 | 설명 |
|------|------|------|
| id | BIGINT | PK |
| email | VARCHAR(255) | 이메일 (UNIQUE) |
| password_hash | VARCHAR(255) | bcrypt 해시 |
| name | VARCHAR(100) | 이름 |
| phone | VARCHAR(20) | 전화번호 |
| phone_verified | BOOLEAN | 전화번호 인증 여부 |
| manner_score | INTEGER | 매너 점수 (기본: 50) |
| total_reviews | INTEGER | 총 리뷰 수 |
| status | VARCHAR(20) | 계정 상태 (active/suspended) |
| created_at | TIMESTAMP | 생성일 |
| updated_at | TIMESTAMP | 수정일 |

## Vehicle (차량)
- **소유자**: User
- **용도**: 예약 시 차량 정보 제공

### 필드
| 필드 | 타입 | 설명 |
|------|------|------|
| id | BIGINT | PK |
| user_id | BIGINT | FK (User) |
| vehicle_number | VARCHAR(20) | 차량번호 (UNIQUE) |
| vehicle_type | ENUM | sedan/suv/van/truck/motorcycle |
| nickname | VARCHAR(50) | 차량 별명 (nullable) |
| is_default | BOOLEAN | 기본 차량 여부 |
| created_at | TIMESTAMP | 생성일 |
| updated_at | TIMESTAMP | 수정일 |

### 비즈니스 규칙
- 한 User는 여러 Vehicle 소유 가능
- is_default는 User당 하나만 true 가능
- 차량번호는 전체에서 유니크

## ParkingSpace (주차 공간)
- **등록자**: User (Host 역할)
- **용도**: Guest가 검색 및 예약
- **상태**: 구현 완료 (최소 버전)

### 필드
| 필드 | 타입 | 설명 | 상태 |
|------|------|------|------|
| id | BIGINT | PK | ✅ |
| host_id | BIGINT | FK (User) - Host | ✅ |
| title | VARCHAR(100) | 주차 공간 제목 | ✅ |
| address | VARCHAR(255) | 주소 | ✅ |
| latitude | VARCHAR(20) | 위도 (Kakao API - NULL 허용) | ✅ |
| longitude | VARCHAR(20) | 경도 (Kakao API - NULL 허용) | ✅ |
| hourly_rate | INTEGER | 시간당 요금 (원, >0 필수) | ✅ |
| description | VARCHAR(500) | 상세 설명 (nullable) | ✅ |
| is_available | BOOLEAN | 현재 예약 가능 여부 | ✅ |
| created_at | TIMESTAMP | 생성일 | ✅ |
| updated_at | TIMESTAMP | 수정일 | ✅ |

### 비즈니스 규칙
- hourly_rate는 1원 이상 필수 (무료 주차 불가)
- 진행중인 예약(confirmed/pending)이 있으면 삭제 불가
- latitude/longitude는 현재 NULL (future: Kakao API 연동)

### 미래 필드 (future-features.md 참조)
- allowed_vehicle_types (JSON) - 허용 차종 목록
- images (JSON) - 이미지 URL 배열
- available_days (JSON) - 운영 요일
- available_start_time (TIME) - 운영 시작 시간
- available_end_time (TIME) - 운영 종료 시간

## Reservation (예약)
- **예약자**: User (Guest)
- **대상**: ParkingSpace
- **차량**: Vehicle (optional)
- **상태**: 구현 완료 (최소 버전 - 결제 없음)

### 필드
| 필드 | 타입 | 설명 | 상태 |
|------|------|------|------|
| id | BIGINT | PK | ✅ |
| guest_id | BIGINT | FK (User) - Guest | ✅ |
| parking_space_id | BIGINT | FK (ParkingSpace) | ✅ |
| vehicle_id | BIGINT | FK (Vehicle) - NULL 허용 | ✅ |
| total_amount | INTEGER | 총 금액 (1시간 요금) | ✅ |
| status | ENUM | pending/confirmed/cancelled/completed | ✅ |
| created_at | TIMESTAMP | 예약 생성일 | ✅ |
| updated_at | TIMESTAMP | 수정일 | ✅ |

### 비즈니스 규칙 (구현 완료)
1. **예약 생성 시**:
   - 자기 자신의 주차 공간 예약 불가
   - 동일 주차 공간에 진행중인 예약(confirmed/pending) 중복 불가
   - vehicle_id 제공 시 소유권 확인
   - is_available=false인 주차 공간 예약 불가
   - total_amount는 parking_space.hourly_rate 값 사용

2. **예약 취소 시** (DELETE):
   - Guest만 취소 가능
   - 이미 취소/완료된 예약 취소 불가
   - 매너 점수 -2 (Guest)

3. **예약 완료 시** (PATCH /complete):
   - Host 또는 Guest만 완료 처리 가능
   - 이미 완료/취소된 예약 완료 불가
   - 매너 점수 +1 (Guest), +1 (Host)

4. **Foreign Key 제약**:
   - vehicle_id: ON DELETE SET NULL (차량 삭제 시 NULL)
   - parking_space_id, guest_id: ON DELETE CASCADE

5. **삭제 제약**:
   - Vehicle에 진행중인 예약 있으면 삭제 불가
   - ParkingSpace에 진행중인 예약 있으면 삭제 불가

### 상태 전이 (현재)
```
confirmed (생성 직후)
   ├─→ cancelled (Guest가 취소, 매너 점수 -2)
   └─→ completed (Host/Guest가 완료, 양측 +1)
```

### 미래 필드 (future-features.md 참조)
- start_time (TIMESTAMP) - 예약 시작 시간
- end_time (TIMESTAMP) - 예약 종료 시간
- cancelled_at (TIMESTAMP) - 취소일
- 상태에 pending 추가 (결제 전 상태)

## Payment (결제)
- **연결**: Reservation (1:1)
- **게이트웨이**: Portone

### 필드 (예정)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | BIGINT | PK |
| reservation_id | BIGINT | FK (Reservation) UNIQUE |
| portone_payment_id | VARCHAR(100) | Portone 결제 ID |
| amount | INTEGER | 결제 금액 |
| status | VARCHAR(20) | pending/completed/cancelled/failed |
| paid_at | TIMESTAMP | 결제 완료 시각 |
| cancelled_at | TIMESTAMP | 취소 시각 (nullable) |

## 인덱스 전략
- `users.email` - UNIQUE INDEX (로그인)
- `vehicles.vehicle_number` - UNIQUE INDEX (중복 방지)
- `vehicles.user_id` - INDEX (내 차량 목록 조회)
- `parking_spaces.host_id` - INDEX (Host의 공간 목록)
- `reservations.guest_id` - INDEX (Guest의 예약 목록)
- `reservations.parking_space_id` - INDEX (공간별 예약 조회)

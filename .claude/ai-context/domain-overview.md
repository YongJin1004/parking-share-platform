# 비즈니스 도메인 규칙

## 핵심 비즈니스 개념

### 역할 전환 (User = Host + Guest)
- 한 사용자가 Host(제공자)와 Guest(이용자) 역할을 **동시에** 가능
- Host: 주차 공간 등록 및 관리
- Guest: 주차 공간 검색, 예약, 결제
- 역할 구분은 엔티티가 아닌 **행위(Action)**로 구분

### 차량 관리 ✅
- 차량 등록은 **선택 사항** (예약 시 vehicle_id는 optional)
- 차량번호는 **전체 시스템에서 유니크** (중복 등록 불가)
- `is_default` 차량: User당 하나만 지정 가능
- 기본 차량 변경 시 기존 기본 차량은 자동으로 해제
- **삭제 제약**: 진행중인 예약(confirmed/pending)에 사용된 차량은 삭제 불가

## 예약 및 결제 규칙

### 예약 생성 ✅ (최소 버전)
1. **검증 규칙**:
   - 자기 자신의 주차 공간 예약 불가
   - 동일 주차 공간에 진행중인 예약(confirmed/pending) 중복 불가
   - vehicle_id 제공 시 소유권 확인
   - is_available=false인 주차 공간 예약 불가
2. **요금 계산**: parking_space.hourly_rate × 1시간 (고정)
3. **초기 상태**: `confirmed` (결제 없이 바로 확정)
4. **미래 기능** (future-features.md):
   - 날짜/시간 기반 예약
   - 결제 연동 (Portone)
   - 상태: pending → confirmed (결제 완료 후)

### 예약 취소 ✅
- **현재 구현**:
  - Guest만 취소 가능
  - 이미 취소/완료된 예약 취소 불가
  - 매너 점수 -2점 (Guest)
  - 환불 없음 (결제 시스템 미구현)
- **미래 기능** (future-features.md):
  - 10분 이내만 취소 가능 (created_at 기준)
  - 결제 환불 자동 처리 (Portone API)

### 예약 완료 ✅
- **권한**: Host 또는 Guest만 완료 처리 가능
- **검증**: 이미 완료/취소된 예약 완료 불가
- **매너 점수**: Guest +1, Host +1 (양측 증가)

### 매너 점수 시스템 ✅
- **초기값**: 50점
- **범위**: 0 ~ 100점 (자동 클리핑)
- **현재 적용 규칙**:
  - 예약 취소: -2점 (Guest)
  - 예약 완료: +1점 (Guest), +1점 (Host)
- **미래 기능** (future-features.md):
  - 노쇼: -5점
  - 리뷰 기반 점수 조정
  - 10점 미만 시 예약 제한

### 노쇼 처리 (미구현)
- 예약 시작 시간 + 30분 경과 후 체크인 없으면 노쇼
- 노쇼 발생 시:
  - Guest의 manner_score -5점
  - Host에게는 환불 없이 요금 지급
  - 예약 상태: confirmed → no_show

## 주차 공간 검색 규칙 ✅

### 필터링 조건 (현재 구현)
1. **키워드 검색**: title 또는 address에 포함된 텍스트
2. **가격 범위**: min_hourly_rate ~ max_hourly_rate
3. **예약 가능 여부**: is_available (기본: true)

### 정렬 기준 (현재)
- 가격 오름차순 (hourly_rate ASC)

### 미래 필터링 (future-features.md)
1. **날짜/시간**: 예약 가능 시간대 필터
2. **차종**: allowed_vehicle_types에 Guest 차량 타입 포함 여부
3. **위치**: Kakao API로 반경 N km 내 검색
4. **정렬 기준 추가**: 거리(가까운 순), Host manner_score(높은 순)

## 결제 규칙

### 요금 계산
- 기본: `시간당 요금 × 사용 시간(시간 단위 올림)`
- 예시:
  - 1시간 30분 사용 = 2시간 요금
  - 30분 사용 = 1시간 요금

### Portone 연동
1. **사전 등록** (`POST /payments/prepare`)
   - 예약 정보 전송
   - merchant_uid 생성
2. **결제 승인** (`POST /payments/complete`)
   - imp_uid 검증
   - 금액 일치 여부 확인
3. **취소/환불** (`POST /payments/cancel`)
   - 10분 이내만 전액 환불
   - 부분 환불 불가

## 권한 및 보안

### 인증
- JWT 토큰 기반 (Bearer Token)
- 토큰 유효기간: **24시간**
- 리프레시 토큰 없음 (MVP 단계)

### 권한 검증 ✅
- **본인만 수정/삭제 가능**:
  - 내 차량 (Vehicle)
  - 내 주차 공간 (ParkingSpace)
  - 내 예약 취소 (Reservation - Guest만)
- **Host는 자신의 주차 공간 예약 불가**
- **중복 예약 방지**: 동일 주차 공간에 진행중인 예약(confirmed/pending) 중복 불가
- **예약 완료 권한**: Host 또는 Guest만 가능

## 데이터 무결성 규칙 ✅

### Foreign Key 제약
- **User 삭제** → Vehicle, ParkingSpace, Reservation (guest_id) CASCADE 삭제
- **ParkingSpace 삭제** → Reservation CASCADE 삭제
- **Vehicle 삭제** → Reservation.vehicle_id SET NULL

### 삭제 제약 (비즈니스 로직)
- **Vehicle**: 진행중인 예약(confirmed/pending)에 사용 중이면 삭제 불가
- **ParkingSpace**: 진행중인 예약(confirmed/pending)이 있으면 삭제 불가
- 에러 메시지: "Cannot delete [resource] with active reservations"

### 필수 검증
- 차량 등록: 차량번호 형식 검증 (정규식)
- 전화번호: `010-XXXX-XXXX` 형식 검증
- 이메일: RFC 5322 표준 검증 (Pydantic EmailStr)

## 향후 추가될 규칙
- 리뷰 시스템 (평점 1~5)
- 쿠폰/할인 시스템
- 정기 예약 (월/주 단위)
- 사진 업로드 제한 (용량, 개수)

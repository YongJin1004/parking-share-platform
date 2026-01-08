# 프로젝트 개요

## 핵심 개념
- **Host**: 주차 공간을 제공하는 사람
- **Guest**: 주차 공간을 이용하는 사람
- **역할 전환**: 한 사용자가 Host와 Guest 모두 가능

## MVP 핵심 기능
1. 회원가입 / 로그인 (JWT) ✅ 완료
2. 차량 등록 (Guest) ✅ 완료
3. 주차 공간 등록 (Host)
4. 주차 공간 검색 (날짜/시간/차종 필터)
5. 예약 및 결제

## 현재 확정된 규칙
- 예약 후 10분 이내만 취소 가능
- 예약 시 즉시 결제 (Portone)
- 노쇼 시 매너 점수 차감

## 구현된 API

### User 인증
- POST /api/v1/auth/register - 회원가입
- POST /api/v1/auth/login - 로그인 (OAuth2 + JWT)

### Vehicle (차량)
- GET /api/v1/vehicles - 내 차량 목록
- POST /api/v1/vehicles - 차량 등록
- PATCH /api/v1/vehicles/{id} - 차량 수정
- DELETE /api/v1/vehicles/{id} - 차량 삭제
- PATCH /api/v1/vehicles/{id}/set-default - 기본 차량 설정

## 개발하면서 추가될 내용
- 주차 공간(ParkingSpace) 엔티티 및 API
- 예약(Reservation) 엔티티 및 API
- 결제 연동 (Portone)
- 위치 기반 검색 (Kakao API)
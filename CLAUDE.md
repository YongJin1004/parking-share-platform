# Parking Share Platform

P2P 주차 공간 공유 플랫폼 - Host와 Guest를 연결

## 기술 스택
- **Backend**: FastAPI 0.109+, PostgreSQL 16, SQLAlchemy 2.0, Pydantic V2
- **Mobile**: Android (Kotlin, Jetpack Compose, Hilt, Retrofit)
- **외부 API**: Portone V2 (본인인증/결제), Kakao Local API (지도)

## AI Context 위치

### 지식 기반 (Knowledge)
- `.claude/ai-context/data-model.md` - 엔티티, 필드, 관계
- `.claude/ai-context/domain-overview.md` - 비즈니스 규칙
- `.claude/ai-context/api-spec.json` - API 엔드포인트 명세 (JSON)
- `.claude/ai-context/external-apis.md` - 외부 API 연동 (Portone V2, Kakao)
- `.claude/ai-context/mobile-auth-implementation.md` - 모바일 인증 구현 상세

### 행동 기반 (Skills)
- `.claude/skills/dev-basics/SKILL.md` - 기본 개발 원칙 및 워크플로우

## 프로젝트 구조
```
parking-share-platform/
├── backend/          # FastAPI 프로젝트
├── mobile/           # Android (Kotlin + Jetpack Compose)
└── .claude/          # AI Context & Skills
```

## 개발 환경
- Database: PostgreSQL 16 (Port: 5432)
- Python: 3.11+
- Android: compileSdk 34, minSdk 24

## 현재 구현 완료
- 회원가입/로그인 (JWT) ✅
- Portone V2 본인인증 (KG 이니시스) ✅
- 차량 CRUD ✅
- 주차 공간 CRUD ✅
  - Kakao 우편번호 WebView 주소 검색 ✅
  - 날짜별 운영 시간(정시 단위)/요금 설정 ✅
  - 허용 차종 멀티셀렉 (sedan/suv/van/truck/motorcycle) ✅
  - 사진 업로드 (기기 사진 보관함 → 서버 로컬 저장) ✅
  - 목록 실시간 갱신 (ON_RESUME lifecycle) ✅
- 예약 CRUD (결제 없음) ✅

## 코딩 가이드라인 (Karpathy Guidelines)
1. **Think Before Coding** - 가정을 명시적으로 말하고, 불명확하면 질문. 여러 해석이 가능하면 제시.
2. **Simplicity First** - 요청한 것만 구현. 투기적 기능, 불필요한 추상화 금지.
3. **Surgical Changes** - 요청한 부분만 수정. 인접 코드 "개선" 금지. 기존 스타일 유지.
4. **Goal-Driven Execution** - 검증 가능한 성공 기준 정의. 멀티스텝 작업은 계획 먼저.

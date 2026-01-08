# Parking Share Platform

P2P 주차 공간 공유 플랫폼 - Host와 Guest를 연결

## AI Context 위치

### 지식 기반 (Knowledge)
- `.claude/ai-context/PROJECT-OVERVIEW.md` - 프로젝트 개요 및 핵심 개념
- `.claude/ai-context/TECH-STACK.md` - 기술 스택 및 환경 설정
- `.claude/ai-context/domain-overview.md` - 비즈니스 규칙 (개발하면서 추가)
- `.claude/ai-context/data-model.md` - 엔티티 및 테이블 구조 (개발하면서 추가)
- `.claude/ai-context/api-spec.json` - API 엔드포인트 명세 (개발하면서 추가)
- `.claude/ai-context/external-apis.md` - 외부 API 연동 (개발하면서 추가)
- `.claude/ai-context/error-handling.md` - 에러 처리 규칙 

### 행동 기반 (Skills)
- `.claude/skills/dev-basics/SKILL.md` - 기본 개발 원칙 및 워크플로우

## 프로젝트 구조
```
parking-share-platform/
├── backend/          # FastAPI 프로젝트
├── mobile/           # Android (추후)
└── .claude/          # AI Context & Skills
```

## 개발 환경
- Database: PostgreSQL 16 (Port: 5432)
- Python: 3.11+
- FastAPI: 0.109+


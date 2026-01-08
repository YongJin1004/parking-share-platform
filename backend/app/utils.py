"""
공통 유틸리티 함수
"""
from sqlalchemy.orm import Session
from app.models import User


def update_manner_score(db: Session, user_id: int, score_change: int):
    """
    사용자의 매너 점수 업데이트

    Args:
        db: 데이터베이스 세션
        user_id: 사용자 ID
        score_change: 점수 변화량 (양수 또는 음수)
    """
    user = db.query(User).filter(User.id == user_id).first()
    if user:
        # 매너 점수 범위: 0 ~ 100
        new_score = max(0, min(100, user.manner_score + score_change))
        user.manner_score = new_score
        db.commit()


def increment_review_count(db: Session, user_id: int):
    """
    사용자의 리뷰 수 증가

    Args:
        db: 데이터베이스 세션
        user_id: 사용자 ID
    """
    user = db.query(User).filter(User.id == user_id).first()
    if user:
        user.total_reviews += 1
        db.commit()

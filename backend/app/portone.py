"""
Portone V2 본인인증 모듈
- KG 이니시스 통합인증
"""
import httpx
import os
import logging
from dotenv import load_dotenv
from fastapi import HTTPException, status

load_dotenv()

logger = logging.getLogger(__name__)

PORTONE_API_SECRET = os.getenv("PORTONE_API_SECRET")
PORTONE_STORE_ID = os.getenv("PORTONE_STORE_ID")


async def get_portone_access_token() -> str:
    """Portone V2 Access Token 발급"""
    url = "https://api.portone.io/login/api-secret"
    headers = {"Content-Type": "application/json"}
    data = {"apiSecret": PORTONE_API_SECRET}

    async with httpx.AsyncClient() as client:
        response = await client.post(url, json=data, headers=headers)
        print(f"토큰 발급 응답: {response.status_code} - {response.text}")

        if response.status_code != 200:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Portone 토큰 발급 실패: {response.text}"
            )

        result = response.json()
        return result.get("accessToken")


async def get_certification_info(identity_verification_id: str) -> dict:
    """
    Portone V2 본인인증 정보 조회

    Args:
        identity_verification_id: 본인인증 ID (identityVerificationId)

    Returns:
        인증된 사용자 정보 (이름, 전화번호, 생년월일 등)
    """
    # 1. Access Token 발급
    access_token = await get_portone_access_token()

    url = f"https://api.portone.io/identity-verifications/{identity_verification_id}?storeId={PORTONE_STORE_ID}"
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }

    print(f"=== Portone API 호출 ===")
    print(f"URL: {url}")
    print(f"identity_verification_id: {identity_verification_id}")
    print(f"Access Token: {access_token[:30]}..." if access_token else "Token is None!")

    async with httpx.AsyncClient() as client:
        response = await client.get(url, headers=headers)

        print(f"응답 코드: {response.status_code}")
        print(f"응답 내용: {response.text}")

        if response.status_code != 200:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"인증 정보 조회 실패: {response.text}"
            )

        result = response.json()

        # V2 응답 구조 확인
        if result.get("status") != "VERIFIED":
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"본인인증이 완료되지 않았습니다. 상태: {result.get('status')}"
            )

        verified_customer = result.get("verifiedCustomer", {})

        return {
            "name": verified_customer.get("name"),
            "phone": verified_customer.get("phoneNumber"),
            "birth": verified_customer.get("birthDate"),
            "gender": verified_customer.get("gender"),
            "ci": verified_customer.get("ci"),
            "di": verified_customer.get("di"),
            "certified": True,
            "certified_at": result.get("verifiedAt")
        }


def format_phone_number(phone: str) -> str:
    """
    전화번호 포맷팅 (01012345678 → 010-1234-5678)
    """
    if phone is None:
        return ""
    digits = ''.join(filter(str.isdigit, phone))
    if len(digits) == 11 and digits.startswith("010"):
        return f"{digits[:3]}-{digits[3:7]}-{digits[7:]}"
    return phone

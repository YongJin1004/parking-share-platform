# 외부 API 연동

## Kakao Local API

### 목적
- 주소 → 좌표 변환
- 위치 기반 주차 공간 검색

### 인증
```python
headers = {
    "Authorization": f"KakaoAK {KAKAO_REST_API_KEY}"
}
```

### 1. 주소 → 좌표 변환
**엔드포인트**: `GET https://dapi.kakao.com/v2/local/search/address.json`

**용도**: ParkingSpace 등록 시 주소로 위도/경도 추출

**요청 예시**:
```python
import requests

url = "https://dapi.kakao.com/v2/local/search/address.json"
params = {"query": "서울특별시 강남구 테헤란로 152"}
headers = {"Authorization": f"KakaoAK {KAKAO_REST_API_KEY}"}

response = requests.get(url, params=params, headers=headers)
data = response.json()

# 좌표 추출
if data['documents']:
    latitude = data['documents'][0]['y']
    longitude = data['documents'][0]['x']
```

**응답 예시**:
```json
{
  "documents": [
    {
      "address_name": "서울 강남구 역삼동 737",
      "x": "127.036489",  // 경도
      "y": "37.500969"    // 위도
    }
  ]
}
```

### 2. 키워드 검색 (위치 기반)
**엔드포인트**: `GET https://dapi.kakao.com/v2/local/search/keyword.json`

**용도**: Guest가 "강남역 주차장" 등으로 검색

**요청 예시**:
```python
params = {
    "query": "강남역 주차장",
    "x": "127.027619",  # 현재 위치 경도
    "y": "37.497942",   # 현재 위치 위도
    "radius": 1000      # 반경 1km
}
```

### 에러 처리
| 코드 | 설명 | 처리 |
|------|------|------|
| 401 | 인증 실패 | API 키 확인 |
| 400 | 잘못된 요청 | 주소 형식 검증 |
| 404 | 결과 없음 | "주소를 찾을 수 없습니다" 응답 |

---

## Portone V2 (본인인증)

### 목적
1. **본인인증** (KG 이니시스 통합인증 - 테스트 모드) ✅ 구현 완료
2. **결제** (카카오페이 or 토스페이먼츠 - 추후 구현)
3. **결제 취소/환불** (추후 구현)

### 환경 변수 (.env)
```bash
PORTONE_STORE_ID=store-xxxxx      # 상점 ID
PORTONE_CHANNEL_KEY=channel-key-xxxxx  # 채널 키 (KG 이니시스)
PORTONE_API_SECRET=xxxxx          # V2 API Secret
```

### 본인인증 플로우 (구현 완료)

```
[Android App]                    [Backend]                    [Portone V2 API]
     |                              |                              |
     |-- WebView에서 JS SDK 호출 -->|                              |
     |   PortOne.requestIdentityVerification()                     |
     |                              |                              |
     |<-- redirectUrl로 리다이렉트 --|                              |
     |   (identityVerificationId 포함)                             |
     |                              |                              |
     |-- POST /verify-certification -->|                           |
     |   (identityVerificationId)   |                              |
     |                              |-- POST /login/api-secret --->|
     |                              |<-- accessToken --------------|
     |                              |                              |
     |                              |-- GET /identity-verifications/{id} -->|
     |                              |<-- 인증 정보 (이름, 전화번호) --|
     |                              |                              |
     |<-- 인증 결과 (name, phone) ---|                              |
     |                              |                              |
     |-- POST /register-with-cert -->|                             |
     |   (email, password, impUid)  |                              |
     |<-- 회원가입 완료 -------------|                              |
```

### Android WebView 연동 (JS SDK v2)

```kotlin
// CertificationScreen.kt - WebView에서 Portone V2 JS SDK 사용
val identityVerificationId = "iv_${System.currentTimeMillis()}"
val redirectUrl = "https://parkingshare.app/identity-verification-result"

val htmlContent = """
    <script src="https://cdn.portone.io/v2/browser-sdk.js"></script>
    <script>
        PortOne.requestIdentityVerification({
            storeId: "$storeId",
            channelKey: "$channelKey",
            identityVerificationId: "$identityVerificationId",
            redirectUrl: "$redirectUrl"
        });
    </script>
""".trimIndent()

// WebViewClient에서 redirectUrl 감지
webViewClient = object : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        if (url.startsWith(redirectUrl)) {
            val uri = Uri.parse(url)
            val resultId = uri.getQueryParameter("identityVerificationId")
            // Backend로 resultId 전송하여 검증
        }
        return false
    }
}
```

### Backend API 구현

**1. Access Token 발급**
```python
# backend/app/portone.py
async def get_portone_access_token() -> str:
    url = "https://api.portone.io/login/api-secret"
    data = {"apiSecret": PORTONE_API_SECRET}

    async with httpx.AsyncClient() as client:
        response = await client.post(url, json=data)
        return response.json().get("accessToken")
```

**2. 본인인증 정보 조회**
```python
async def get_certification_info(identity_verification_id: str) -> dict:
    access_token = await get_portone_access_token()

    url = f"https://api.portone.io/identity-verifications/{identity_verification_id}?storeId={PORTONE_STORE_ID}"
    headers = {"Authorization": f"Bearer {access_token}"}

    async with httpx.AsyncClient() as client:
        response = await client.get(url, headers=headers)
        result = response.json()

        if result.get("status") != "VERIFIED":
            raise HTTPException(status_code=400, detail="본인인증 미완료")

        verified = result.get("verifiedCustomer", {})
        return {
            "name": verified.get("name"),
            "phone": verified.get("phoneNumber"),
            "certified": True
        }
```

**3. API 엔드포인트**
```python
# POST /api/v1/auth/verify-certification
# - 본인인증 검증 후 이름/전화번호 반환

# POST /api/v1/auth/register-with-cert
# - 본인인증 ID + 이메일/비밀번호로 회원가입
# - phone_verified = True로 자동 설정
```

### 에러 처리
| 상황 | 원인 | 해결 |
|------|------|------|
| 401 Unauthorized | API Secret 인증 실패 | Bearer 토큰 방식 사용 |
| "redirectUrl 필수" | 모바일에서 리다이렉션 방식 필요 | redirectUrl 파라미터 추가 |
| "등록된 pg 설정 정보를 찾을 수 없습니다" | V1 SDK 사용 | V2 SDK로 변경 |

### 주의사항
- **V1 SDK (cdn.iamport.kr)가 아닌 V2 SDK (cdn.portone.io) 사용**
- 모바일에서는 리다이렉션 방식만 지원 (콜백 방식 불가)
- Access Token은 API 호출마다 새로 발급 (캐싱 추후 구현)

### 결제 (카카오페이 or 토스페이먼츠)

**1. 결제 사전 등록**
**엔드포인트**: `POST https://api.iamport.kr/payments/prepare`

**용도**: 결제 전 금액 사전 등록 (위변조 방지)

**요청**:
```python
import requests

url = "https://api.iamport.kr/payments/prepare"
headers = {"Authorization": f"Bearer {access_token}"}
data = {
    "merchant_uid": f"reservation_{reservation_id}_{timestamp}",
    "amount": total_amount
}

response = requests.post(url, json=data, headers=headers)
```

**프론트엔드 결제 호출 (카카오페이)**:
```javascript
IMP.request_pay({
    pg: 'kakaopay.TC0ONETIME',  // 테스트 모드
    pay_method: 'card',
    merchant_uid: merchant_uid,
    amount: 10000,
    name: '주차 공간 예약',
    buyer_name: '홍길동'
}, function(response) {
    if (response.success) {
        // Backend로 imp_uid 전송 → 검증
        verifyPayment(response.imp_uid);
    }
});
```

**프론트엔드 결제 호출 (토스페이먼츠)**:
```javascript
IMP.request_pay({
    pg: 'tosspayments',
    pay_method: 'card',
    merchant_uid: merchant_uid,
    amount: 10000,
    name: '주차 공간 예약'
}, callback);
```

**2. 결제 승인 (검증)**
**엔드포인트**: `GET https://api.iamport.kr/payments/{imp_uid}`

**용도**: 클라이언트 결제 완료 후 서버에서 검증

**요청**:
```python
url = f"https://api.iamport.kr/payments/{imp_uid}"
headers = {"Authorization": f"Bearer {access_token}"}

response = requests.get(url, headers=headers)
payment_data = response.json()

# 금액 검증
if payment_data['amount'] == expected_amount:
    # 예약 상태 업데이트: pending → confirmed
    pass
else:
    # 위변조 감지 → 결제 취소
    pass
```

**3. 결제 취소/환불**
**엔드포인트**: `POST https://api.iamport.kr/payments/cancel`

**용도**: 10분 이내 예약 취소 시 전액 환불

**요청**:
```python
url = "https://api.iamport.kr/payments/cancel"
headers = {"Authorization": f"Bearer {access_token}"}
data = {
    "imp_uid": imp_uid,
    "amount": cancel_amount,  # 전액 환불
    "reason": "사용자 요청"
}

response = requests.post(url, json=data, headers=headers)
```

### 토큰 발급
**엔드포인트**: `POST https://api.iamport.kr/users/getToken`

**요청**:
```python
url = "https://api.iamport.kr/users/getToken"
data = {
    "imp_key": PORTONE_API_KEY,
    "imp_secret": PORTONE_API_SECRET
}

response = requests.post(url, json=data)
access_token = response.json()['response']['access_token']
```

### 에러 처리
| 코드 | 설명 | 처리 |
|------|------|------|
| -1 | 이미 취소된 결제 | 중복 취소 방지 |
| 401 | 인증 실패 | 토큰 재발급 |
| 404 | 결제 건 없음 | imp_uid 확인 |

### 웹훅 (추후 구현)
Portone → Backend 자동 알림
- 결제 완료
- 결제 취소
- 결제 실패

**엔드포인트**: `POST /api/v1/webhooks/portone`

---

## Kakao 우편번호 서비스 (구현 완료 ✅)

### 목적
- **Host가 주차 공간 등록 시 주소 검색**
- 선택한 주소를 Kakao Local API로 좌표 변환 (미래)

### 주의사항 (트러블슈팅 경험)
- ~~Daum~~ → **Kakao**로 마이그레이션됨 (CDN, 네임스페이스 모두 변경)
- CDN: `t1.daumcdn.net` ❌ → `t1.kakaocdn.net` ✅
- 네임스페이스: `daum.Postcode` ❌ → `kakao.Postcode` ✅
- Android WebView에서 `file://` 프로토콜 사용 불가 → `loadDataWithBaseURL` 필수
- Compose `Dialog` 내부에서 `fillMaxHeight()` 동작 안 함 → `Box` 오버레이로 대체

### Android WebView 구현 (구현 완료)

**핵심 포인트**:
```kotlin
// loadDataWithBaseURL 필수: baseUrl을 kakao 도메인으로 설정해야 protocol-relative URL(//) 해석 가능
webView.loadDataWithBaseURL(
    "https://postcode.map.kakao.com",
    POSTCODE_HTML,
    "text/html",
    "utf-8",
    null
)

// CSS: height:100% 대신 position:fixed 사용 (Quirks Mode 대응)
// #wrap { position:fixed; top:0; left:0; right:0; bottom:0; }

// JavascriptInterface: 익명 객체 아닌 named class 필수 (reflection 안정성)
private class AddressBridge(private val callback: (String) -> Unit) {
    @JavascriptInterface
    fun onAddressSelected(address: String) { ... }
}
```

**HTML 템플릿**:
```html
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>
*{margin:0;padding:0;box-sizing:border-box;}
body{overflow:hidden;}
#wrap{position:fixed;top:0;left:0;right:0;bottom:0;}
</style>
</head>
<body>
<div id="wrap"></div>
<script src="//t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>
<script>
new kakao.Postcode({
    width:'100%', height:'100%',
    oncomplete:function(data){
        AndroidBridge.onAddressSelected(data.roadAddress||data.address);
    }
}).embed(document.getElementById('wrap'));
</script>
</body>
</html>
```

### Backend에서 좌표 변환 (Kakao Local API)

**엔드포인트**: `GET https://dapi.kakao.com/v2/local/search/address.json`

**요청**:
```python
import requests

def get_coordinates(address: str):
    url = "https://dapi.kakao.com/v2/local/search/address.json"
    headers = {"Authorization": f"KakaoAK {KAKAO_REST_API_KEY}"}
    params = {"query": address}

    response = requests.get(url, headers=headers, params=params)
    data = response.json()

    if data['documents']:
        return {
            "latitude": data['documents'][0]['y'],
            "longitude": data['documents'][0]['x']
        }
    return None
```

**ParkingSpace 등록 시 자동 호출**:
- Host가 주소 입력
- Backend가 Kakao API 호출
- 위도/경도 자동 저장

---

## API 키 관리 (.env)

```bash
# Kakao
KAKAO_REST_API_KEY=your_kakao_rest_api_key

# Portone
PORTONE_API_KEY=your_portone_api_key
PORTONE_API_SECRET=your_portone_api_secret
PORTONE_MODE=test

# Resend (이메일 발송 - 추후)
RESEND_API_KEY=your_resend_api_key
```

## 주의사항
- **절대 Git에 커밋하지 말 것**: `.gitignore`에 `.env` 추가 필수
- 테스트 모드에서 충분히 검증 후 운영 전환
- API 호출 실패 시 재시도 로직 구현 (max 3회)

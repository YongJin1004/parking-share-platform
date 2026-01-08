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

## Portone (구 아임포트)

### 목적
1. **본인인증** (KG 이니시스 - 테스트 모드)
2. **결제** (카카오페이 or 토스페이먼츠 - 테스트 모드)
3. **결제 취소/환불**

### 설정
```python
PORTONE_API_KEY = os.getenv("PORTONE_API_KEY")
PORTONE_API_SECRET = os.getenv("PORTONE_API_SECRET")
PORTONE_MODE = "test"  # 테스트 모드 고정
```

### 본인인증 (KG 이니시스)

**목적**: 전화번호 인증 (회원가입 시)

**프론트엔드 연동**:
```javascript
IMP.init('YOUR_IMP_CODE'); // Portone 가맹점 식별코드

IMP.certification({
    pg: 'inicis_unified',  // KG 이니시스
    merchant_uid: `cert_${new Date().getTime()}`
}, function(response) {
    if (response.success) {
        // 인증 성공 → Backend로 imp_uid 전송
        verifyIdentity(response.imp_uid);
    }
});
```

**Backend 검증**:
```python
def verify_phone_number(imp_uid: str):
    url = f"https://api.iamport.kr/certifications/{imp_uid}"
    headers = {"Authorization": f"Bearer {access_token}"}

    response = requests.get(url, headers=headers)
    cert_data = response.json()['response']

    return {
        "name": cert_data['name'],
        "phone": cert_data['phone'],
        "verified": True
    }
```

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

## Daum 우편번호 API + Kakao Maps

### 목적
- **Host가 주차 공간 등록 시 주소 입력 및 위도/경도 추출**
- 지도에 마커 표시

### 워크플로우
1. **Daum 우편번호 서비스**로 주소 검색
2. 선택한 주소를 **Kakao Local API**로 위도/경도 변환
3. 위도/경도를 DB에 저장
4. 지도에 마커 표시

### Daum 우편번호 서비스 (프론트엔드)

**CDN**:
```html
<script src="//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>
```

**예시**:
```javascript
new daum.Postcode({
    oncomplete: function(data) {
        // data.address: "서울 강남구 테헤란로 152"
        // Backend로 전송 → Kakao API로 좌표 변환
        sendToBackend({
            address: data.address
        });
    }
}).open();
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

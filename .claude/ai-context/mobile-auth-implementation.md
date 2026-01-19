# Mobile Authentication Implementation

## 개요
Android 앱의 인증 시스템 구현 완료 (회원가입 + 로그인)
- **Backend**: FastAPI OAuth2 + JWT
- **Mobile**: Jetpack Compose + Clean Architecture

---

## 1. 핵심 아키텍처

### Clean Architecture 레이어
```
Presentation (UI) → Domain (UseCase) → Data (Repository) → Remote (API)
```

### 의존성 주입
- **Hilt (Dagger)** 사용
- 모든 ViewModel, Repository, API는 Hilt로 주입

---

## 2. 인증 플로우

### 회원가입 (Registration)
**API**: `POST /api/v1/auth/register`

**Request Body** (JSON):
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동",
  "phone": "010-1234-5678"  // 중요: 반드시 이 형식
}
```

**Response**: `UserResponse` (토큰 없음, 회원가입만)

**UI 플로우**:
1. 사용자가 정보 입력
2. 전화번호 자동 포맷팅 (`formatPhoneNumber`)
3. 실시간 유효성 검사 (`isValidPhoneNumber`)
4. 회원가입 성공 → 로그인 화면으로 이동

### 로그인 (Login)
**API**: `POST /api/v1/auth/login`

**Request** (Form-UrlEncoded - OAuth2 표준):
```
username=user@example.com  // 주의: email이지만 필드명은 username
password=password123
```

**Response** (`TokenResponse`):
```json
{
  "access_token": "eyJhbGc...",
  "token_type": "Bearer"
}
```

**UI 플로우**:
1. 사용자가 이메일/비밀번호 입력
2. 로그인 성공 → 토큰 저장 (DataStore)
3. Home 화면으로 이동

---

## 3. 토큰 관리 (중요!)

### TokenManager.kt
**위치**: `mobile/app/src/main/java/com/parking/share/data/local/TokenManager.kt`

**DataStore 사용**:
```kotlin
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")
```

**주요 메서드**:
```kotlin
// 토큰 저장
suspend fun saveToken(accessToken: String, tokenType: String)

// 토큰 삭제
suspend fun clearToken()

// Authorization 헤더 생성
suspend fun getAuthHeader(): String?  // "Bearer eyJhbGc..."
```

**중요 - Flow 처리**:
```kotlin
// ❌ 잘못된 방법 (무한 대기):
context.dataStore.data.map { }.collect { }

// ✅ 올바른 방법 (단일 값):
val preferences = context.dataStore.data.first()
```

---

## 4. 전화번호 검증 (Critical!)

### Backend 요구사항
- **정규식**: `^010-\d{4}-\d{4}$`
- **형식**: `010-1234-5678` (정확히 13자)
- **검증 실패 시**: HTTP 422 Unprocessable Entity

### Mobile 구현 (RegisterScreen.kt)

**자동 포맷팅**:
```kotlin
fun formatPhoneNumber(input: String): String {
    val digits = input.filter { it.isDigit() }
    return when {
        digits.length <= 3 -> digits
        digits.length <= 7 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
        else -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7, minOf(11, digits.length))}"
    }
}
```

**유효성 검사**:
```kotlin
fun isValidPhoneNumber(phoneNumber: String): Boolean {
    return phoneNumber.matches(Regex("^010-\\d{4}-\\d{4}$"))
}
```

**UI 적용**:
- 입력 중 자동으로 하이픈(-) 삽입
- 최대 13자 제한
- 형식 오류 시 에러 메시지 표시
- 올바른 형식일 때만 회원가입 버튼 활성화

---

## 5. API 통신 설정

### Retrofit Configuration
**BaseURL**: `http://10.0.2.2:8000/api/v1/`
- `10.0.2.2`: Android 에뮬레이터에서 localhost 접근
- 실기기 테스트 시: PC의 실제 IP 주소 사용

**인증 헤더 자동 추가** (AuthInterceptor):
```kotlin
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenManager.getAuthHeader() }
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", token)
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
```

### 중요 설정
**AndroidManifest.xml**:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<application android:usesCleartextTraffic="true">
```

---

## 6. 주요 파일 구조

```
mobile/app/src/main/java/com/parking/share/
├── data/
│   ├── local/
│   │   └── TokenManager.kt              # 토큰 저장/관리
│   ├── remote/
│   │   ├── api/AuthApi.kt               # Retrofit API 인터페이스
│   │   └── dto/AuthDto.kt               # Request/Response DTO
│   └── repository/
│       └── AuthRepositoryImpl.kt        # Repository 구현
├── domain/
│   ├── model/User.kt                    # Domain 모델
│   ├── repository/AuthRepository.kt     # Repository 인터페이스
│   └── usecase/
│       ├── LoginUseCase.kt
│       └── RegisterUseCase.kt
├── presentation/
│   └── auth/
│       ├── LoginScreen.kt               # 로그인 UI
│       ├── LoginViewModel.kt
│       ├── RegisterScreen.kt            # 회원가입 UI (전화번호 검증 포함)
│       └── RegisterViewModel.kt
└── di/
    ├── NetworkModule.kt                 # Retrofit, OkHttp 설정
    └── RepositoryModule.kt              # Repository DI
```

---

## 7. Gradle 설정 (검증된 버전)

### 프로젝트 레벨 (mobile/build.gradle.kts)
```kotlin
buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.3.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.50")
    }
}
```

### 앱 레벨 (mobile/app/build.gradle.kts)
```kotlin
android {
    compileSdk = 34

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"  // Kotlin 1.9.22 호환
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}
```

### Gradle Wrapper
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
```

---

## 8. 디버깅 팁

### 로그 확인
```kotlin
// ViewModel에서 로그 추가
Log.d("RegisterViewModel", "회원가입 시작: email=$email, phone=$phone")
Log.d("LoginViewModel", "로그인 성공: $token")
```

### OkHttp 로깅
```kotlin
val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}
```

### 일반적인 에러

| 에러 | 원인 | 해결 |
|------|------|------|
| HTTP 422 | 전화번호 형식 오류 | `010-XXXX-XXXX` 형식 확인 |
| HTTP 401 | 토큰 만료/없음 | 토큰 재발급 또는 로그인 |
| Connection refused | Backend 미실행 | Backend 서버 시작 확인 |
| 무한 로딩 | Flow `.collect {}` 사용 | `.first()` 사용 |

---

## 9. 테스트 시나리오

### 회원가입 테스트
1. ✅ 모든 필드 입력 (email, password, name, phone)
2. ✅ 전화번호 자동 포맷팅 확인 ("01012345678" → "010-1234-5678")
3. ✅ 형식 오류 시 에러 메시지 표시
4. ✅ 회원가입 성공 → 로그인 화면 이동

### 로그인 테스트
1. ✅ 가입한 이메일/비밀번호로 로그인
2. ✅ 토큰 저장 확인 (DataStore)
3. ✅ 로그인 성공 → Home 화면 이동

---

## 10. 본인인증 (Portone V2) ✅ 구현 완료

### 개요
회원가입 전 본인인증 필수 (KG 이니시스 통합인증)

### 플로우
```
로그인 화면 → "회원가입" 클릭 → 본인인증 화면 → 인증 완료 → 회원가입 화면 (이름/전화번호 자동입력)
```

### 화면 구조
```
presentation/auth/
├── CertificationScreen.kt       # WebView 본인인증
├── CertificationViewModel.kt    # 인증 검증 로직
├── RegisterWithCertScreen.kt    # 본인인증 후 회원가입
└── RegisterWithCertViewModel.kt # 회원가입 API 호출
```

### 네비게이션 (NavGraph.kt)
```kotlin
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Certification : Screen("certification")
    object RegisterWithCert : Screen("register_with_cert/{impUid}/{name}/{phone}")
    object Home : Screen("home")
}
```

### WebView 본인인증 (CertificationScreen.kt)

**Portone V2 JS SDK 사용**:
```kotlin
val htmlContent = """
    <script src="https://cdn.portone.io/v2/browser-sdk.js"></script>
    <script>
        PortOne.requestIdentityVerification({
            storeId: "$storeId",
            channelKey: "$channelKey",
            identityVerificationId: "$identityVerificationId",
            redirectUrl: "$redirectUrl"  // 필수!
        });
    </script>
""".trimIndent()
```

**redirectUrl 감지**:
```kotlin
webViewClient = object : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        if (url.startsWith(redirectUrl)) {
            val resultId = Uri.parse(url).getQueryParameter("identityVerificationId")
            // Backend로 검증 요청
            viewModel.verifyCertification(resultId!!)
            return true
        }
        return false
    }
}
```

### API 호출

**1. 본인인증 검증**:
```kotlin
// CertificationViewModel.kt
suspend fun verifyCertification(identityVerificationId: String) {
    val response = authApi.verifyCertification(
        CertificationVerifyRequest(impUid = identityVerificationId)
    )
    // response.name, response.phone 저장
}
```

**2. 회원가입**:
```kotlin
// RegisterWithCertViewModel.kt
suspend fun register(email: String, password: String, impUid: String) {
    authApi.registerWithCert(
        RegisterWithCertRequest(email, password, impUid)
    )
}
```

### DTO
```kotlin
// AuthDto.kt
@Serializable
data class CertificationVerifyRequest(
    @SerialName("imp_uid") val impUid: String
)

@Serializable
data class CertificationVerifyResponse(
    val name: String,
    val phone: String,
    val certified: Boolean
)

@Serializable
data class RegisterWithCertRequest(
    val email: String,
    val password: String,
    @SerialName("imp_uid") val impUid: String
)
```

### BuildConfig 설정 (app/build.gradle.kts)
```kotlin
buildConfigField("String", "PORTONE_STORE_ID", "\"store-xxxxx\"")
buildConfigField("String", "PORTONE_CHANNEL_KEY", "\"channel-key-xxxxx\"")
```

### 주의사항
- **V2 SDK 사용** (`cdn.portone.io`, NOT `cdn.iamport.kr`)
- **redirectUrl 필수** (모바일 WebView는 콜백 방식 불가)
- WebView 설정: `javaScriptEnabled = true`, `domStorageEnabled = true`

---

## 11. 향후 작업

### 필요 기능
- [ ] 토큰 자동 갱신 (Refresh Token)
- [ ] 로그아웃 기능
- [ ] 자동 로그인 (토큰 유효성 확인)
- [ ] 비밀번호 찾기/재설정
- [ ] 이메일 인증

### 보안 강화
- [ ] HTTPS 적용 (프로덕션)
- [ ] Certificate Pinning
- [ ] Root Detection
- [ ] 생체 인증 (지문/얼굴)

---

## 11. MSA 연동 시 주의사항

### 인증 서비스 분리 시
- **Gateway Pattern**: API Gateway에서 토큰 검증
- **Service-to-Service**: JWT를 각 서비스로 전달
- **Token Introspection**: Auth Service에 토큰 검증 API 제공

### 토큰 관리
- **Access Token**: 짧은 만료 시간 (15분~1시간)
- **Refresh Token**: 긴 만료 시간 (7일~30일), 안전하게 저장
- **Token Rotation**: Refresh Token 갱신 시 새로운 Refresh Token 발급

### Backend 엔드포인트 확장 (필요 시)
```
POST /api/v1/auth/register      # 회원가입
POST /api/v1/auth/login         # 로그인 (토큰 발급)
POST /api/v1/auth/refresh       # 토큰 갱신
POST /api/v1/auth/logout        # 로그아웃
GET  /api/v1/auth/me            # 현재 사용자 정보
POST /api/v1/auth/verify-email  # 이메일 인증
```

---

## 요약 체크리스트

- [x] Clean Architecture 구조 구현
- [x] Hilt DI 설정 완료
- [x] Retrofit + Kotlinx Serialization 설정
- [x] DataStore 토큰 관리
- [x] 전화번호 자동 포맷팅 + 검증
- [x] 회원가입 API 연동
- [x] 로그인 API 연동 (OAuth2 Form)
- [x] Authorization 헤더 자동 추가
- [x] UI/UX 에러 처리
- [x] 테스트 검증 완료

**핵심**: 전화번호 형식(`010-XXXX-XXXX`), Flow `.first()` 사용, OAuth2 Form 방식

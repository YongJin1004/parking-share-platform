package com.parking.share.presentation.auth

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.parking.share.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificationScreen(
    onCertificationSuccess: (impUid: String, name: String, phone: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CertificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showWebView by remember { mutableStateOf(false) }

    // 인증 성공 시 다음 화면으로 이동
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && uiState.impUid != null) {
            onCertificationSuccess(
                uiState.impUid!!,
                uiState.certifiedName ?: "",
                uiState.certifiedPhone ?: ""
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("본인인증") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showWebView) {
                            showWebView = false
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Text("←")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showWebView) {
            // WebView로 본인인증 진행
            CertificationWebView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                storeId = BuildConfig.PORTONE_STORE_ID,
                channelKey = BuildConfig.PORTONE_CHANNEL_KEY,
                onSuccess = { identityVerificationId ->
                    showWebView = false
                    viewModel.verifyCertification(identityVerificationId)
                },
                onFail = { errorMsg ->
                    showWebView = false
                    viewModel.setError(errorMsg)
                }
            )
        } else {
            // 안내 화면
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "회원가입을 위해\n본인인증이 필요합니다",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "본인인증 완료 후\n이름과 전화번호가 자동으로 입력됩니다",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = { showWebView = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("본인인증 시작", style = MaterialTheme.typography.titleMedium)
                    }
                }

                uiState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onNavigateBack) {
                    Text("나중에 하기")
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CertificationWebView(
    modifier: Modifier = Modifier,
    storeId: String,
    channelKey: String,
    onSuccess: (identityVerificationId: String) -> Unit,
    onFail: (errorMsg: String) -> Unit
) {
    val identityVerificationId = remember { "iv_${System.currentTimeMillis()}" }
    // 리다이렉션 결과를 받을 URL (앱 내부에서 감지)
    val redirectUrl = "https://parkingshare.app/identity-verification-result"

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script src="https://cdn.portone.io/v2/browser-sdk.js"></script>
            <style>
                body {
                    margin: 0;
                    padding: 20px;
                    font-family: -apple-system, BlinkMacSystemFont, sans-serif;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    min-height: 100vh;
                    background: #f5f5f5;
                }
                .loading {
                    text-align: center;
                    color: #666;
                }
                .spinner {
                    border: 3px solid #f3f3f3;
                    border-top: 3px solid #3498db;
                    border-radius: 50%;
                    width: 30px;
                    height: 30px;
                    animation: spin 1s linear infinite;
                    margin: 0 auto 16px;
                }
                @keyframes spin {
                    0% { transform: rotate(0deg); }
                    100% { transform: rotate(360deg); }
                }
            </style>
        </head>
        <body>
            <div class="loading">
                <div class="spinner"></div>
                <div id="status">본인인증을 준비중입니다...</div>
            </div>
            <script>
                async function requestCertification() {
                    try {
                        document.getElementById('status').innerText = '본인인증 창을 여는 중...';

                        // 리다이렉션 방식으로 호출
                        await PortOne.requestIdentityVerification({
                            storeId: "$storeId",
                            channelKey: "$channelKey",
                            identityVerificationId: "$identityVerificationId",
                            redirectUrl: "$redirectUrl"
                        });

                        // 리다이렉션 방식이므로 여기까지 도달하면 에러
                        document.getElementById('status').innerText = '인증 창으로 이동 중...';
                    } catch (error) {
                        document.getElementById('status').innerText = '오류: ' + (error.message || '알 수 없는 오류');
                    }
                }

                // 페이지 로드 후 실행
                window.onload = requestCertification;
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(true)
                settings.userAgentString = settings.userAgentString + " ParkingShareApp"

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        Log.d("CertificationWebView", "URL: $url")

                        // 리다이렉션 URL 감지
                        if (url.startsWith(redirectUrl)) {
                            val uri = Uri.parse(url)
                            val resultId = uri.getQueryParameter("identityVerificationId")
                            val code = uri.getQueryParameter("code")

                            view?.post {
                                if (code != null) {
                                    // 에러 발생
                                    val message = uri.getQueryParameter("message") ?: "본인인증에 실패했습니다"
                                    onFail(message)
                                } else if (resultId != null) {
                                    // 성공
                                    onSuccess(resultId)
                                } else {
                                    onFail("인증 결과를 확인할 수 없습니다")
                                }
                            }
                            return true
                        }
                        return false
                    }
                }

                loadDataWithBaseURL(
                    "https://service.portone.io",
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

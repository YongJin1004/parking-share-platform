package com.parking.share.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }

    // 전화번호 포맷팅 함수 (010-XXXX-XXXX 형식)
    fun formatPhoneNumber(input: String): String {
        val digits = input.filter { it.isDigit() }
        return when {
            digits.length <= 3 -> digits
            digits.length <= 7 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
            else -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7, minOf(11, digits.length))}"
        }
    }

    // 전화번호 유효성 검사
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.matches(Regex("^010-\\d{4}-\\d{4}$"))
    }

    // 회원가입 성공 시 로그인 화면으로 이동
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onRegisterSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("회원가입") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 이메일 입력
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("이메일") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 비밀번호 입력
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호 (최소 8자)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 이름 입력
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("이름") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 전화번호 입력
            OutlinedTextField(
                value = phone,
                onValueChange = { newValue ->
                    val formatted = formatPhoneNumber(newValue)
                    if (formatted.length <= 13) { // 010-XXXX-XXXX = 13자
                        phone = formatted
                        phoneError = if (formatted.isNotEmpty() && !isValidPhoneNumber(formatted)) {
                            "010-XXXX-XXXX 형식으로 입력해주세요"
                        } else {
                            null
                        }
                    }
                },
                label = { Text("전화번호") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                placeholder = { Text("010-1234-5678") },
                isError = phoneError != null,
                supportingText = phoneError?.let { { Text(it) } }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 회원가입 버튼
            Button(
                onClick = {
                    if (isValidPhoneNumber(phone)) {
                        viewModel.register(email, password, name, phone)
                    } else {
                        phoneError = "올바른 전화번호 형식이 아닙니다"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading &&
                        email.isNotBlank() &&
                        password.isNotBlank() &&
                        name.isNotBlank() &&
                        phone.isNotBlank() &&
                        isValidPhoneNumber(phone)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("회원가입")
                }
            }

            // 상태 표시
            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Text(
                        text = "회원가입 처리 중...",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                uiState.isSuccess -> {
                    Text(
                        text = "회원가입 성공! 로그인 화면으로 이동합니다.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = "에러: ${uiState.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

package com.parking.share.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parking.share.data.remote.api.AuthApi
import com.parking.share.data.remote.dto.RegisterWithCertRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterWithCertUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class RegisterWithCertViewModel @Inject constructor(
    private val authApi: AuthApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterWithCertUiState())
    val uiState: StateFlow<RegisterWithCertUiState> = _uiState.asStateFlow()

    fun register(email: String, password: String, impUid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                Log.d("RegisterWithCertVM", "회원가입 시작: email=$email, impUid=$impUid")

                val response = authApi.registerWithCert(
                    RegisterWithCertRequest(
                        email = email,
                        password = password,
                        impUid = impUid
                    )
                )

                Log.d("RegisterWithCertVM", "회원가입 성공: ${response.name}")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            } catch (e: Exception) {
                Log.e("RegisterWithCertVM", "회원가입 실패", e)

                val errorMessage = when {
                    e.message?.contains("400") == true -> "이미 가입된 이메일 또는 전화번호입니다"
                    e.message?.contains("422") == true -> "입력 정보를 확인해주세요"
                    else -> "회원가입에 실패했습니다: ${e.message}"
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMessage
                )
            }
        }
    }
}

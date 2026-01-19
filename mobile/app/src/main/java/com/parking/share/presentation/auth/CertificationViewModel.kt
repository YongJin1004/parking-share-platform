package com.parking.share.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parking.share.data.remote.api.AuthApi
import com.parking.share.data.remote.dto.CertificationVerifyRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CertificationUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val impUid: String? = null,
    val certifiedName: String? = null,
    val certifiedPhone: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class CertificationViewModel @Inject constructor(
    private val authApi: AuthApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(CertificationUiState())
    val uiState: StateFlow<CertificationUiState> = _uiState.asStateFlow()

    fun startLoading() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )
    }

    fun verifyCertification(impUid: String) {
        viewModelScope.launch {
            try {
                Log.d("CertificationVM", "본인인증 검증 시작: impUid=$impUid")

                val response = authApi.verifyCertification(
                    CertificationVerifyRequest(impUid = impUid)
                )

                Log.d("CertificationVM", "본인인증 검증 성공: name=${response.name}, phone=${response.phone}")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    impUid = impUid,
                    certifiedName = response.name,
                    certifiedPhone = response.phone
                )
            } catch (e: Exception) {
                Log.e("CertificationVM", "본인인증 검증 실패", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "인증 정보 확인에 실패했습니다: ${e.message}"
                )
            }
        }
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = message
        )
    }
}

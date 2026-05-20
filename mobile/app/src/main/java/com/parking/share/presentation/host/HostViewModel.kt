package com.parking.share.presentation.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parking.share.domain.model.ParkingSpace
import com.parking.share.domain.repository.ParkingSpaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HostUiState(
    val isLoading: Boolean = false,
    val parkingSpaces: List<ParkingSpace> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HostViewModel @Inject constructor(
    private val parkingSpaceRepository: ParkingSpaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HostUiState())
    val uiState: StateFlow<HostUiState> = _uiState.asStateFlow()

    init {
        loadMyParkingSpaces()
    }

    fun loadMyParkingSpaces() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            parkingSpaceRepository.getMyParkingSpaces()
                .onSuccess { spaces ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        parkingSpaces = spaces
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "주차 공간 목록을 불러올 수 없습니다."
                    )
                }
        }
    }

    fun deleteParkingSpace(id: Int) {
        viewModelScope.launch {
            // 낙관적 업데이트: 서버 응답 전에 즉시 목록에서 제거
            _uiState.value = _uiState.value.copy(
                parkingSpaces = _uiState.value.parkingSpaces.filter { it.id != id }
            )

            parkingSpaceRepository.deleteParkingSpace(id)
                .onFailure { e ->
                    // 실패 시 목록 다시 로드하여 복원
                    loadMyParkingSpaces()
                    _uiState.value = _uiState.value.copy(error = e.message ?: "삭제에 실패했습니다.")
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

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
            _uiState.value = _uiState.value.copy(isLoading = true)

            parkingSpaceRepository.deleteParkingSpace(id)
                .onSuccess {
                    loadMyParkingSpaces()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "삭제에 실패했습니다."
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

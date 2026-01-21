package com.parking.share.presentation.host

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parking.share.data.remote.dto.ScheduleItemDto
import com.parking.share.domain.repository.ParkingSpaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DateTimeSlot(
    val startTime: String = "09:00",
    val endTime: String = "18:00",
    val hourlyRate: String = ""
)

data class AddParkingSpaceUiState(
    val address: String = "",
    val description: String = "",
    val calendarYear: Int = currentYear(),
    val calendarMonth: Int = currentMonth(),
    val selectedDates: Map<String, DateTimeSlot> = emptyMap(),
    val selectedVehicleTypes: Set<String> = emptySet(),
    val selectedImageUris: List<Uri> = emptyList(),
    val showAddressSearch: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

private fun currentYear() = Calendar.getInstance().get(Calendar.YEAR)
private fun currentMonth() = Calendar.getInstance().get(Calendar.MONTH) + 1

@HiltViewModel
class AddParkingSpaceViewModel @Inject constructor(
    private val parkingSpaceRepository: ParkingSpaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddParkingSpaceUiState())
    val uiState: StateFlow<AddParkingSpaceUiState> = _uiState.asStateFlow()

    fun onAddressSelected(address: String) {
        _uiState.value = _uiState.value.copy(address = address, showAddressSearch = false, error = null)
    }

    fun onDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun onShowAddressSearch(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddressSearch = show)
    }

    fun onPrevMonth() {
        val s = _uiState.value
        val (year, month) = if (s.calendarMonth == 1) s.calendarYear - 1 to 12
                            else s.calendarYear to s.calendarMonth - 1
        _uiState.value = s.copy(calendarYear = year, calendarMonth = month)
    }

    fun onNextMonth() {
        val s = _uiState.value
        val (year, month) = if (s.calendarMonth == 12) s.calendarYear + 1 to 1
                            else s.calendarYear to s.calendarMonth + 1
        _uiState.value = s.copy(calendarYear = year, calendarMonth = month)
    }

    fun onDateToggle(dateStr: String) {
        val current = _uiState.value.selectedDates.toMutableMap()
        if (current.containsKey(dateStr)) current.remove(dateStr)
        else current[dateStr] = DateTimeSlot()
        _uiState.value = _uiState.value.copy(selectedDates = current)
    }

    fun onStartTimeChange(dateStr: String, time: String) {
        update(dateStr) { it.copy(startTime = time) }
    }

    fun onEndTimeChange(dateStr: String, time: String) {
        update(dateStr) { it.copy(endTime = time) }
    }

    fun onHourlyRateChange(dateStr: String, rate: String) {
        update(dateStr) { it.copy(hourlyRate = rate.filter { c -> c.isDigit() }) }
    }

    fun onVehicleTypeToggle(type: String) {
        val current = _uiState.value.selectedVehicleTypes.toMutableSet()
        if (current.contains(type)) current.remove(type) else current.add(type)
        _uiState.value = _uiState.value.copy(selectedVehicleTypes = current)
    }

    fun onImagesSelected(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(selectedImageUris = uris)
    }

    private fun update(dateStr: String, block: (DateTimeSlot) -> DateTimeSlot) {
        val current = _uiState.value.selectedDates.toMutableMap()
        current[dateStr]?.let { current[dateStr] = block(it) }
        _uiState.value = _uiState.value.copy(selectedDates = current)
    }

    fun createParkingSpace() {
        val state = _uiState.value

        if (state.address.isBlank()) {
            _uiState.value = state.copy(error = "주소를 검색해주세요.")
            return
        }
        if (state.selectedDates.isEmpty()) {
            _uiState.value = state.copy(error = "운영할 날짜를 선택해주세요.")
            return
        }
        val invalidRate = state.selectedDates.values.any {
            it.hourlyRate.toIntOrNull() == null || it.hourlyRate.toIntOrNull()!! <= 0
        }
        if (invalidRate) {
            _uiState.value = state.copy(error = "모든 날짜의 요금을 입력해주세요.")
            return
        }

        val schedule = state.selectedDates.entries.sortedBy { it.key }.map { (date, slot) ->
            ScheduleItemDto(
                date = date,
                startTime = slot.startTime,
                endTime = slot.endTime,
                hourlyRate = slot.hourlyRate.toInt()
            )
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            parkingSpaceRepository.createParkingSpace(
                address = state.address,
                hourlyRate = null,
                description = state.description.takeIf { it.isNotBlank() },
                availableSchedule = schedule,
                allowedVehicleTypes = state.selectedVehicleTypes.toList().takeIf { it.isNotEmpty() }
            ).onSuccess { createdSpace ->
                if (state.selectedImageUris.isNotEmpty()) {
                    parkingSpaceRepository.uploadImages(createdSpace.id, state.selectedImageUris)
                }
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "등록에 실패했습니다.")
            }
        }
    }
}

package com.parking.share.domain.model

data class ScheduleItem(
    val day: String,
    val startTime: String,
    val endTime: String
)

data class ParkingSpace(
    val id: Int,
    val hostId: Int,
    val title: String,
    val address: String,
    val latitude: String?,
    val longitude: String?,
    val hourlyRate: Int,
    val description: String?,
    val isAvailable: Boolean,
    val availableSchedule: List<ScheduleItem>?
)

package com.parking.share.data.remote.dto

import com.parking.share.domain.model.ParkingSpace
import com.parking.share.domain.model.ScheduleItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleItemDto(
    val date: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("hourly_rate") val hourlyRate: Int
)

@Serializable
data class ParkingSpaceResponse(
    val id: Int,
    @SerialName("host_id") val hostId: Int,
    val title: String,
    val address: String,
    val latitude: String? = null,
    val longitude: String? = null,
    @SerialName("hourly_rate") val hourlyRate: Int,
    val description: String? = null,
    @SerialName("is_available") val isAvailable: Boolean,
    @SerialName("available_schedule") val availableSchedule: List<ScheduleItemDto>? = null,
    @SerialName("allowed_vehicle_types") val allowedVehicleTypes: List<String>? = null,
    val images: List<String>? = null
)

@Serializable
data class ParkingSpaceCreateRequest(
    val address: String,
    val description: String? = null,
    @SerialName("is_available") val isAvailable: Boolean = true,
    @SerialName("available_schedule") val availableSchedule: List<ScheduleItemDto>? = null,
    @SerialName("allowed_vehicle_types") val allowedVehicleTypes: List<String>? = null
)

fun ParkingSpaceResponse.toDomain(): ParkingSpace {
    return ParkingSpace(
        id = id,
        hostId = hostId,
        title = title,
        address = address,
        latitude = latitude,
        longitude = longitude,
        hourlyRate = hourlyRate,
        description = description,
        isAvailable = isAvailable,
        availableSchedule = availableSchedule?.map { ScheduleItem(it.date, it.startTime, it.endTime) }
    )
}

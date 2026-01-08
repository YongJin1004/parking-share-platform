package com.parking.share.data.remote.dto

import com.parking.share.domain.model.ParkingSpace
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParkingSpaceResponse(
    val id: Int,

    @SerialName("host_id")
    val hostId: Int,

    val title: String,
    val address: String,
    val latitude: String? = null,
    val longitude: String? = null,

    @SerialName("hourly_rate")
    val hourlyRate: Int,

    val description: String? = null,

    @SerialName("is_available")
    val isAvailable: Boolean
)

@Serializable
data class ParkingSpaceCreateRequest(
    val title: String,
    val address: String,

    @SerialName("hourly_rate")
    val hourlyRate: Int,

    val description: String? = null,

    @SerialName("is_available")
    val isAvailable: Boolean = true
)

// Mapper
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
        isAvailable = isAvailable
    )
}

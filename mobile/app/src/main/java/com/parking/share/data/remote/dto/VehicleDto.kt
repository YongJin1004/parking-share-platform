package com.parking.share.data.remote.dto

import com.parking.share.domain.model.Vehicle
import com.parking.share.domain.model.VehicleType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VehicleResponse(
    val id: Int,

    @SerialName("user_id")
    val userId: Int,

    @SerialName("vehicle_number")
    val vehicleNumber: String,

    @SerialName("vehicle_type")
    val vehicleType: String,

    val nickname: String? = null,

    @SerialName("is_default")
    val isDefault: Boolean
)

@Serializable
data class VehicleCreateRequest(
    @SerialName("vehicle_number")
    val vehicleNumber: String,

    @SerialName("vehicle_type")
    val vehicleType: String,

    val nickname: String? = null,

    @SerialName("is_default")
    val isDefault: Boolean = false
)

// Mapper
fun VehicleResponse.toDomain(): Vehicle {
    return Vehicle(
        id = id,
        userId = userId,
        vehicleNumber = vehicleNumber,
        vehicleType = VehicleType.valueOf(vehicleType.uppercase()),
        nickname = nickname,
        isDefault = isDefault
    )
}

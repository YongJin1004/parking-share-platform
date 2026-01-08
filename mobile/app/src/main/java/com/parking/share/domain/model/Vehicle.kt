package com.parking.share.domain.model

data class Vehicle(
    val id: Int,
    val userId: Int,
    val vehicleNumber: String,
    val vehicleType: VehicleType,
    val nickname: String?,
    val isDefault: Boolean
)

enum class VehicleType {
    SEDAN,
    SUV,
    VAN,
    TRUCK,
    MOTORCYCLE
}

package com.parking.share.domain.model

data class ParkingSpace(
    val id: Int,
    val hostId: Int,
    val title: String,
    val address: String,
    val latitude: String?,
    val longitude: String?,
    val hourlyRate: Int,
    val description: String?,
    val isAvailable: Boolean
)

package com.parking.share.domain.model

data class Reservation(
    val id: Int,
    val guestId: Int,
    val parkingSpaceId: Int,
    val vehicleId: Int?,
    val status: ReservationStatus,
    val totalAmount: Int
)

enum class ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}

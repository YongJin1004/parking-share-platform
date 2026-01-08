package com.parking.share.data.remote.dto

import com.parking.share.domain.model.Reservation
import com.parking.share.domain.model.ReservationStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReservationResponse(
    val id: Int,

    @SerialName("guest_id")
    val guestId: Int,

    @SerialName("parking_space_id")
    val parkingSpaceId: Int,

    @SerialName("vehicle_id")
    val vehicleId: Int? = null,

    val status: String,

    @SerialName("total_amount")
    val totalAmount: Int
)

@Serializable
data class ReservationCreateRequest(
    @SerialName("parking_space_id")
    val parkingSpaceId: Int,

    @SerialName("vehicle_id")
    val vehicleId: Int? = null
)

// Mapper
fun ReservationResponse.toDomain(): Reservation {
    return Reservation(
        id = id,
        guestId = guestId,
        parkingSpaceId = parkingSpaceId,
        vehicleId = vehicleId,
        status = ReservationStatus.valueOf(status.uppercase()),
        totalAmount = totalAmount
    )
}

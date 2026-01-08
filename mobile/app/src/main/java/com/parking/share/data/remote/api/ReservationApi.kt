package com.parking.share.data.remote.api

import com.parking.share.data.remote.dto.ReservationCreateRequest
import com.parking.share.data.remote.dto.ReservationResponse
import retrofit2.http.*

interface ReservationApi {
    @GET("reservations")
    suspend fun getMyReservations(): List<ReservationResponse>

    @POST("reservations")
    suspend fun createReservation(@Body request: ReservationCreateRequest): ReservationResponse

    @DELETE("reservations/{id}")
    suspend fun cancelReservation(@Path("id") id: Int)

    @PATCH("reservations/{id}/complete")
    suspend fun completeReservation(@Path("id") id: Int): ReservationResponse
}

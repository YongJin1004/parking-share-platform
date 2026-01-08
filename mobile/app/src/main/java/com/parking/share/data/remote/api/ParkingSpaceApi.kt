package com.parking.share.data.remote.api

import com.parking.share.data.remote.dto.ParkingSpaceCreateRequest
import com.parking.share.data.remote.dto.ParkingSpaceResponse
import retrofit2.http.*

interface ParkingSpaceApi {
    @GET("parking-spaces")
    suspend fun getMyParkingSpaces(): List<ParkingSpaceResponse>

    @POST("parking-spaces")
    suspend fun createParkingSpace(@Body request: ParkingSpaceCreateRequest): ParkingSpaceResponse

    @GET("parking-spaces/search")
    suspend fun searchParkingSpaces(
        @Query("keyword") keyword: String? = null,
        @Query("min_hourly_rate") minHourlyRate: Int? = null,
        @Query("max_hourly_rate") maxHourlyRate: Int? = null,
        @Query("is_available") isAvailable: Boolean? = true
    ): List<ParkingSpaceResponse>

    @DELETE("parking-spaces/{id}")
    suspend fun deleteParkingSpace(@Path("id") id: Int)
}

package com.parking.share.data.remote.api

import com.parking.share.data.remote.dto.VehicleCreateRequest
import com.parking.share.data.remote.dto.VehicleResponse
import retrofit2.http.*

interface VehicleApi {
    @GET("vehicles")
    suspend fun getMyVehicles(): List<VehicleResponse>

    @POST("vehicles")
    suspend fun createVehicle(@Body request: VehicleCreateRequest): VehicleResponse

    @DELETE("vehicles/{id}")
    suspend fun deleteVehicle(@Path("id") id: Int)
}

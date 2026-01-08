package com.parking.share.data.remote.api

import com.parking.share.data.remote.dto.LoginRequest
import com.parking.share.data.remote.dto.RegisterRequest
import com.parking.share.data.remote.dto.TokenResponse
import com.parking.share.data.remote.dto.UserResponse
import retrofit2.http.*

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponse

    @GET("auth/me")
    suspend fun getCurrentUser(): UserResponse
}

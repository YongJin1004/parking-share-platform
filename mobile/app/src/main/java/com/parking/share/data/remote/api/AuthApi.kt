package com.parking.share.data.remote.api

import com.parking.share.data.remote.dto.CertificationVerifyRequest
import com.parking.share.data.remote.dto.CertificationVerifyResponse
import com.parking.share.data.remote.dto.LoginRequest
import com.parking.share.data.remote.dto.RegisterRequest
import com.parking.share.data.remote.dto.RegisterWithCertRequest
import com.parking.share.data.remote.dto.TokenResponse
import com.parking.share.data.remote.dto.UserResponse
import retrofit2.http.*

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @POST("auth/register-with-cert")
    suspend fun registerWithCert(@Body request: RegisterWithCertRequest): UserResponse

    @POST("auth/verify-certification")
    suspend fun verifyCertification(@Body request: CertificationVerifyRequest): CertificationVerifyResponse

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponse

    @GET("auth/me")
    suspend fun getCurrentUser(): UserResponse
}

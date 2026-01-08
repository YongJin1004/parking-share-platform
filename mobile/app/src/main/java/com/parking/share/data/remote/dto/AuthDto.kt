package com.parking.share.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,  // OAuth2 표준: email을 username으로 전송
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String
)

@Serializable
data class TokenResponse(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("token_type")
    val tokenType: String
)

@Serializable
data class UserResponse(
    val id: Int,
    val email: String,
    val name: String,
    val phone: String,

    @SerialName("phone_verified")
    val phoneVerified: Boolean,

    @SerialName("manner_score")
    val mannerScore: Int,

    @SerialName("total_reviews")
    val totalReviews: Int,

    val status: String
)

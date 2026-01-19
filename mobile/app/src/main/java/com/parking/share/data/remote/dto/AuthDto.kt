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

// 본인인증 후 회원가입 요청
@Serializable
data class RegisterWithCertRequest(
    val email: String,
    val password: String,
    @SerialName("imp_uid")
    val impUid: String
)

// 본인인증 검증 요청
@Serializable
data class CertificationVerifyRequest(
    @SerialName("imp_uid")
    val impUid: String
)

// 본인인증 검증 응답
@Serializable
data class CertificationVerifyResponse(
    val name: String,
    val phone: String,
    val certified: Boolean
)

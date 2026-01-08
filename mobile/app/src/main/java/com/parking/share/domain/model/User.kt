package com.parking.share.domain.model

data class User(
    val id: Int,
    val email: String,
    val name: String,
    val phone: String,
    val phoneVerified: Boolean,
    val mannerScore: Int,
    val totalReviews: Int,
    val status: String
)

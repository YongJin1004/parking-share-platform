package com.parking.share.domain.repository

import com.parking.share.domain.model.User

interface AuthRepository {
    suspend fun register(email: String, password: String, name: String, phone: String): Result<User>
    suspend fun login(email: String, password: String): Result<String>
    suspend fun getCurrentUser(): Result<User>
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
}

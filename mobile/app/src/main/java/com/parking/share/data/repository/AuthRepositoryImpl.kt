package com.parking.share.data.repository

import com.parking.share.data.local.TokenManager
import com.parking.share.data.remote.api.AuthApi
import com.parking.share.data.remote.dto.RegisterRequest
import com.parking.share.domain.model.User
import com.parking.share.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun register(
        email: String,
        password: String,
        name: String,
        phone: String
    ): Result<User> {
        return try {
            val response = authApi.register(
                RegisterRequest(
                    email = email,
                    password = password,
                    name = name,
                    phone = phone
                )
            )
            val user = User(
                id = response.id,
                email = response.email,
                name = response.name,
                phone = response.phone,
                phoneVerified = response.phoneVerified,
                mannerScore = response.mannerScore,
                totalReviews = response.totalReviews,
                status = response.status
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = authApi.login(username = email, password = password)
            tokenManager.saveToken(response.accessToken, response.tokenType)
            Result.success(response.accessToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Result<User> {
        return try {
            val response = authApi.getCurrentUser()
            val user = User(
                id = response.id,
                email = response.email,
                name = response.name,
                phone = response.phone,
                phoneVerified = response.phoneVerified,
                mannerScore = response.mannerScore,
                totalReviews = response.totalReviews,
                status = response.status
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        tokenManager.clearToken()
    }

    override suspend fun isLoggedIn(): Boolean {
        return tokenManager.accessToken.first() != null
    }
}

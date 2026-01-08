package com.parking.share.domain.usecase

import com.parking.share.domain.model.User
import com.parking.share.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        name: String,
        phone: String
    ): Result<User> {
        return authRepository.register(email, password, name, phone)
    }
}

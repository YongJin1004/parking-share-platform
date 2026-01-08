package com.parking.share.domain.usecase

import com.parking.share.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<String> {
        return authRepository.login(email, password)
    }
}

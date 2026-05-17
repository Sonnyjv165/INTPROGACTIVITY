package com.example.intprogactivity.domain.usecase.auth

import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.util.Result
import com.example.intprogactivity.util.isValidEmail
import javax.inject.Inject

class ForgotPasswordUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (!email.isValidEmail())
            return Result.Error(Exception("Please enter a valid email address."))
        return authRepository.sendPasswordResetEmail(email)
    }
}

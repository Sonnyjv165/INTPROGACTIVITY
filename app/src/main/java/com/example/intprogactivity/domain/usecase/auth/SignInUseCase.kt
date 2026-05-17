package com.example.intprogactivity.domain.usecase.auth

import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.util.Result
import com.example.intprogactivity.util.isValidEmail
import javax.inject.Inject

class SignInUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank() || password.isBlank())
            return Result.Error(Exception("Email and password are required."))
        if (!email.isValidEmail())
            return Result.Error(Exception("Please enter a valid email address."))
        return authRepository.signInWithEmail(email, password)
    }
}

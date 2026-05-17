package com.example.intprogactivity.domain.usecase.auth

import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.util.Result
import com.example.intprogactivity.util.isValidEmail
import com.example.intprogactivity.util.isValidPassword
import javax.inject.Inject

class RegisterUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String,
        displayName: String
    ): Result<User> {
        if (displayName.isBlank())
            return Result.Error(Exception("Full name is required."))
        if (!email.isValidEmail())
            return Result.Error(Exception("Please enter a valid email address."))
        if (!password.isValidPassword())
            return Result.Error(Exception("Password must be at least 8 characters, include an uppercase letter and a number."))
        if (password != confirmPassword)
            return Result.Error(Exception("Passwords do not match."))
        return authRepository.register(email, password, displayName)
    }
}

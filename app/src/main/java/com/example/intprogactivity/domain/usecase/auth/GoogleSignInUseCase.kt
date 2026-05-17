package com.example.intprogactivity.domain.usecase.auth

import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.util.Result
import javax.inject.Inject

class GoogleSignInUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(idToken: String): Result<User> =
        authRepository.signInWithGoogle(idToken)
}

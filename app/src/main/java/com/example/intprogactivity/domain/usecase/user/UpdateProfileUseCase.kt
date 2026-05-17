package com.example.intprogactivity.domain.usecase.user

import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.domain.repository.UserRepository
import com.example.intprogactivity.util.Result
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(user: User, displayName: String, phone: String): Result<Unit> {
        if (displayName.isBlank()) return Result.Error(Exception("Name cannot be empty."))
        return userRepository.updateUserProfile(user.copy(displayName = displayName, phone = phone.ifBlank { null }))
    }
}

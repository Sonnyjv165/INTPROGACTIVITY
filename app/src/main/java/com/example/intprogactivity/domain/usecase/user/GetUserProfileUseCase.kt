package com.example.intprogactivity.domain.usecase.user

import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(uid: String): Flow<User?> = userRepository.getUserProfileFlow(uid)
}

package com.example.intprogactivity.domain.usecase.rewards

import com.example.intprogactivity.domain.model.TripCoinTransaction
import com.example.intprogactivity.domain.repository.UserRepository
import com.example.intprogactivity.util.Result
import javax.inject.Inject

class GetCoinHistoryUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(uid: String): Result<List<TripCoinTransaction>> =
        userRepository.getTripCoinHistory(uid)
}

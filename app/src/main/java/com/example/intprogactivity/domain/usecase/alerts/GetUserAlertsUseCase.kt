package com.example.intprogactivity.domain.usecase.alerts

import com.example.intprogactivity.domain.model.PriceAlert
import com.example.intprogactivity.domain.repository.PriceAlertRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserAlertsUseCase @Inject constructor(
    private val priceAlertRepository: PriceAlertRepository
) {
    operator fun invoke(userId: String): Flow<List<PriceAlert>> =
        priceAlertRepository.getUserAlertsFlow(userId)
}

package com.example.intprogactivity.domain.usecase.alerts

import com.example.intprogactivity.domain.model.PriceAlert
import com.example.intprogactivity.domain.repository.PriceAlertRepository
import com.example.intprogactivity.util.Result
import javax.inject.Inject

class CreateAlertUseCase @Inject constructor(
    private val priceAlertRepository: PriceAlertRepository
) {
    suspend operator fun invoke(alert: PriceAlert): Result<PriceAlert> {
        if (alert.origin.isBlank() || alert.destination.isBlank())
            return Result.Error(Exception("Origin and destination are required."))
        if (alert.targetPrice <= 0)
            return Result.Error(Exception("Target price must be greater than 0."))
        return priceAlertRepository.createAlert(alert)
    }
}

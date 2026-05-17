package com.example.intprogactivity.domain.repository

import com.example.intprogactivity.domain.model.MembershipTier
import com.example.intprogactivity.domain.model.TripCoinTransaction
import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.util.Result
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUserProfile(uid: String): Result<User>
    suspend fun createUserProfile(user: User): Result<Unit>
    suspend fun updateUserProfile(user: User): Result<Unit>
    suspend fun updateMembershipTier(uid: String, tier: MembershipTier): Result<Unit>
    suspend fun addTripCoins(uid: String, amount: Int, description: String, bookingId: String?): Result<Unit>
    suspend fun incrementBookingStats(uid: String, spend: Double): Result<Unit>
    suspend fun getTripCoinHistory(uid: String): Result<List<TripCoinTransaction>>
    fun getUserProfileFlow(uid: String): Flow<User?>
}

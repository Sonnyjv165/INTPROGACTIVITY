package com.example.intprogactivity.data.repository

import com.example.intprogactivity.data.remote.firebase.FirestoreUserSource
import com.example.intprogactivity.data.remote.firebase.toUser
import com.example.intprogactivity.domain.model.MembershipTier
import com.example.intprogactivity.domain.model.TripCoinTransaction
import com.example.intprogactivity.domain.model.TransactionType
import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.domain.repository.UserRepository
import com.example.intprogactivity.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userSource: FirestoreUserSource
) : UserRepository {

    override suspend fun getUserProfile(uid: String): Result<User> = try {
        val data = userSource.getUser(uid)
        if (data != null) Result.Success(data.toUser(uid))
        else Result.Error(Exception("User not found"))
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override suspend fun createUserProfile(user: User): Result<Unit> = try {
        val data = mapOf(
            "email" to user.email,
            "displayName" to user.displayName,
            "membershipTier" to user.membershipTier.name,
            "tripCoins" to user.tripCoins,
            "totalBookings" to user.totalBookings,
            "totalSpend" to user.totalSpend,
            "createdAt" to user.createdAt
        )
        userSource.createUser(user.uid, data)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override suspend fun updateUserProfile(user: User): Result<Unit> = try {
        val updates = mutableMapOf<String, Any>(
            "displayName" to user.displayName
        )
        user.phone?.let { updates["phone"] = it }
        userSource.updateUser(user.uid, updates)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override suspend fun updateMembershipTier(uid: String, tier: MembershipTier): Result<Unit> = try {
        val updates = mutableMapOf<String, Any>("membershipTier" to tier.name)
        if (tier in setOf(MembershipTier.PLATINUM, MembershipTier.DIAMOND, MembershipTier.DIAMOND_PLUS)) {
            updates["tierExpiryDate"] = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000
        }
        userSource.updateUser(uid, updates)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override suspend fun addTripCoins(
        uid: String, amount: Int, description: String, bookingId: String?
    ): Result<Unit> = try {
        userSource.awardCoins(uid, amount, description, bookingId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override suspend fun incrementBookingStats(uid: String, spend: Double): Result<Unit> = try {
        userSource.incrementBookingStats(uid, spend)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override suspend fun getTripCoinHistory(uid: String): Result<List<TripCoinTransaction>> = try {
        val rawList = userSource.getCoinHistory(uid)
        val txList = rawList.map { data ->
            TripCoinTransaction(
                transactionId = data["transactionId"] as? String ?: "",
                userId = uid,
                amount = (data["amount"] as? Long)?.toInt() ?: 0,
                type = TransactionType.valueOf(data["type"] as? String ?: "EARNED"),
                description = data["description"] as? String ?: "",
                bookingId = data["bookingId"] as? String,
                createdAt = data["createdAt"] as? Long ?: 0L
            )
        }
        Result.Success(txList)
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override fun getUserProfileFlow(uid: String): Flow<User?> = userSource.getUserFlow(uid)
}

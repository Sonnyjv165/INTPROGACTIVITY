package com.example.intprogactivity.data.repository

import com.example.intprogactivity.data.remote.firebase.FirebaseAuthSource
import com.example.intprogactivity.data.remote.firebase.FirestoreUserSource
import com.example.intprogactivity.data.remote.firebase.toUser
import com.example.intprogactivity.domain.model.MembershipTier
import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authSource: FirebaseAuthSource,
    private val userSource: FirestoreUserSource
) : AuthRepository {

    override val currentUser: Flow<User?> = authSource.currentFirebaseUser.flatMapLatest { fbUser ->
        if (fbUser == null) flowOf(null)
        else userSource.getUserFlow(fbUser.uid)
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> = try {
        val fbUser = authSource.signInWithEmail(email, password)
        val userData = userSource.getUser(fbUser.uid)
        val user = userData?.toUser(fbUser.uid)
            ?: User(uid = fbUser.uid, email = fbUser.email ?: email)
        Result.Success(user)
    } catch (e: Exception) {
        Result.Error(e, mapFirebaseError(e))
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> = try {
        val credential = authSource.googleCredential(idToken)
        val fbUser = authSource.signInWithCredential(credential)
        var userData = userSource.getUser(fbUser.uid)
        if (userData == null) {
            // Split Firebase displayName into first/last for the web-compatible schema
            val parts = (fbUser.displayName ?: "").trim().split(" ", limit = 2)
            val newUser = buildUserMap(
                uid       = fbUser.uid,
                email     = fbUser.email ?: "",
                firstName = parts.getOrNull(0) ?: "",
                lastName  = parts.getOrNull(1) ?: "",
                providerId = fbUser.providerId
            )
            userSource.createUser(fbUser.uid, newUser)
            userData = newUser.filterValues { it != null } as Map<String, Any>
        }
        Result.Success(userData.toUser(fbUser.uid))
    } catch (e: Exception) {
        Result.Error(e, mapFirebaseError(e))
    }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<User> = try {
        val fbUser = authSource.createUserWithEmail(email, password)
        val userMap = buildUserMap(fbUser.uid, email, firstName, lastName, null)
        userSource.createUser(fbUser.uid, userMap)
        Result.Success((userMap.filterValues { it != null } as Map<String, Any>).toUser(fbUser.uid))
    } catch (e: Exception) {
        Result.Error(e, mapFirebaseError(e))
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        authSource.sendPasswordReset(email)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e, mapFirebaseError(e))
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = try {
        authSource.changePassword(currentPassword, newPassword)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e, mapFirebaseError(e))
    }

    override suspend fun signOut() = authSource.signOut()

    override suspend fun getCurrentUser(): User? {
        val fbUser = authSource.getCurrentFirebaseUser() ?: return null
        return userSource.getUser(fbUser.uid)?.toUser(fbUser.uid)
    }

    override fun isUserLoggedIn(): Boolean = authSource.getCurrentFirebaseUser() != null

    private fun buildUserMap(
        uid: String,
        email: String,
        firstName: String,
        lastName: String,
        providerId: String?
    ): Map<String, Any?> = mapOf(
        "uid"           to uid,
        "email"         to email,
        "firstName"     to firstName,
        "lastName"      to lastName,
        "middleInitial" to "",
        "suffix"        to "",
        "displayName"   to "$firstName $lastName".trim(),
        "dob"           to "",
        "phone"         to "",
        "nationality"   to "",
        "photoUrl"      to "",
        "providerId"    to providerId,
        "role"          to "user",
        "status"        to "ACTIVE",
        "membershipTier" to MembershipTier.SILVER.name,
        "loyaltyPoints" to 0,
        "totalBookings" to 0,
        "totalSpend"    to 0.0,
        "createdAt"     to System.currentTimeMillis()
    )

    private fun mapFirebaseError(e: Exception): String = when {
        e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ||
        e.message?.contains("wrong-password") == true ||
        e.message?.contains("user-not-found") == true -> "Invalid email or password."
        e.message?.contains("email-already-in-use") == true -> "An account with this email already exists."
        e.message?.contains("network") == true -> "Network error. Check your connection."
        else -> e.message ?: "Authentication failed."
    }
}

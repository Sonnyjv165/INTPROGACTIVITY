package com.example.intprogactivity.domain.repository

import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.util.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun register(email: String, password: String, displayName: String): Result<User>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun signOut()
    suspend fun getCurrentUser(): User?
    fun isUserLoggedIn(): Boolean
}

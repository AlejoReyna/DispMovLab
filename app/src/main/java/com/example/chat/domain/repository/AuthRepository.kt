package com.example.chat.domain.repository

import com.example.chat.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<UserProfile?>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String, displayName: String): Result<Unit>
    suspend fun signOut()
    suspend fun sendPasswordReset(email: String): Result<Unit>
}

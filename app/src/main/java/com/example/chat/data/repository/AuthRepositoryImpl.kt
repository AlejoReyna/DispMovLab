package com.example.chat.data.repository

import com.example.chat.data.remote.FirebaseAuthDataSource
import com.example.chat.domain.model.UserProfile
import com.example.chat.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val dataSource: FirebaseAuthDataSource
) : AuthRepository {
    override val currentUser: Flow<UserProfile?> = dataSource.currentUser

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        dataSource.signIn(email, password)
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<Unit> = runCatching {
        dataSource.signUp(email, password, displayName)
    }

    override suspend fun signOut() {
        dataSource.signOut()
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        dataSource.sendPasswordReset(email)
    }
}

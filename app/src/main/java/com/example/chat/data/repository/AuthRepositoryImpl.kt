package com.example.chat.data.repository

import android.app.Activity
import com.example.chat.data.remote.FirebaseAuthDataSource
import com.example.chat.domain.model.UserProfile
import com.example.chat.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val dataSource: FirebaseAuthDataSource
) : AuthRepository {
    override val currentUser: Flow<UserProfile?> = dataSource.currentUser

    // ── Email / Password ────────────────────────────────────────────────────

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        dataSource.signIn(email, password)
    }

    override suspend fun signInWithPhone(phoneNumber: String, password: String): Result<Unit> = runCatching {
        dataSource.signInWithPhone(phoneNumber, password)
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

    override suspend fun resendEmailVerification(): Result<Unit> = runCatching {
        dataSource.resendEmailVerification()
    }

    override suspend fun reloadUser(): Result<Unit> = runCatching {
        dataSource.reloadUser()
    }

    // ── Phone Auth (SMS verification) ───────────────────────────────────────

    override suspend fun sendPhoneVerification(
        phoneNumber: String,
        activity: Activity
    ): Result<String?> = runCatching {
        dataSource.sendPhoneVerification(phoneNumber, activity)
    }

    override suspend fun verifyPhoneCode(
        verificationId: String,
        smsCode: String
    ): Result<Unit> = runCatching {
        dataSource.verifyPhoneCode(verificationId, smsCode)
    }

    override suspend fun linkEmailPassword(
        phoneNumber: String,
        displayName: String,
        email: String,
        password: String
    ): Result<Unit> = runCatching {
        dataSource.linkEmailPassword(phoneNumber, displayName, email, password)
    }
}

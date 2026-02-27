package com.example.chat.domain.repository

import android.app.Activity
import com.example.chat.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<UserProfile?>

    // ── Email / Password ────────────────────────────────────────────────────
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signInWithPhone(phoneNumber: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String, displayName: String): Result<Unit>
    suspend fun signOut()
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun resendEmailVerification(): Result<Unit>
    suspend fun reloadUser(): Result<Unit>

    // ── Phone Auth (SMS verification) ───────────────────────────────────────

    /**
     * Sends an SMS code to [phoneNumber].
     * Returns the verificationId on success, or null if the device auto-verified (user already signed in).
     */
    suspend fun sendPhoneVerification(phoneNumber: String, activity: Activity): Result<String?>

    /** Verifies the SMS [smsCode] and signs the user in via phone credential. */
    suspend fun verifyPhoneCode(verificationId: String, smsCode: String): Result<Unit>

    /**
     * Links email/password to the currently phone-authenticated user,
     * sends email verification, and stores the profile in Firestore.
     */
    suspend fun linkEmailPassword(
        phoneNumber: String,
        displayName: String,
        email: String,
        password: String
    ): Result<Unit>
}

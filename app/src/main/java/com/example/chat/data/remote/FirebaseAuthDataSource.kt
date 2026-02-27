package com.example.chat.data.remote

import android.app.Activity
import com.example.chat.domain.model.UserProfile
import com.example.chat.domain.model.UserRole
import com.google.firebase.FirebaseException
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseAuthDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    // Emitting Unit here triggers a manual re-read of the current user (e.g. after email verification reload)
    private val manualRefresh = MutableSharedFlow<Unit>(replay = 0)

    val currentUser: Flow<UserProfile?> = merge(
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user == null) {
                    trySend(null)
                } else {
                    firestore.collection("users").document(user.uid).get()
                        .addOnSuccessListener { doc ->
                            val role = if (doc.getString("role") == "admin") UserRole.ADMIN else UserRole.USER
                            trySend(
                                UserProfile(
                                    uid = user.uid,
                                    email = user.email.orEmpty(),
                                    displayName = user.displayName.orEmpty(),
                                    role = role,
                                    isActive = doc.getBoolean("isActive") ?: true,
                                    isEmailVerified = user.isEmailVerified
                                )
                            )
                        }
                        .addOnFailureListener {
                            trySend(
                                UserProfile(
                                    uid = user.uid,
                                    email = user.email.orEmpty(),
                                    displayName = user.displayName.orEmpty(),
                                    role = UserRole.USER,
                                    isEmailVerified = user.isEmailVerified
                                )
                            )
                        }
                }
            }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        },
        manualRefresh.map { fetchCurrentUserProfile() }
    )

    private suspend fun fetchCurrentUserProfile(): UserProfile? {
        val user = auth.currentUser ?: return null
        return try {
            val doc = firestore.collection("users").document(user.uid).get().await()
            val role = if (doc.getString("role") == "admin") UserRole.ADMIN else UserRole.USER
            UserProfile(
                uid = user.uid,
                email = user.email.orEmpty(),
                displayName = user.displayName.orEmpty(),
                role = role,
                isActive = doc.getBoolean("isActive") ?: true,
                isEmailVerified = user.isEmailVerified
            )
        } catch (e: Exception) {
            UserProfile(
                uid = user.uid,
                email = user.email.orEmpty(),
                displayName = user.displayName.orEmpty(),
                role = UserRole.USER,
                isEmailVerified = user.isEmailVerified
            )
        }
    }

    // ── Email / Password Auth ────────────────────────────────────────────────

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    /** Looks up the email in the public phoneIndex, then signs in with password. */
    suspend fun signInWithPhone(phoneNumber: String, password: String) {
        val doc = firestore.collection("phoneIndex").document(phoneNumber).get().await()
        val email = doc.getString("email")
            ?: throw IllegalArgumentException("No se encontró ninguna cuenta con ese número")
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signUp(email: String, password: String, displayName: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.updateProfile(
            userProfileChangeRequest { this.displayName = displayName }
        )?.await()
        result.user?.sendEmailVerification()?.await()
        val uid = result.user?.uid ?: return
        firestore.collection("users").document(uid).set(
            mapOf(
                "uid" to uid,
                "email" to email,
                "displayName" to displayName,
                "role" to "user",
                "isActive" to true,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    // ── Phone Auth (SMS verification) ────────────────────────────────────────

    /**
     * Sends an SMS verification code to [phoneNumber].
     * Returns the verificationId to use in [verifyPhoneCode], or null if
     * the device auto-verified the code (user already signed in automatically).
     */
    suspend fun sendPhoneVerification(phoneNumber: String, activity: Activity): String? =
        suspendCancellableCoroutine { cont ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Android auto-detected the SMS code — sign in directly
                    auth.signInWithCredential(credential)
                        .addOnSuccessListener { if (cont.isActive) cont.resume(null) }
                        .addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    if (cont.isActive) cont.resume(verificationId)
                }
            }
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }

    /**
     * Verifies the SMS [smsCode] using the [verificationId] from [sendPhoneVerification].
     * Signs the user in with their phone number credential.
     */
    suspend fun verifyPhoneCode(verificationId: String, smsCode: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
        auth.signInWithCredential(credential).await()
    }

    /**
     * Links an email/password credential to the currently phone-authenticated user,
     * sends an email verification message, and stores the profile in Firestore.
     * Must be called after [verifyPhoneCode] (or auto-verification) has signed the user in.
     */
    suspend fun linkEmailPassword(phoneNumber: String, displayName: String, email: String, password: String) {
        val user = auth.currentUser ?: throw IllegalStateException("No authenticated user")
        // Use the explicitly passed phoneNumber instead of user.phoneNumber,
        // because test phone numbers may not populate user.phoneNumber in Firebase Auth.

        // Update display name
        user.updateProfile(userProfileChangeRequest { this.displayName = displayName }).await()

        // Link email/password credential to the phone-authenticated account
        val emailCredential = EmailAuthProvider.getCredential(email, password)
        user.linkWithCredential(emailCredential).await()

        // Send email verification
        user.sendEmailVerification().await()

        // Store profile in Firestore
        firestore.collection("users").document(user.uid).set(
            mapOf(
                "uid" to user.uid,
                "email" to email,
                "displayName" to displayName,
                "phone" to phoneNumber,
                "role" to "user",
                "isActive" to true,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
        ).await()

        // Store phone → email mapping in public index (used for unauthenticated login lookup)
        if (phoneNumber.isNotEmpty()) {
            firestore.collection("phoneIndex").document(phoneNumber).set(
                mapOf("email" to email)
            ).await()
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun resendEmailVerification() {
        auth.currentUser?.sendEmailVerification()?.await()
    }

    suspend fun reloadUser() {
        auth.currentUser?.reload()?.await()
        manualRefresh.emit(Unit)
    }
}

package com.example.chat.data.remote

import com.example.chat.domain.model.UserProfile
import com.example.chat.domain.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser: Flow<UserProfile?> = callbackFlow {
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
                                isActive = doc.getBoolean("isActive") ?: true
                            )
                        )
                    }
                    .addOnFailureListener {
                        trySend(
                            UserProfile(
                                uid = user.uid,
                                email = user.email.orEmpty(),
                                displayName = user.displayName.orEmpty(),
                                role = UserRole.USER
                            )
                        )
                    }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signUp(email: String, password: String, displayName: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.updateProfile(
            userProfileChangeRequest {
                this.displayName = displayName
            }
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
                "createdAt" to com.google.firebase.Timestamp.now(),
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
        ).await()
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }
}

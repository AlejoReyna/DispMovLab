package com.example.chat.core

import com.example.chat.data.remote.FirebaseAuthDataSource
import com.example.chat.data.remote.FirestoreChatDataSource
import com.example.chat.data.remote.VideoCallSignalingDataSource
import com.example.chat.data.repository.AuthRepositoryImpl
import com.example.chat.data.repository.ChatRepositoryImpl
import com.example.chat.domain.repository.AuthRepository
import com.example.chat.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object AppContainer {
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    private val authDataSource by lazy { FirebaseAuthDataSource(firebaseAuth, firestore) }
    private val chatDataSource by lazy {
        FirestoreChatDataSource(
            auth = firebaseAuth,
            firestore = firestore,
            storage = storage
        )
    }

    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(authDataSource) }
    val chatRepository: ChatRepository by lazy { ChatRepositoryImpl(chatDataSource) }

    /** WebRTC signaling via Firestore — used by VideoCallViewModel. */
    val videoCallSignalingDataSource: VideoCallSignalingDataSource by lazy {
        VideoCallSignalingDataSource(firebaseAuth, firestore)
    }
}

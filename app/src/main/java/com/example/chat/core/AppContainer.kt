package com.example.chat.core

import com.example.chat.data.remote.FirebaseAuthDataSource
import com.example.chat.data.remote.FirestoreChatDataSource
import com.example.chat.data.repository.AuthRepositoryImpl
import com.example.chat.data.repository.ChatRepositoryImpl
import com.example.chat.domain.repository.AuthRepository
import com.example.chat.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage

object AppContainer {
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val messaging: FirebaseMessaging by lazy { FirebaseMessaging.getInstance() }

    private val authDataSource by lazy { FirebaseAuthDataSource(firebaseAuth, firestore) }
    private val chatDataSource by lazy {
        FirestoreChatDataSource(
            auth = firebaseAuth,
            firestore = firestore,
            functions = functions,
            storage = storage,
            messaging = messaging
        )
    }

    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(authDataSource) }
    val chatRepository: ChatRepository by lazy { ChatRepositoryImpl(chatDataSource) }
}

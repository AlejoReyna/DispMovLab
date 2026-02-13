package com.example.chat.domain.usecase

import com.example.chat.domain.repository.ChatRepository

class SendMessageUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(conversationId: String, text: String): Result<Unit> =
        repository.sendMessage(conversationId, text)
}

class CreateDirectConversationUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(otherUid: String): Result<String> =
        repository.createDirectConversation(otherUid)
}

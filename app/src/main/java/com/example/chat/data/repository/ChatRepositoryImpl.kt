package com.example.chat.data.repository

import com.example.chat.data.remote.FirestoreChatDataSource
import com.example.chat.domain.model.Conversation
import com.example.chat.domain.model.Message
import com.example.chat.domain.model.ModerationReport
import com.example.chat.domain.model.UserProfile
import com.example.chat.domain.model.UserRole
import com.example.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class ChatRepositoryImpl(
    private val dataSource: FirestoreChatDataSource
) : ChatRepository {
    override fun observeConversations(limit: Long): Flow<List<Conversation>> =
        dataSource.observeConversations(limit)

    override fun observeMessages(conversationId: String, limit: Long): Flow<List<Message>> =
        dataSource.observeMessages(conversationId, limit)

    override suspend fun sendMessage(conversationId: String, text: String): Result<Unit> = runCatching {
        dataSource.sendMessage(conversationId, text)
    }

    override suspend fun createDirectConversation(otherUid: String): Result<String> = runCatching {
        dataSource.createDirectConversation(otherUid)
    }

    override suspend fun getConversationTitle(conversationId: String): Result<String> = runCatching {
        dataSource.getConversationTitle(conversationId)
    }

    override suspend fun markAsRead(conversationId: String, messageId: String): Result<Unit> = runCatching {
        dataSource.markAsRead(conversationId, messageId)
    }

    override suspend fun searchUserByPhone(phoneNumber: String): Result<UserProfile?> = runCatching {
        dataSource.searchUserByPhone(phoneNumber)
    }

    override suspend fun getUsers(limit: Long): Result<List<UserProfile>> = runCatching {
        dataSource.getUsers(limit)
    }

    override suspend fun updateUserActiveState(uid: String, active: Boolean): Result<Unit> = runCatching {
        dataSource.updateUserActiveState(uid, active)
    }

    override suspend fun updateUserRole(uid: String, role: UserRole): Result<Unit> = runCatching {
        dataSource.updateUserRole(uid, role)
    }

    override suspend fun createReport(conversationId: String, messageId: String, reason: String): Result<Unit> = runCatching {
        dataSource.createReport(conversationId, messageId, reason)
    }

    override fun observeOpenReports(limit: Long): Flow<List<ModerationReport>> =
        dataSource.observeOpenReports(limit)
}

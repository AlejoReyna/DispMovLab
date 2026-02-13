package com.example.chat.domain.repository

import com.example.chat.domain.model.Conversation
import com.example.chat.domain.model.Message
import com.example.chat.domain.model.ModerationReport
import com.example.chat.domain.model.UserProfile
import com.example.chat.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeConversations(limit: Long = 30): Flow<List<Conversation>>
    fun observeMessages(conversationId: String, limit: Long = 50): Flow<List<Message>>
    suspend fun sendMessage(conversationId: String, text: String): Result<Unit>
    suspend fun createDirectConversation(otherUid: String): Result<String>
    suspend fun markAsRead(conversationId: String, messageId: String): Result<Unit>
    suspend fun getUsers(limit: Long = 50): Result<List<UserProfile>>
    suspend fun updateUserActiveState(uid: String, active: Boolean): Result<Unit>
    suspend fun updateUserRole(uid: String, role: UserRole): Result<Unit>
    suspend fun createReport(conversationId: String, messageId: String, reason: String): Result<Unit>
    fun observeOpenReports(limit: Long = 50): Flow<List<ModerationReport>>
}

package com.example.chat.domain.model

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val phone: String = "",
    val role: UserRole = UserRole.USER,
    val isActive: Boolean = true,
    val isEmailVerified: Boolean = false
)

enum class UserRole {
    USER,
    ADMIN
}

data class Conversation(
    val id: String,
    val title: String,
    val type: ConversationType = ConversationType.DIRECT,
    val lastMessagePreview: String? = null,
    val updatedAtMillis: Long = 0L
)

enum class ConversationType {
    DIRECT,
    GROUP
}

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val createdAtMillis: Long,
    val status: MessageStatus = MessageStatus.SENT
)

enum class MessageStatus {
    SENT,
    DELIVERED,
    READ,
    FAILED
}

data class ModerationReport(
    val id: String,
    val reporterUid: String,
    val conversationId: String,
    val messageId: String,
    val reason: String,
    val state: ReportState
)

enum class ReportState {
    OPEN,
    IN_REVIEW,
    CLOSED
}

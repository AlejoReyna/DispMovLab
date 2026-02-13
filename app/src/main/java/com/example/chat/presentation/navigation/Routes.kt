package com.example.chat.presentation.navigation

object Routes {
    const val Auth = "auth"
    const val Conversations = "conversations"
    const val ChatRoom = "chat_room"
    const val Admin = "admin"

    fun chatRoom(conversationId: String): String = "$ChatRoom/$conversationId"
}

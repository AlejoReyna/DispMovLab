package com.example.chat.presentation.navigation

object Routes {
    const val Auth              = "auth"
    const val EmailVerification = "email_verification"
    const val Conversations     = "conversations"
    const val UserPicker        = "user_picker"
    const val ChatRoom          = "chat_room"
    const val VideoCall         = "video_call"
    const val Admin             = "admin"

    fun chatRoom(conversationId: String)  = "$ChatRoom/$conversationId"
    fun videoCall(conversationId: String) = "$VideoCall/$conversationId"
}

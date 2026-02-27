package com.example.chat.core

/**
 * Legacy Agora SDK manager.
 *
 * This class has been replaced by [WebRTCManager] which uses open WebRTC + Firebase
 * signaling (following the Firebase WebRTC codelab approach) and does not require
 * an external Agora App ID.
 *
 * Kept as an empty stub so that any remaining references compile without error.
 */
@Deprecated("Use WebRTCManager instead")
object AgoraManager

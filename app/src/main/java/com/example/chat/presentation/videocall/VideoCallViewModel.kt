package com.example.chat.presentation.videocall

import android.content.Context
import android.view.SurfaceView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chat.BuildConfig
import com.example.chat.core.AgoraManager
import io.agora.rtc2.IRtcEngineEventHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VideoCallUiState(
    /** Agora App ID is present in BuildConfig */
    val isConfigured: Boolean = BuildConfig.AGORA_APP_ID.isNotBlank(),
    val isConnected: Boolean = false,
    val remoteUserJoined: Boolean = false,
    val remoteUid: Int = 0,
    val isAudioMuted: Boolean = false,
    val isVideoMuted: Boolean = false,
    val error: String? = null
)

class VideoCallViewModel(private val channelId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoCallUiState())
    val uiState: StateFlow<VideoCallUiState> = _uiState.asStateFlow()

    private val eventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            _uiState.update { it.copy(isConnected = true) }
        }
        override fun onUserJoined(uid: Int, elapsed: Int) {
            _uiState.update { it.copy(remoteUserJoined = true, remoteUid = uid) }
        }
        override fun onUserOffline(uid: Int, reason: Int) {
            _uiState.update { it.copy(remoteUserJoined = false, remoteUid = 0) }
        }
        override fun onError(err: Int) {
            _uiState.update { it.copy(error = "Error Agora: $err") }
        }
    }

    fun initAndJoin(context: Context) {
        val appId = BuildConfig.AGORA_APP_ID
        if (appId.isBlank()) {
            _uiState.update { it.copy(error = "AGORA_APP_ID no configurado") }
            return
        }
        AgoraManager.init(context, appId, eventHandler)
        // For testing use null token. For production use a server-generated token.
        AgoraManager.joinChannel(token = null, channelName = channelId)
    }

    fun setupLocalVideo(surfaceView: SurfaceView) = AgoraManager.setupLocalVideo(surfaceView)

    fun setupRemoteVideo(uid: Int, surfaceView: SurfaceView) =
        AgoraManager.setupRemoteVideo(uid, surfaceView)

    fun toggleAudio() {
        val muted = !_uiState.value.isAudioMuted
        AgoraManager.muteLocalAudio(muted)
        _uiState.update { it.copy(isAudioMuted = muted) }
    }

    fun toggleVideo() {
        val muted = !_uiState.value.isVideoMuted
        AgoraManager.muteLocalVideo(muted)
        _uiState.update { it.copy(isVideoMuted = muted) }
    }

    fun leaveCall() {
        AgoraManager.leaveChannel()
        _uiState.update { it.copy(isConnected = false, remoteUserJoined = false) }
    }

    override fun onCleared() {
        super.onCleared()
        AgoraManager.leaveChannel()
    }
}

class VideoCallViewModelFactory(private val channelId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return VideoCallViewModel(channelId) as T
    }
}

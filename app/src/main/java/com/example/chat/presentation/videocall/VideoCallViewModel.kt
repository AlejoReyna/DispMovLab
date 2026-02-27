package com.example.chat.presentation.videocall

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chat.core.AppContainer
import com.example.chat.core.WebRTCManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer

data class VideoCallUiState(
    val isConnected: Boolean = false,
    val remoteUserJoined: Boolean = false,
    val isAudioMuted: Boolean = false,
    val isVideoMuted: Boolean = false,
    val error: String? = null
)

/**
 * Manages a WebRTC 1-to-1 video call using Firestore for signaling.
 *
 * Flow (mirrors the Firebase WebRTC codelab, adapted for Android):
 *  - No offer in Firestore → this device is the **caller**: creates offer, waits for answer.
 *  - Offer already present  → this device is the **callee**: reads offer, creates answer.
 */
class VideoCallViewModel(private val conversationId: String) : ViewModel() {

    private val signalingDataSource = AppContainer.videoCallSignalingDataSource

    private val _uiState = MutableStateFlow(VideoCallUiState())
    val uiState: StateFlow<VideoCallUiState> = _uiState.asStateFlow()

    private var webRTCManager: WebRTCManager? = null
    private var isCaller = false
    private var iceCandidateJob: Job? = null

    // ── Entry point ───────────────────────────────────────────────────────────

    /**
     * Called from the UI once camera/microphone permissions are granted.
     * Initialises WebRTC, starts local video, and begins the signaling handshake.
     */
    fun initAndStart(
        context: Context,
        localRenderer: SurfaceViewRenderer,
        remoteRenderer: SurfaceViewRenderer
    ) {
        val manager = WebRTCManager(context).also { webRTCManager = it }
        manager.initialize()

        manager.onIceCandidateFound = { candidate ->
            viewModelScope.launch {
                runCatching { signalingDataSource.addIceCandidate(conversationId, candidate, isCaller) }
            }
        }
        manager.onRemoteTrackReceived = {
            _uiState.update { it.copy(remoteUserJoined = true) }
        }
        manager.onConnectionStateChanged = { state ->
            _uiState.update {
                it.copy(
                    isConnected = state == PeerConnection.PeerConnectionState.CONNECTED,
                    remoteUserJoined = state == PeerConnection.PeerConnectionState.CONNECTED
                            || _uiState.value.remoteUserJoined
                )
            }
        }

        manager.startLocalVideo(localRenderer)
        manager.createPeerConnection()
        manager.setupRemoteVideo(remoteRenderer)

        viewModelScope.launch {
            val hasOffer = runCatching { signalingDataSource.getOffer(conversationId) }
                .getOrNull() != null
            if (hasOffer) startAsCallee() else startAsCaller()
        }
    }

    // ── Caller ────────────────────────────────────────────────────────────────

    private fun startAsCaller() {
        isCaller = true
        val manager = webRTCManager ?: return

        manager.onOfferCreated = { offer ->
            viewModelScope.launch {
                runCatching { signalingDataSource.storeOffer(conversationId, offer) }
                    .onFailure { e ->
                        _uiState.update { it.copy(error = e.message) }
                        return@launch
                    }

                // Block until the callee stores an answer.
                runCatching { signalingDataSource.awaitAnswer(conversationId) }
                    .onSuccess { answer -> manager.setRemoteDescription(answer) }
                    .onFailure { e -> _uiState.update { it.copy(error = e.message) } }

                listenForRemoteCandidates()
            }
        }
        manager.createOffer()
    }

    // ── Callee ────────────────────────────────────────────────────────────────

    private suspend fun startAsCallee() {
        isCaller = false
        val manager = webRTCManager ?: return

        val offer = runCatching { signalingDataSource.getOffer(conversationId) }.getOrNull()
        if (offer == null) {
            _uiState.update { it.copy(error = "No active call found for this conversation.") }
            return
        }

        manager.setRemoteDescription(offer)

        manager.onAnswerCreated = { answer ->
            viewModelScope.launch {
                runCatching { signalingDataSource.storeAnswer(conversationId, answer) }
                    .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            }
        }
        manager.createAnswer()

        listenForRemoteCandidates()
    }

    // ── ICE candidates ────────────────────────────────────────────────────────

    private fun listenForRemoteCandidates() {
        iceCandidateJob?.cancel()
        iceCandidateJob = viewModelScope.launch {
            signalingDataSource.listenForIceCandidates(conversationId, isCaller)
                .collect { candidate -> webRTCManager?.addIceCandidate(candidate) }
        }
    }

    // ── Controls ──────────────────────────────────────────────────────────────

    fun toggleAudio() {
        val muted = !_uiState.value.isAudioMuted
        webRTCManager?.toggleAudio(muted)
        _uiState.update { it.copy(isAudioMuted = muted) }
    }

    fun toggleVideo() {
        val muted = !_uiState.value.isVideoMuted
        webRTCManager?.toggleVideo(muted)
        _uiState.update { it.copy(isVideoMuted = muted) }
    }

    fun leaveCall() {
        iceCandidateJob?.cancel()
        viewModelScope.launch { runCatching { signalingDataSource.endCall(conversationId) } }
        webRTCManager?.close()
        webRTCManager = null
        _uiState.update { it.copy(isConnected = false, remoteUserJoined = false) }
    }

    override fun onCleared() {
        super.onCleared()
        webRTCManager?.close()
    }
}

class VideoCallViewModelFactory(private val conversationId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return VideoCallViewModel(conversationId) as T
    }
}

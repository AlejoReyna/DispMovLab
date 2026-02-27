package com.example.chat.core

import android.content.Context
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/**
 * Manages a single WebRTC PeerConnection for a 1-to-1 video call.
 *
 * Lifecycle:
 *  1. [initialize] — initialises PeerConnectionFactory (once per instance).
 *  2. [startLocalVideo] — opens the camera and renders the local preview.
 *  3. [createPeerConnection] — builds the PeerConnection with STUN servers.
 *  4. [setupRemoteVideo] — attaches a renderer for the incoming remote stream.
 *  5. [createOffer] or [createAnswer] + [setRemoteDescription] — SDP exchange.
 *  6. [addIceCandidate] — called as ICE candidates arrive from signaling.
 *  7. [close] — releases all resources.
 */
class WebRTCManager(private val context: Context) {

    val eglBase: EglBase = EglBase.create()

    private lateinit var factory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null

    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null

    private var remoteVideoTrack: VideoTrack? = null
    private var pendingRemoteRenderer: SurfaceViewRenderer? = null

    // ── Callbacks ─────────────────────────────────────────────────────────────
    var onIceCandidateFound: ((IceCandidate) -> Unit)? = null
    var onRemoteTrackReceived: ((VideoTrack) -> Unit)? = null
    var onConnectionStateChanged: ((PeerConnection.PeerConnectionState) -> Unit)? = null
    var onOfferCreated: ((SessionDescription) -> Unit)? = null
    var onAnswerCreated: ((SessionDescription) -> Unit)? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer()
    )

    // ── Initialisation ────────────────────────────────────────────────────────

    fun initialize() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(context.applicationContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
            )
            .createPeerConnectionFactory()
    }

    // ── Local video ───────────────────────────────────────────────────────────

    fun startLocalVideo(localRenderer: SurfaceViewRenderer) {
        localRenderer.init(eglBase.eglBaseContext, null)
        localRenderer.setMirror(true)

        val videoSource = factory.createVideoSource(/* isScreencast= */ false)
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)

        videoCapturer = createCameraCapturer()
        videoCapturer?.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)

        localVideoTrack = factory.createVideoTrack("local_video_track", videoSource)
        localVideoTrack?.addSink(localRenderer)

        val audioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("local_audio_track", audioSource)
    }

    // ── PeerConnection ────────────────────────────────────────────────────────

    fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peerConnection = factory.createPeerConnection(rtcConfig, pcObserver)

        // Add local media tracks so they are included in the SDP offer/answer.
        localAudioTrack?.let { peerConnection?.addTrack(it, listOf("local_stream")) }
        localVideoTrack?.let { peerConnection?.addTrack(it, listOf("local_stream")) }
    }

    // ── Remote video ──────────────────────────────────────────────────────────

    fun setupRemoteVideo(renderer: SurfaceViewRenderer) {
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setMirror(false)
        if (remoteVideoTrack != null) {
            remoteVideoTrack?.addSink(renderer)
        } else {
            // Track hasn't arrived yet — store renderer and attach when it does.
            pendingRemoteRenderer = renderer
        }
    }

    // ── SDP offer / answer ────────────────────────────────────────────────────

    fun createOffer() {
        peerConnection?.createOffer(
            object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription) {
                    // Set local description, then fire the callback.
                    peerConnection?.setLocalDescription(simpleSdpObserver(onSet = {
                        onOfferCreated?.invoke(sdp)
                    }), sdp)
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String) {}
                override fun onSetFailure(error: String) {}
            },
            MediaConstraints()
        )
    }

    fun createAnswer() {
        peerConnection?.createAnswer(
            object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription) {
                    peerConnection?.setLocalDescription(simpleSdpObserver(onSet = {
                        onAnswerCreated?.invoke(sdp)
                    }), sdp)
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String) {}
                override fun onSetFailure(error: String) {}
            },
            MediaConstraints()
        )
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(simpleSdpObserver(), sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    // ── Audio / video mute ────────────────────────────────────────────────────

    fun toggleAudio(mute: Boolean) {
        localAudioTrack?.setEnabled(!mute)
    }

    fun toggleVideo(mute: Boolean) {
        localVideoTrack?.setEnabled(!mute)
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    fun close() {
        try {
            videoCapturer?.stopCapture()
        } catch (_: InterruptedException) { }
        videoCapturer?.dispose()
        surfaceTextureHelper?.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        peerConnection?.close()
        peerConnection = null
        if (::factory.isInitialized) factory.dispose()
        eglBase.release()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun createCameraCapturer(): CameraVideoCapturer {
        val enumerator = Camera2Enumerator(context)
        val names = enumerator.deviceNames
        // Prefer front-facing camera for a typical video-call UX.
        for (name in names) {
            if (enumerator.isFrontFacing(name)) return enumerator.createCapturer(name, null)
        }
        return enumerator.createCapturer(names.first(), null)
    }

    private fun simpleSdpObserver(
        onCreate: (SessionDescription) -> Unit = {},
        onSet: () -> Unit = {}
    ) = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) = onCreate(sdp)
        override fun onSetSuccess() = onSet()
        override fun onCreateFailure(error: String) {}
        override fun onSetFailure(error: String) {}
    }

    // ── PeerConnection.Observer ───────────────────────────────────────────────

    private val pcObserver = object : PeerConnection.Observer {

        override fun onIceCandidate(candidate: IceCandidate) {
            onIceCandidateFound?.invoke(candidate)
        }

        override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {
            val track = receiver.track()
            if (track is VideoTrack) {
                remoteVideoTrack = track
                pendingRemoteRenderer?.let {
                    track.addSink(it)
                    pendingRemoteRenderer = null
                }
                onRemoteTrackReceived?.invoke(track)
            }
        }

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
            onConnectionStateChanged?.invoke(newState)
        }

        // Required overrides — intentionally empty.
        override fun onSignalingChange(newState: PeerConnection.SignalingState) {}
        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {}
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {}
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
        override fun onAddStream(stream: MediaStream) {}
        override fun onRemoveStream(stream: MediaStream) {}
        override fun onDataChannel(dc: DataChannel) {}
        override fun onRenegotiationNeeded() {}
    }
}

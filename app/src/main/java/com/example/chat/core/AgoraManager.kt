package com.example.chat.core

import android.content.Context
import android.view.SurfaceView
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

/**
 * Singleton wrapper for the Agora RTC Engine.
 *
 * Initialise once per process via [init], then call [joinChannel] / [leaveChannel] per call.
 * The engine is kept alive between calls for faster reconnection.
 * Call [destroy] only when the app is terminating.
 */
object AgoraManager {

    private var engine: RtcEngine? = null

    /**
     * Initialise the engine.  Safe to call multiple times (idempotent).
     * @param appId  Your Agora App ID (from BuildConfig.AGORA_APP_ID).
     * @param eventHandler  Callbacks for join, leave, remote-user events, etc.
     */
    fun init(context: Context, appId: String, eventHandler: IRtcEngineEventHandler) {
        if (engine != null) return          // already initialised
        if (appId.isBlank()) return         // App ID not configured
        val config = RtcEngineConfig().apply {
            mContext = context.applicationContext
            mAppId = appId
            mEventHandler = eventHandler
        }
        engine = RtcEngine.create(config)
        engine?.enableVideo()
    }

    fun joinChannel(token: String?, channelName: String, uid: Int = 0) {
        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        }
        engine?.joinChannel(token, channelName, uid, options)
    }

    fun leaveChannel() {
        engine?.leaveChannel()
    }

    fun setupLocalVideo(surfaceView: SurfaceView) {
        val canvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        engine?.setupLocalVideo(canvas)
        engine?.startPreview()
    }

    fun setupRemoteVideo(uid: Int, surfaceView: SurfaceView) {
        val canvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        engine?.setupRemoteVideo(canvas)
    }

    fun muteLocalAudio(mute: Boolean) {
        engine?.muteLocalAudioStream(mute)
    }

    fun muteLocalVideo(mute: Boolean) {
        engine?.muteLocalVideoStream(mute)
        if (mute) engine?.stopPreview() else engine?.startPreview()
    }

    fun destroy() {
        engine?.leaveChannel()
        RtcEngine.destroy()
        engine = null
    }

    val isInitialised: Boolean get() = engine != null
}

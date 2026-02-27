package com.example.chat.presentation.videocall

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.chat.R
import org.webrtc.SurfaceViewRenderer

private val videoCallPermissions = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO
)

/**
 * Full-screen video call UI using WebRTC's [SurfaceViewRenderer].
 *
 * The composable owns the two renderer instances and passes them to the ViewModel
 * via [onInitAndStart] once permissions are granted.  Renderers are released inside
 * [DisposableEffect] when the composable leaves composition.
 */
@Composable
fun VideoCallScreen(
    uiState: VideoCallUiState,
    onInitAndStart: (localRenderer: SurfaceViewRenderer, remoteRenderer: SurfaceViewRenderer) -> Unit,
    onToggleAudio: () -> Unit,
    onToggleVideo: () -> Unit,
    onLeaveCall: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Create renderers once; release them on disposal.
    val localRenderer = remember { SurfaceViewRenderer(context) }
    val remoteRenderer = remember { SurfaceViewRenderer(context) }

    val permissionsGranted = remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted.value = results.values.all { it }
    }

    // Request permissions on first composition.
    LaunchedEffect(Unit) {
        permissionLauncher.launch(videoCallPermissions)
    }

    // Once permissions are granted, initialise WebRTC and start the call.
    LaunchedEffect(permissionsGranted.value) {
        if (permissionsGranted.value) {
            onInitAndStart(localRenderer, remoteRenderer)
        }
    }

    // Release renderers BEFORE closing the WebRTC manager (which destroys the EGL context).
    DisposableEffect(Unit) {
        onDispose {
            runCatching { localRenderer.clearImage(); localRenderer.release() }
            runCatching { remoteRenderer.clearImage(); remoteRenderer.release() }
            onLeaveCall()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // ── Remote video (full screen) ────────────────────────────────────────
        if (uiState.remoteUserJoined) {
            AndroidView(
                factory = { remoteRenderer },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = if (uiState.isConnected)
                    stringResource(R.string.video_call_waiting)
                else
                    stringResource(R.string.video_call_connecting),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ── Local video (picture-in-picture, top-right) ───────────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(width = 100.dp, height = 140.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.DarkGray
        ) {
            AndroidView(
                factory = { localRenderer },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Error banner ──────────────────────────────────────────────────────
        uiState.error?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }

        // ── Controls (bottom) ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Microphone toggle
            IconButton(
                onClick = onToggleAudio,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isAudioMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = stringResource(
                        if (uiState.isAudioMuted) R.string.btn_unmute_audio else R.string.btn_mute_audio
                    ),
                    tint = Color.White
                )
            }

            // End call
            FloatingActionButton(
                onClick = { onLeaveCall(); onNavigateBack() },
                containerColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(68.dp)
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = stringResource(R.string.btn_end_call),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Camera toggle
            IconButton(
                onClick = onToggleVideo,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isVideoMuted) Icons.Default.VideocamOff else Icons.Default.Videocam,
                    contentDescription = stringResource(
                        if (uiState.isVideoMuted) R.string.btn_unmute_video else R.string.btn_mute_video
                    ),
                    tint = Color.White
                )
            }
        }
    }
}

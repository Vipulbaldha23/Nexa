package com.example.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.ui.PlayerView
import com.example.player.GestureIndicator
import com.example.player.MediaTrackInfo
import com.example.player.NexaPlayerManager
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassIconButton
import com.example.ui.components.VideoDetailsDialog
import com.example.ui.components.liquidGlass
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderViolet
import com.example.ui.theme.NexaCyan
import com.example.ui.theme.NexaViolet
import com.example.ui.theme.PlayerProgressBarActive
import com.example.ui.theme.PlayerProgressBarBuffered
import com.example.ui.theme.PlayerProgressBarTrack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun VideoPlayerScreen(
    playerManager: NexaPlayerManager,
    onBack: () -> Unit,
    onEnterPiP: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val uiState by playerManager.uiState.collectAsState()

    var showDetailsDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    var isSeeking by remember { mutableStateOf(false) }
    var seekSliderPosition by remember { mutableFloatStateOf(0f) }

    // Keep screen ON during playback
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // Restore window brightness
            val lp = activity?.window?.attributes
            lp?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            activity?.window?.attributes = lp
            // Restore portrait orientation if modified
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Handle back button: if locked, unlock first; else onBack()
    BackHandler {
        if (uiState.isLocked) {
            playerManager.setLocked(false)
        } else {
            playerManager.saveCurrentPosition()
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ExoPlayer View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    player = playerManager.player
                    resizeMode = playerManager.getExoResizeMode(uiState.resizeMode)
                }
            },
            update = { playerView ->
                playerView.player = playerManager.player
                playerView.resizeMode = playerManager.getExoResizeMode(uiState.resizeMode)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture Detection Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.isLocked) {
                    if (uiState.isLocked) return@pointerInput
                    detectTapGestures(
                        onTap = {
                            playerManager.toggleControls()
                        },
                        onDoubleTap = { offset ->
                            val width = size.width
                            when {
                                offset.x < width * 0.38f -> playerManager.seekBackward(10)
                                offset.x > width * 0.62f -> playerManager.seekForward(10)
                                else -> playerManager.togglePlayPause()
                            }
                        }
                    )
                }
                .pointerInput(uiState.isLocked) {
                    if (uiState.isLocked) return@pointerInput
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val width = size.width
                        val isLeft = change.position.x < width * 0.5f
                        val deltaPercent = -dragAmount.y / 800f

                        if (isLeft) {
                            playerManager.adjustBrightnessDelta(deltaPercent)
                            // Apply to window
                            val bright = uiState.brightness
                            val lp = activity?.window?.attributes
                            lp?.screenBrightness = bright
                            activity?.window?.attributes = lp
                        } else {
                            playerManager.adjustVolumeDelta(deltaPercent)
                        }
                    }
                }
        )

        // Buffering Indicator
        if (uiState.isBuffering) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .liquidGlass(
                        shape = CircleShape,
                        backgroundColor = Color(0x66000000),
                        hasGlow = true,
                        glowColor = NexaCyan
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = NexaCyan,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        // Gesture Feedback HUD
        uiState.gestureIndicator?.let { indicator ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .liquidGlass(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = Color(0xCC0B1220),
                        borderColor = GlassBorderCyan,
                        hasGlow = true,
                        glowColor = NexaCyan
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (indicator) {
                    is GestureIndicator.Volume -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = NexaCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Volume ${indicator.percent}%",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    is GestureIndicator.Brightness -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BrightnessMedium,
                                contentDescription = null,
                                tint = NexaCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Brightness ${indicator.percent}%",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    is GestureIndicator.Seek -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (indicator.isForward) Icons.Default.Forward10 else Icons.Default.Replay10,
                                contentDescription = null,
                                tint = NexaCyan,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (indicator.isForward) "+${indicator.deltaSeconds}s" else "-${indicator.deltaSeconds}s",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Lock button HUD when locked
        if (uiState.isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                LiquidGlassIconButton(
                    icon = Icons.Default.Lock,
                    onClick = { playerManager.setLocked(false) },
                    tint = NexaViolet,
                    hasGlow = true,
                    glowColor = NexaViolet,
                    size = 52.dp,
                    contentDescription = "Unlock Controls"
                )
            }
        }

        // Full Controls Overlay (Smooth animated visibility)
        AnimatedVisibility(
            visible = uiState.isControlsVisible && !uiState.isLocked,
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xD9060A12),
                                Color(0x33000000),
                                Color(0x33000000),
                                Color(0xE6060A12)
                            )
                        )
                    )
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = {
                            playerManager.saveCurrentPosition()
                            onBack()
                        },
                        contentDescription = "Back"
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.currentVideo?.title ?: "Playing Video",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = uiState.currentVideo?.folderName ?: "Nexa Player",
                            style = MaterialTheme.typography.bodySmall,
                            color = NexaCyan.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Aspect Ratio Button
                    LiquidGlassIconButton(
                        icon = Icons.Default.AspectRatio,
                        onClick = { playerManager.cycleResizeMode() },
                        contentDescription = "Resize mode (${uiState.resizeMode.displayName})",
                        size = 40.dp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Audio Track Button
                    LiquidGlassIconButton(
                        icon = Icons.Default.VolumeUp,
                        onClick = { showAudioDialog = true },
                        contentDescription = "Audio Tracks",
                        size = 40.dp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Subtitle Button
                    LiquidGlassIconButton(
                        icon = if (uiState.isSubtitlesEnabled) Icons.Default.ClosedCaption else Icons.Outlined.ClosedCaption,
                        onClick = { showSubtitleDialog = true },
                        tint = if (uiState.isSubtitlesEnabled) NexaCyan else TextSecondary,
                        contentDescription = "Subtitles",
                        size = 40.dp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // PiP Button
                    LiquidGlassIconButton(
                        icon = Icons.Default.PictureInPictureAlt,
                        onClick = onEnterPiP,
                        contentDescription = "Picture in Picture",
                        size = 40.dp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Video Info Button
                    LiquidGlassIconButton(
                        icon = Icons.Default.Info,
                        onClick = { showDetailsDialog = true },
                        contentDescription = "Details",
                        size = 40.dp
                    )
                }

                // Center Playback Controls
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // Previous Track
                    LiquidGlassIconButton(
                        icon = Icons.Default.SkipPrevious,
                        onClick = { playerManager.playPrevious() },
                        tint = if (uiState.hasPrevious) TextPrimary else Color(0x40FFFFFF),
                        size = 48.dp,
                        contentDescription = "Previous Video"
                    )

                    // Seek -10s
                    LiquidGlassIconButton(
                        icon = Icons.Default.Replay10,
                        onClick = { playerManager.seekBackward(10) },
                        size = 54.dp,
                        contentDescription = "Seek Backward 10s"
                    )

                    // Large Glass Play/Pause Button
                    LiquidGlassIconButton(
                        icon = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        onClick = { playerManager.togglePlayPause() },
                        tint = NexaCyan,
                        hasGlow = true,
                        glowColor = NexaCyan,
                        size = 76.dp,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play"
                    )

                    // Seek +10s
                    LiquidGlassIconButton(
                        icon = Icons.Default.Forward10,
                        onClick = { playerManager.seekForward(10) },
                        size = 54.dp,
                        contentDescription = "Seek Forward 10s"
                    )

                    // Next Track
                    LiquidGlassIconButton(
                        icon = Icons.Default.SkipNext,
                        onClick = { playerManager.playNext() },
                        tint = if (uiState.hasNext) TextPrimary else Color(0x40FFFFFF),
                        size = 48.dp,
                        contentDescription = "Next Video"
                    )
                }

                // Bottom Timeline & Utility Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Timeline Slider & Buffered track
                    val duration = uiState.durationMs.toFloat().coerceAtLeast(1f)
                    val currentPos = if (isSeeking) seekSliderPosition else uiState.currentPositionMs.toFloat()
                    val bufferedPos = uiState.bufferedPositionMs.toFloat()

                    // Time Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPos.toLong()),
                            color = NexaCyan,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatTime(uiState.durationMs),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    // Interactive Slider
                    Slider(
                        value = currentPos.coerceIn(0f, duration),
                        onValueChange = { newPos ->
                            isSeeking = true
                            seekSliderPosition = newPos
                        },
                        onValueChangeFinished = {
                            playerManager.seekTo(seekSliderPosition.toLong())
                            isSeeking = false
                        },
                        valueRange = 0f..duration,
                        colors = SliderDefaults.colors(
                            thumbColor = NexaCyan,
                            activeTrackColor = NexaCyan,
                            inactiveTrackColor = PlayerProgressBarTrack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Secondary bottom actions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Lock Controls button
                        LiquidGlassIconButton(
                            icon = Icons.Default.LockOpen,
                            onClick = { playerManager.setLocked(true) },
                            tint = TextSecondary,
                            size = 38.dp,
                            contentDescription = "Lock Screen"
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Speed Pill
                            LiquidGlassButton(
                                text = "${uiState.playbackSpeed}x",
                                icon = Icons.Default.Speed,
                                onClick = { showSpeedDialog = true },
                                accentColor = NexaCyan
                            )

                            // Screen Orientation Toggle
                            LiquidGlassIconButton(
                                icon = Icons.Default.ScreenRotation,
                                onClick = {
                                    val currentOrient = activity?.requestedOrientation
                                    activity?.requestedOrientation = if (currentOrient == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                                },
                                size = 38.dp,
                                contentDescription = "Toggle Orientation"
                            )
                        }
                    }
                }
            }
        }
    }

    // Video Details Dialog
    if (showDetailsDialog && uiState.currentVideo != null) {
        VideoDetailsDialog(
            video = uiState.currentVideo!!,
            onDismiss = { showDetailsDialog = false }
        )
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        SpeedSelectionDialog(
            currentSpeed = uiState.playbackSpeed,
            onSelectSpeed = { speed ->
                playerManager.setPlaybackSpeed(speed)
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }

    // Audio Track Selection Dialog
    if (showAudioDialog) {
        TrackSelectionDialog(
            title = "Select Audio Track",
            tracks = uiState.audioTracks,
            onSelectTrack = { track ->
                playerManager.selectAudioTrack(track)
                showAudioDialog = false
            },
            onDismiss = { showAudioDialog = false }
        )
    }

    // Subtitle Track Selection Dialog
    if (showSubtitleDialog) {
        TrackSelectionDialog(
            title = "Subtitles",
            tracks = uiState.subtitleTracks,
            allowDisable = true,
            onSelectTrack = { track ->
                playerManager.selectSubtitleTrack(track)
                showSubtitleDialog = false
            },
            onDisable = {
                playerManager.selectSubtitleTrack(null)
                showSubtitleDialog = false
            },
            onDismiss = { showSubtitleDialog = false }
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@Composable
fun SpeedSelectionDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(300.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = Color(0xF20B1322),
                    borderColor = GlassBorderCyan
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Playback Speed",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                speeds.chunked(4).forEach { rowSpeeds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSpeeds.forEach { speed ->
                            val isSelected = speed == currentSpeed
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .liquidGlass(
                                        shape = RoundedCornerShape(10.dp),
                                        backgroundColor = if (isSelected) NexaCyan.copy(alpha = 0.25f) else Color(0x22131F33),
                                        borderColor = if (isSelected) NexaCyan else GlassBorderCyan.copy(alpha = 0.3f),
                                        hasGlow = isSelected,
                                        glowColor = NexaCyan
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                LiquidGlassButton(
                                    text = "${speed}x",
                                    onClick = { onSelectSpeed(speed) },
                                    modifier = Modifier.fillMaxSize(),
                                    accentColor = if (isSelected) NexaCyan else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackSelectionDialog(
    title: String,
    tracks: List<MediaTrackInfo>,
    allowDisable: Boolean = false,
    onSelectTrack: (MediaTrackInfo) -> Unit,
    onDisable: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = Color(0xF20B1322),
                    borderColor = GlassBorderCyan
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (allowDisable && onDisable != null) {
                    LiquidGlassCard(
                        shape = RoundedCornerShape(12.dp),
                        onClick = onDisable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Off / None",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                if (tracks.isEmpty()) {
                    Text(
                        text = "No additional tracks available.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    tracks.forEach { track ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(12.dp),
                            onClick = { onSelectTrack(track) },
                            hasGlow = track.isSelected,
                            glowColor = NexaCyan,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.label,
                                    color = if (track.isSelected) NexaCyan else TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (track.isSelected) {
                                    Text(
                                        text = "Active",
                                        color = NexaCyan,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                LiquidGlassButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = NexaCyan
                )
            }
        }
    }
}

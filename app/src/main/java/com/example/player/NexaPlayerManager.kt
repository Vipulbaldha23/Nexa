package com.example.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.model.PlayerSettings
import com.example.data.model.VideoItem
import com.example.data.model.VideoResizeMode
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val currentVideo: VideoItem? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val resizeMode: VideoResizeMode = VideoResizeMode.FIT,
    val isControlsVisible: Boolean = true,
    val isLocked: Boolean = false,
    val audioTracks: List<MediaTrackInfo> = emptyList(),
    val subtitleTracks: List<MediaTrackInfo> = emptyList(),
    val isSubtitlesEnabled: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val volume: Float = 1.0f,
    val brightness: Float = 0.5f,
    val gestureIndicator: GestureIndicator? = null,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null
)

sealed class GestureIndicator {
    data class Volume(val percent: Int) : GestureIndicator()
    data class Brightness(val percent: Int) : GestureIndicator()
    data class Seek(val deltaSeconds: Int, val isForward: Boolean) : GestureIndicator()
}

class NexaPlayerManager(
    private val context: Context,
    private val repository: VideoRepository
) {
    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setSeekForwardIncrementMs(10000L)
        .setSeekBackIncrementMs(10000L)
        .build()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var autoHideJob: Job? = null
    private var gestureDismissJob: Job? = null

    private var currentPlaylist: List<VideoItem> = emptyList()
    private var currentIndex: Int = -1

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                _uiState.update {
                    it.copy(
                        isBuffering = isBuffering,
                        durationMs = player.duration.coerceAtLeast(0L),
                        bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
                    )
                }
                if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEnded()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                    resetAutoHideTimer()
                } else {
                    stopProgressTracker()
                    saveCurrentPosition()
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                extractTracks(tracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("NexaPlayerManager", "Playback error", error)
                _uiState.update { it.copy(errorMessage = error.localizedMessage ?: "Playback Error") }
            }
        })
    }

    fun playVideo(video: VideoItem, playlist: List<VideoItem> = listOf(video), startPositionMs: Long? = null) {
        currentPlaylist = playlist
        currentIndex = playlist.indexOfFirst { it.uriString == video.uriString }.coerceAtLeast(0)

        _uiState.update {
            it.copy(
                currentVideo = video,
                hasNext = currentIndex < currentPlaylist.size - 1,
                hasPrevious = currentIndex > 0,
                errorMessage = null,
                isControlsVisible = true
            )
        }

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(video.uriString))
            .setMediaId(video.uriString)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()

        val resumePos = startPositionMs ?: video.lastPositionMs
        if (resumePos > 1000L && (video.durationMs <= 0 || resumePos < video.durationMs - 3000L)) {
            player.seekTo(resumePos)
        }

        player.playWhenReady = true
        resetAutoHideTimer()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        resetAutoHideTimer()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L)))
        _uiState.update { it.copy(currentPositionMs = positionMs) }
        resetAutoHideTimer()
    }

    fun seekForward(seconds: Int = 10) {
        val target = (player.currentPosition + seconds * 1000L).coerceAtMost(player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        showGestureIndicator(GestureIndicator.Seek(seconds, isForward = true))
        resetAutoHideTimer()
    }

    fun seekBackward(seconds: Int = 10) {
        val target = (player.currentPosition - seconds * 1000L).coerceAtLeast(0L)
        player.seekTo(target)
        showGestureIndicator(GestureIndicator.Seek(seconds, isForward = false))
        resetAutoHideTimer()
    }

    fun setPlaybackSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
        resetAutoHideTimer()
    }

    fun cycleResizeMode() {
        val nextMode = when (_uiState.value.resizeMode) {
            VideoResizeMode.FIT -> VideoResizeMode.ZOOM
            VideoResizeMode.ZOOM -> VideoResizeMode.STRETCH
            VideoResizeMode.STRETCH -> VideoResizeMode.FIXED_16_9
            VideoResizeMode.FIXED_16_9 -> VideoResizeMode.FIT
        }
        _uiState.update { it.copy(resizeMode = nextMode) }
        resetAutoHideTimer()
    }

    fun getExoResizeMode(mode: VideoResizeMode): Int {
        return when (mode) {
            VideoResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            VideoResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            VideoResizeMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            VideoResizeMode.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    fun toggleControls() {
        if (_uiState.value.isLocked) return
        val newVisible = !_uiState.value.isControlsVisible
        _uiState.update { it.copy(isControlsVisible = newVisible) }
        if (newVisible) {
            resetAutoHideTimer()
        } else {
            autoHideJob?.cancel()
        }
    }

    fun showControls() {
        if (_uiState.value.isLocked) return
        _uiState.update { it.copy(isControlsVisible = true) }
        resetAutoHideTimer()
    }

    fun setLocked(locked: Boolean) {
        _uiState.update { it.copy(isLocked = locked, isControlsVisible = !locked) }
    }

    fun resetAutoHideTimer() {
        autoHideJob?.cancel()
        if (_uiState.value.isLocked) return
        autoHideJob = scope.launch {
            delay(4500)
            if (player.isPlaying && !_uiState.value.isLocked) {
                _uiState.update { it.copy(isControlsVisible = false) }
            }
        }
    }

    fun adjustVolumeDelta(deltaPercent: Float) {
        val currentVol = _uiState.value.volume
        val newVol = (currentVol + deltaPercent).coerceIn(0f, 1f)
        player.volume = newVol
        _uiState.update { it.copy(volume = newVol) }
        showGestureIndicator(GestureIndicator.Volume((newVol * 100).toInt()))
    }

    fun adjustBrightnessDelta(deltaPercent: Float) {
        val current = _uiState.value.brightness
        val newBright = (current + deltaPercent).coerceIn(0.05f, 1f)
        _uiState.update { it.copy(brightness = newBright) }
        showGestureIndicator(GestureIndicator.Brightness((newBright * 100).toInt()))
    }

    private fun showGestureIndicator(indicator: GestureIndicator) {
        _uiState.update { it.copy(gestureIndicator = indicator) }
        gestureDismissJob?.cancel()
        gestureDismissJob = scope.launch {
            delay(1200)
            _uiState.update { it.copy(gestureIndicator = null) }
        }
    }

    fun playNext() {
        if (currentIndex < currentPlaylist.size - 1) {
            saveCurrentPosition()
            val nextVideo = currentPlaylist[currentIndex + 1]
            playVideo(nextVideo, currentPlaylist, startPositionMs = 0L)
        }
    }

    fun playPrevious() {
        if (currentIndex > 0) {
            saveCurrentPosition()
            val prevVideo = currentPlaylist[currentIndex - 1]
            playVideo(prevVideo, currentPlaylist, startPositionMs = 0L)
        }
    }

    private fun onPlaybackEnded() {
        saveCurrentPosition()
        if (currentIndex < currentPlaylist.size - 1) {
            playNext()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val current = player.currentPosition.coerceAtLeast(0L)
                val duration = player.duration.coerceAtLeast(0L)
                val buffered = player.bufferedPosition.coerceAtLeast(0L)
                _uiState.update {
                    it.copy(
                        currentPositionMs = current,
                        durationMs = duration,
                        bufferedPositionMs = buffered
                    )
                }
                delay(300)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun saveCurrentPosition() {
        val video = _uiState.value.currentVideo ?: return
        val pos = player.currentPosition
        val duration = player.duration
        if (pos > 0 && duration > 0) {
            scope.launch(Dispatchers.IO) {
                repository.savePlaybackPosition(video.uriString, pos, duration)
            }
        }
    }

    private fun extractTracks(tracks: Tracks) {
        val audioList = mutableListOf<MediaTrackInfo>()
        val subList = mutableListOf<MediaTrackInfo>()

        for (groupIndex in 0 until tracks.groups.size) {
            val group = tracks.groups[groupIndex]
            val trackType = group.type

            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val isSelected = group.isTrackSelected(trackIndex)
                val label = format.label ?: format.language ?: "Track ${trackIndex + 1}"

                val info = MediaTrackInfo(
                    groupIndex = groupIndex,
                    trackIndex = trackIndex,
                    label = label,
                    language = format.language,
                    isSelected = isSelected
                )

                if (trackType == C.TRACK_TYPE_AUDIO) {
                    audioList.add(info)
                } else if (trackType == C.TRACK_TYPE_TEXT) {
                    subList.add(info)
                }
            }
        }

        _uiState.update {
            it.copy(
                audioTracks = audioList,
                subtitleTracks = subList,
                isSubtitlesEnabled = subList.any { s -> s.isSelected }
            )
        }
    }

    fun selectAudioTrack(track: MediaTrackInfo) {
        val group = player.currentTracks.groups.getOrNull(track.groupIndex) ?: return
        val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(track.trackIndex))
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(override)
            .build()
        resetAutoHideTimer()
    }

    fun selectSubtitleTrack(track: MediaTrackInfo?) {
        if (track == null) {
            // Disable subtitles
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            _uiState.update { it.copy(isSubtitlesEnabled = false) }
        } else {
            val group = player.currentTracks.groups.getOrNull(track.groupIndex) ?: return
            val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(track.trackIndex))
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(override)
                .build()
            _uiState.update { it.copy(isSubtitlesEnabled = true) }
        }
        resetAutoHideTimer()
    }

    fun release() {
        saveCurrentPosition()
        stopProgressTracker()
        autoHideJob?.cancel()
        gestureDismissJob?.cancel()
        player.release()
    }
}

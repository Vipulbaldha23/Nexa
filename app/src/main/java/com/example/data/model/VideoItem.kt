package com.example.data.model

import java.util.Locale

data class VideoItem(
    val id: Long,
    val uriString: String,
    val title: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateAddedSec: Long,
    val mimeType: String,
    val folderName: String,
    val path: String,
    val lastPositionMs: Long = 0L,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val isSample: Boolean = false
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "Unknown"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                else -> String.format(Locale.US, "%.0f KB", kb)
            }
        }

    val resolutionLabel: String
        get() {
            val maxDim = maxOf(width, height)
            return when {
                maxDim >= 3840 -> "4K UHD"
                maxDim >= 2560 -> "2K QHD"
                maxDim >= 1920 -> "1080p"
                maxDim >= 1280 -> "720p"
                maxDim > 0 -> "${width}x${height}"
                else -> "HD"
            }
        }

    val progressPercent: Float
        get() {
            if (durationMs <= 0 || lastPositionMs <= 0) return 0f
            return (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
}

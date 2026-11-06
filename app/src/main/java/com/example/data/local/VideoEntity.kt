package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_playback_state")
data class VideoEntity(
    @PrimaryKey
    val uriString: String,
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastPlayedTimestamp: Long = 0L,
    val isFavorite: Boolean = false,
    val playCount: Int = 0
)

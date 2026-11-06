package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM video_playback_state")
    fun getAllPlaybackStates(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM video_playback_state WHERE uriString = :uri LIMIT 1")
    suspend fun getPlaybackState(uri: String): VideoEntity?

    @Query("SELECT * FROM video_playback_state WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM video_playback_state WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 20")
    fun getRecentHistory(): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: VideoEntity)

    @Query("UPDATE video_playback_state SET isFavorite = :isFavorite WHERE uriString = :uri")
    suspend fun updateFavorite(uri: String, isFavorite: Boolean)

    @Query("UPDATE video_playback_state SET lastPositionMs = :position, lastPlayedTimestamp = :timestamp, playCount = playCount + 1 WHERE uriString = :uri")
    suspend fun updatePlaybackPosition(uri: String, position: Long, timestamp: Long)
}

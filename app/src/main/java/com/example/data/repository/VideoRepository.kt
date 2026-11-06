package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.local.NexaDatabase
import com.example.data.local.VideoEntity
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

data class VideoFolder(
    val name: String,
    val path: String,
    val videoCount: Int,
    val coverVideoUri: String
)

class VideoRepository(
    private val context: Context,
    private val database: NexaDatabase = NexaDatabase.getInstance(context)
) {
    private val contentResolver: ContentResolver = context.contentResolver
    private val videoDao = database.videoDao()

    // High quality demo videos to guarantee out-of-the-box working playback in all test environments
    private val sampleVideos = listOf(
        VideoItem(
            id = -101L,
            uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            title = "Big Buck Bunny - 4K Remastered",
            displayName = "BigBuckBunny.mp4",
            durationMs = 596000L,
            sizeBytes = 158000000L,
            width = 3840,
            height = 2160,
            dateAddedSec = System.currentTimeMillis() / 1000 - 3600 * 2,
            mimeType = "video/mp4",
            folderName = "Nexa Showcase",
            path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            isSample = true
        ),
        VideoItem(
            id = -102L,
            uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            title = "Elephants Dream - Sci-Fi Core",
            displayName = "ElephantsDream.mp4",
            durationMs = 653000L,
            sizeBytes = 185000000L,
            width = 1920,
            height = 1080,
            dateAddedSec = System.currentTimeMillis() / 1000 - 3600 * 12,
            mimeType = "video/mp4",
            folderName = "Sci-Fi Cinema",
            path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            isSample = true
        ),
        VideoItem(
            id = -103L,
            uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            title = "Tears of Steel - Cyberpunk VFX",
            displayName = "TearsOfSteel.mp4",
            durationMs = 734000L,
            sizeBytes = 220000000L,
            width = 1920,
            height = 800,
            dateAddedSec = System.currentTimeMillis() / 1000 - 3600 * 24,
            mimeType = "video/mp4",
            folderName = "Sci-Fi Cinema",
            path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            isSample = true
        ),
        VideoItem(
            id = -104L,
            uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            title = "Quantum Spark: High Speed Optics",
            displayName = "ForBiggerBlazes.mp4",
            durationMs = 15000L,
            sizeBytes = 15000000L,
            width = 1280,
            height = 720,
            dateAddedSec = System.currentTimeMillis() / 1000 - 3600 * 48,
            mimeType = "video/mp4",
            folderName = "Nexa Showcase",
            path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            isSample = true
        )
    )

    fun getVideosFlow(): Flow<List<VideoItem>> {
        val mediaStoreFlow = flow {
            emit(queryDeviceVideos())
        }.flowOn(Dispatchers.IO)

        return combine(mediaStoreFlow, videoDao.getAllPlaybackStates()) { deviceVideos, states ->
            val stateMap = states.associateBy { it.uriString }
            val combinedList = (deviceVideos.ifEmpty { sampleVideos }).map { video ->
                val state = stateMap[video.uriString]
                if (state != null) {
                    video.copy(
                        lastPositionMs = state.lastPositionMs,
                        isFavorite = state.isFavorite,
                        playCount = state.playCount
                    )
                } else {
                    video
                }
            }
            combinedList
        }
    }

    private suspend fun queryDeviceVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoItem>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = if (idCol >= 0) cursor.getLong(idCol) else 0L
                    val uri = ContentUris.withAppendedId(collection, id)
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "Video_$id" else "Video_$id"
                    val title = if (titleCol >= 0) cursor.getString(titleCol) ?: name else name
                    val duration = if (durCol >= 0) cursor.getLong(durCol) else 0L
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "video/*" else "video/*"
                    val folder = if (bucketCol >= 0) cursor.getString(bucketCol) ?: "Internal Storage" else "Internal Storage"
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: uri.toString() else uri.toString()

                    videoList.add(
                        VideoItem(
                            id = id,
                            uriString = uri.toString(),
                            title = title,
                            displayName = name,
                            durationMs = duration,
                            sizeBytes = size,
                            width = width,
                            height = height,
                            dateAddedSec = dateAdded,
                            mimeType = mime,
                            folderName = folder,
                            path = path,
                            isSample = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error querying MediaStore", e)
        }

        // If device has real videos, also append samples at end so demo showcase is always discoverable
        if (videoList.isNotEmpty()) {
            videoList.addAll(sampleVideos)
        }

        videoList
    }

    suspend fun toggleFavorite(uriString: String, currentFavorite: Boolean) = withContext(Dispatchers.IO) {
        val existing = videoDao.getPlaybackState(uriString)
        if (existing != null) {
            videoDao.updateFavorite(uriString, !currentFavorite)
        } else {
            videoDao.upsertState(
                VideoEntity(
                    uriString = uriString,
                    isFavorite = !currentFavorite,
                    lastPlayedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun savePlaybackPosition(uriString: String, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        val existing = videoDao.getPlaybackState(uriString)
        if (existing != null) {
            videoDao.updatePlaybackPosition(uriString, positionMs, System.currentTimeMillis())
        } else {
            videoDao.upsertState(
                VideoEntity(
                    uriString = uriString,
                    lastPositionMs = positionMs,
                    durationMs = durationMs,
                    lastPlayedTimestamp = System.currentTimeMillis(),
                    playCount = 1
                )
            )
        }
    }

    suspend fun parseExternalUri(uri: Uri): VideoItem = withContext(Dispatchers.IO) {
        var displayName = "Imported Video"
        var sizeBytes = 0L

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                    if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            Log.w("VideoRepository", "Could not query OpenableColumns for $uri", e)
        }

        var durationMs = 0L
        var width = 0
        var height = 0
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            retriever.release()
        } catch (e: Exception) {
            Log.w("VideoRepository", "Could not extract metadata for $uri", e)
        }

        val state = videoDao.getPlaybackState(uri.toString())

        VideoItem(
            id = System.currentTimeMillis(),
            uriString = uri.toString(),
            title = displayName.substringBeforeLast("."),
            displayName = displayName,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            dateAddedSec = System.currentTimeMillis() / 1000,
            mimeType = contentResolver.getType(uri) ?: "video/*",
            folderName = "Imported",
            path = uri.toString(),
            lastPositionMs = state?.lastPositionMs ?: 0L,
            isFavorite = state?.isFavorite ?: false,
            isSample = false
        )
    }
}

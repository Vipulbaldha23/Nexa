package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NexaAccent
import com.example.data.model.PlayerSettings
import com.example.data.model.VideoItem
import com.example.data.model.VideoSortOrder
import com.example.data.repository.VideoFolder
import com.example.data.repository.VideoRepository
import com.example.player.NexaPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val videos: List<VideoItem> = emptyList(),
    val filteredVideos: List<VideoItem> = emptyList(),
    val continueWatching: List<VideoItem> = emptyList(),
    val favoriteVideos: List<VideoItem> = emptyList(),
    val folders: List<VideoFolder> = emptyList(),
    val selectedFolder: String? = null,
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val sortOrder: VideoSortOrder = VideoSortOrder.DATE_DESC,
    val isGridView: Boolean = true,
    val settings: PlayerSettings = PlayerSettings(),
    val activeVideoForPlayer: VideoItem? = null,
    val selectedVideoForDetails: VideoItem? = null,
    val hasStoragePermission: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VideoRepository(application)
    val playerManager = NexaPlayerManager(application, repository)

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(VideoSortOrder.DATE_DESC)
    private val _isGridView = MutableStateFlow(true)
    private val _selectedFolder = MutableStateFlow<String?>(null)
    private val _selectedCategory = MutableStateFlow("All")
    private val _settings = MutableStateFlow(PlayerSettings())
    private val _activeVideoForPlayer = MutableStateFlow<VideoItem?>(null)
    private val _selectedVideoForDetails = MutableStateFlow<VideoItem?>(null)
    private val _hasStoragePermission = MutableStateFlow(true)

    val uiState: StateFlow<MainUiState> = combine(
        repository.getVideosFlow(),
        _searchQuery,
        _sortOrder,
        _selectedFolder,
        _selectedCategory,
        _isGridView,
        _settings,
        _activeVideoForPlayer,
        _selectedVideoForDetails
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val allVideos = params[0] as List<VideoItem>
        val query = params[1] as String
        val sort = params[2] as VideoSortOrder
        val folder = params[3] as String?
        val category = params[4] as String
        val isGrid = params[5] as Boolean
        val settings = params[6] as PlayerSettings
        val activeVideo = params[7] as VideoItem?
        val detailsVideo = params[8] as VideoItem?

        // Compute Continue Watching (videos that have lastPositionMs > 2s and < 95% progress)
        val continueList = allVideos.filter {
            it.lastPositionMs > 2000L && it.progressPercent < 0.95f
        }.sortedByDescending { it.dateAddedSec }

        // Compute Favorites
        val favorites = allVideos.filter { it.isFavorite }

        // Compute Folders
        val folderMap = allVideos.groupBy { it.folderName }
        val folders = folderMap.map { (name, vList) ->
            VideoFolder(
                name = name,
                path = vList.firstOrNull()?.path ?: "",
                videoCount = vList.size,
                coverVideoUri = vList.firstOrNull()?.uriString ?: ""
            )
        }.sortedBy { it.name }

        // Filter by folder, category, and search query
        var filtered = allVideos

        if (folder != null) {
            filtered = filtered.filter { it.folderName == folder }
        }

        if (category == "4K UHD") {
            filtered = filtered.filter { it.resolutionLabel.contains("4K") }
        } else if (category == "Short Clips") {
            filtered = filtered.filter { it.durationMs in 1..60000L }
        } else if (category == "Sci-Fi Cinema") {
            filtered = filtered.filter { it.folderName.contains("Sci-Fi") || it.folderName.contains("Showcase") }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(q) ||
                it.displayName.lowercase().contains(q) ||
                it.folderName.lowercase().contains(q)
            }
        }

        // Apply Sorting
        filtered = when (sort) {
            VideoSortOrder.DATE_DESC -> filtered.sortedByDescending { it.dateAddedSec }
            VideoSortOrder.DATE_ASC -> filtered.sortedBy { it.dateAddedSec }
            VideoSortOrder.NAME_ASC -> filtered.sortedBy { it.title.lowercase() }
            VideoSortOrder.NAME_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            VideoSortOrder.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
            VideoSortOrder.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
        }

        MainUiState(
            videos = allVideos,
            filteredVideos = filtered,
            continueWatching = continueList,
            favoriteVideos = favorites,
            folders = folders,
            selectedFolder = folder,
            selectedCategory = category,
            searchQuery = query,
            sortOrder = sort,
            isGridView = isGrid,
            settings = settings,
            activeVideoForPlayer = activeVideo,
            selectedVideoForDetails = detailsVideo,
            hasStoragePermission = _hasStoragePermission.value
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MainUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: VideoSortOrder) {
        _sortOrder.value = order
    }

    fun toggleGridView() {
        _isGridView.update { !it }
    }

    fun selectFolder(folderName: String?) {
        _selectedFolder.value = folderName
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun playVideo(video: VideoItem, playlist: List<VideoItem> = uiState.value.filteredVideos) {
        _activeVideoForPlayer.value = video
        playerManager.playVideo(video, playlist)
    }

    fun closePlayer() {
        playerManager.saveCurrentPosition()
        playerManager.player.pause()
        _activeVideoForPlayer.value = null
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            repository.toggleFavorite(video.uriString, video.isFavorite)
        }
    }

    fun showVideoDetails(video: VideoItem) {
        _selectedVideoForDetails.value = video
    }

    fun dismissVideoDetails() {
        _selectedVideoForDetails.value = null
    }

    fun openExternalUri(uri: Uri) {
        viewModelScope.launch {
            val video = repository.parseExternalUri(uri)
            playVideo(video, listOf(video))
        }
    }

    fun updateSettings(newSettings: PlayerSettings) {
        _settings.value = newSettings
    }

    fun setAmoledMode(enabled: Boolean) {
        _settings.update { it.copy(amoledMode = enabled) }
    }

    fun setAccent(accent: NexaAccent) {
        _settings.update { it.copy(themeAccent = accent) }
    }

    fun setDoubleTapSeekSeconds(seconds: Int) {
        _settings.update { it.copy(doubleTapSeekSeconds = seconds) }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        _settings.update { it.copy(autoPlayNext = enabled) }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}

package com.example

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.components.LiquidGlassFloatingNavigationBar
import com.example.ui.components.NavigationTab
import com.example.ui.components.VideoDetailsDialog
import com.example.ui.screens.favorites.FavoritesScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.player.VideoPlayerScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var isInPictureInPicture = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle opening video files from other apps
        handleIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            // Permission Request Launcher
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { /* MediaStore flow automatically refreshes */ }

            LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_MEDIA_VIDEO)
                        != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                if (permissionsToRequest.isNotEmpty()) {
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                }
            }

            // SAF Document Picker launcher to open any video
            val openFilePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {}
                    viewModel.openExternalUri(uri)
                }
            }

            MyApplicationTheme(amoledMode = uiState.settings.amoledMode) {
                var currentTabId by remember { mutableStateOf("home") }

                val tabs = listOf(
                    NavigationTab("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
                    NavigationTab("library", "Library", Icons.Outlined.VideoLibrary, Icons.Filled.VideoLibrary),
                    NavigationTab("favorites", "Favorites", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
                    NavigationTab("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (uiState.settings.amoledMode) Color(0xFF000000) else Color(0xFF06080D))
                ) {
                    if (uiState.activeVideoForPlayer != null) {
                        // Fullscreen Cinematic Video Player
                        VideoPlayerScreen(
                            playerManager = viewModel.playerManager,
                            onBack = { viewModel.closePlayer() },
                            onEnterPiP = { enterPictureInPicture() }
                        )
                    } else {
                        // Main App UI with Floating Liquid Glass Bottom Navigation
                        Scaffold(
                            containerColor = Color.Transparent,
                            bottomBar = {
                                LiquidGlassFloatingNavigationBar(
                                    tabs = tabs,
                                    selectedTabId = currentTabId,
                                    onTabSelected = { currentTabId = it }
                                )
                            }
                        ) { innerPadding ->
                            Crossfade(
                                targetState = currentTabId,
                                label = "screen_crossfade"
                            ) { tab ->
                                when (tab) {
                                    "home" -> HomeScreen(
                                        uiState = uiState,
                                        onVideoClick = { video -> viewModel.playVideo(video) },
                                        onToggleFavorite = { video -> viewModel.toggleFavorite(video) },
                                        onShowDetails = { video -> viewModel.showVideoDetails(video) },
                                        onSearchChange = { q -> viewModel.setSearchQuery(q) },
                                        onCategorySelect = { cat -> viewModel.selectCategory(cat) },
                                        onOpenFilePicker = { openFilePicker.launch(arrayOf("video/*")) }
                                    )
                                    "library" -> LibraryScreen(
                                        uiState = uiState,
                                        onVideoClick = { video -> viewModel.playVideo(video) },
                                        onToggleFavorite = { video -> viewModel.toggleFavorite(video) },
                                        onShowDetails = { video -> viewModel.showVideoDetails(video) },
                                        onSearchChange = { q -> viewModel.setSearchQuery(q) },
                                        onSortChange = { order -> viewModel.setSortOrder(order) },
                                        onToggleGrid = { viewModel.toggleGridView() },
                                        onSelectFolder = { folder -> viewModel.selectFolder(folder) },
                                        onOpenFilePicker = { openFilePicker.launch(arrayOf("video/*")) }
                                    )
                                    "favorites" -> FavoritesScreen(
                                        uiState = uiState,
                                        onVideoClick = { video -> viewModel.playVideo(video) },
                                        onPlayAll = { vList ->
                                            if (vList.isNotEmpty()) {
                                                viewModel.playVideo(vList.first(), vList)
                                            }
                                        },
                                        onToggleFavorite = { video -> viewModel.toggleFavorite(video) },
                                        onShowDetails = { video -> viewModel.showVideoDetails(video) }
                                    )
                                    "settings" -> SettingsScreen(
                                        uiState = uiState,
                                        onUpdateSettings = { s -> viewModel.updateSettings(s) }
                                    )
                                }
                            }
                        }
                    }

                    // Video Details Dialog
                    uiState.selectedVideoForDetails?.let { video ->
                        VideoDetailsDialog(
                            video = video,
                            onDismiss = { viewModel.dismissVideoDetails() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri != null) {
            viewModel.openExternalUri(uri)
        }
    }

    private fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(16, 9)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            try {
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                // Device might not support PiP
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPictureInPicture.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            viewModel.playerManager.setLocked(true)
        } else {
            viewModel.playerManager.setLocked(false)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Automatically enter PiP if video is actively playing
        if (viewModel.playerManager.player.isPlaying && viewModel.uiState.value.activeVideoForPlayer != null) {
            enterPictureInPicture()
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.playerManager.saveCurrentPosition()
    }
}

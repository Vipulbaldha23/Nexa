package com.example.ui.screens.library

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.VideoItem
import com.example.data.model.VideoSortOrder
import com.example.data.repository.VideoFolder
import com.example.ui.MainUiState
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassIconButton
import com.example.ui.components.LiquidGlassSearchBar
import com.example.ui.components.VideoListItem
import com.example.ui.components.VideoThumbnailCard
import com.example.ui.components.liquidGlass
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.NexaCyan
import com.example.ui.theme.NexaViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun LibraryScreen(
    uiState: MainUiState,
    onVideoClick: (VideoItem) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onShowDetails: (VideoItem) -> Unit,
    onSearchChange: (String) -> Unit,
    onSortChange: (VideoSortOrder) -> Unit,
    onToggleGrid: () -> Unit,
    onSelectFolder: (String?) -> Unit,
    onOpenFilePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    fun shareVideo(video: VideoItem) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, video.title)
            putExtra(Intent.EXTRA_TEXT, "Watching '${video.title}' on Nexa Player: ${video.uriString}")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Library Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Video Library",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (uiState.selectedFolder != null) "Folder: ${uiState.selectedFolder}" else "${uiState.filteredVideos.size} items indexed",
                    style = MaterialTheme.typography.bodySmall,
                    color = NexaCyan
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // View Mode Toggle (Grid vs List)
                LiquidGlassIconButton(
                    icon = if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                    onClick = onToggleGrid,
                    size = 40.dp,
                    contentDescription = "Toggle Grid/List View"
                )

                // Sort Dropdown
                Box {
                    LiquidGlassIconButton(
                        icon = Icons.Default.FilterList,
                        onClick = { sortMenuExpanded = true },
                        size = 40.dp,
                        contentDescription = "Sort Options"
                    )

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier
                            .liquidGlass(
                                shape = RoundedCornerShape(14.dp),
                                backgroundColor = Color(0xF20F1626),
                                borderColor = GlassBorderCyan
                            )
                    ) {
                        VideoSortOrder.values().forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.displayName,
                                        color = if (order == uiState.sortOrder) NexaCyan else TextPrimary,
                                        fontWeight = if (order == uiState.sortOrder) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSortChange(order)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Folder Breadcrumb (if inside a folder)
        if (uiState.selectedFolder != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassButton(
                    text = "All Folders",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { onSelectFolder(null) },
                    accentColor = NexaViolet
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "/ ${uiState.selectedFolder}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Search Bar
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
            LiquidGlassSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "Search videos in library..."
            )
        }

        // Tabs: "Videos" vs "Folders"
        if (uiState.selectedFolder == null) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NexaCyan,
                        height = 2.dp
                    )
                },
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "All Videos (${uiState.videos.size})",
                            color = if (selectedTab == 0) NexaCyan else TextSecondary,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Folders (${uiState.folders.size})",
                            color = if (selectedTab == 1) NexaCyan else TextSecondary,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content Area
        if (selectedTab == 1 && uiState.selectedFolder == null) {
            // Folders Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.folders) { folder ->
                    FolderCard(
                        folder = folder,
                        onClick = { onSelectFolder(folder.name) }
                    )
                }
            }
        } else {
            // Videos Grid or List View
            if (uiState.isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.filteredVideos) { video ->
                        VideoThumbnailCard(
                            video = video,
                            onClick = { onVideoClick(video) },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onShowDetails = { onShowDetails(video) },
                            onShare = { shareVideo(video) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.filteredVideos) { video ->
                        VideoListItem(
                            video = video,
                            onClick = { onVideoClick(video) },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onShowDetails = { onShowDetails(video) },
                            onShare = { shareVideo(video) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FolderCard(
    folder: VideoFolder,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F1726))
            ) {
                if (folder.coverVideoUri.isNotEmpty()) {
                    AsyncImage(
                        model = folder.coverVideoUri,
                        contentDescription = folder.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Overlay with folder icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x55000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = NexaCyan,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${folder.videoCount} videos",
                style = MaterialTheme.typography.bodySmall,
                color = NexaViolet
            )
        }
    }
}

package com.example.ui.screens.favorites

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VideoItem
import com.example.ui.MainUiState
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.VideoListItem
import com.example.ui.components.liquidGlass
import com.example.ui.theme.GlassBorderViolet
import com.example.ui.theme.NexaCyan
import com.example.ui.theme.NexaViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FavoritesScreen(
    uiState: MainUiState,
    onVideoClick: (VideoItem) -> Unit,
    onPlayAll: (List<VideoItem>) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onShowDetails: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${uiState.favoriteVideos.size} saved videos",
                    style = MaterialTheme.typography.bodySmall,
                    color = NexaViolet
                )
            }

            if (uiState.favoriteVideos.isNotEmpty()) {
                LiquidGlassButton(
                    text = "Play All",
                    icon = Icons.Default.PlayArrow,
                    onClick = { onPlayAll(uiState.favoriteVideos) },
                    accentColor = NexaViolet
                )
            }
        }

        // List or Empty State
        if (uiState.favoriteVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(24.dp),
                            borderColor = GlassBorderViolet,
                            hasGlow = true,
                            glowColor = NexaViolet.copy(alpha = 0.2f)
                        )
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .liquidGlass(
                                    shape = CircleShape,
                                    borderColor = NexaViolet,
                                    hasGlow = true,
                                    glowColor = NexaViolet
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = NexaViolet,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Favorites Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the heart icon on any video in your library or home screen to add it to your quick-access favorites.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.favoriteVideos) { video ->
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

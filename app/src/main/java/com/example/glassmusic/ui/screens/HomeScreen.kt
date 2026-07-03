package com.example.glassmusic.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.example.glassmusic.data.Track
import com.example.glassmusic.playback.PlaybackViewModel
import com.example.glassmusic.ui.components.VisualizerView
import com.example.glassmusic.ui.components.glassmorphic
import com.example.glassmusic.ui.components.neonGlow
import com.example.glassmusic.ui.theme.*
import java.util.Locale

@UnstableApi
@Composable
fun HomeScreen(
    viewModel: PlaybackViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBgStart, DarkBgEnd)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 240.dp, top = 20.dp)
        ) {
            // Header Branding Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "D Family Music",
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "D Family",
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Equalizer icon shortcut
                        IconButton(
                            onClick = onNavigateToEqualizer,
                            modifier = Modifier
                                .size(44.dp)
                                .glassmorphic(cornerRadius = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = "Equalizer",
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // Your Playlists Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Your Playlists",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            QuickPlaylistCard(
                                title = "Favorites",
                                icon = Icons.Rounded.Favorite,
                                iconColor = Color(0xFFEF4444), // Pink/Red
                                onClick = {
                                    val favoriteTracks = (Track.DEFAULT_TRACKS + Track.CHILL_VIBES_TRACKS + Track.WORKOUT_MIX_TRACKS + Track.DEEP_FOCUS_TRACKS).filter { state.favorites.contains(it.id) }
                                    if (favoriteTracks.isNotEmpty()) {
                                        viewModel.setPlaylist(favoriteTracks, 0)
                                        onNavigateToPlayer()
                                    } else {
                                        Toast.makeText(context, "No favorites yet. Tap the heart on a song first!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        if (state.playlists.isNotEmpty()) {
                            itemsIndexed(state.playlists) { index, playlist ->
                                val colors = listOf(Color(0xFF22D3EE), Color(0xFFF97316), Color(0xFFF97316), Color(0xFFA855F7))
                                val color = colors[index % colors.size]
                                val icon = when (playlist.name.lowercase()) {
                                    "chill vibes" -> Icons.Rounded.Air
                                    "workout mix" -> Icons.Rounded.FitnessCenter
                                    "deep focus" -> Icons.Rounded.SelfImprovement
                                    else -> Icons.Rounded.QueueMusic
                                }
                                QuickPlaylistCard(
                                    title = playlist.name,
                                    icon = icon,
                                    iconColor = color,
                                    onClick = {
                                        if (playlist.tracks.isNotEmpty()) {
                                            viewModel.setPlaylist(playlist.tracks, 0)
                                            onNavigateToPlayer()
                                        } else {
                                            Toast.makeText(context, "Playlist is empty!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        } else {
                            item {
                                QuickPlaylistCard(
                                    title = "Chill Vibes",
                                    icon = Icons.Rounded.Air,
                                    iconColor = Color(0xFF22D3EE), // Cyan
                                    onClick = {
                                        viewModel.setPlaylist(Track.CHILL_VIBES_TRACKS, 0)
                                        onNavigateToPlayer()
                                    }
                                )
                            }
                            item {
                                QuickPlaylistCard(
                                    title = "Workout Mix",
                                    icon = Icons.Rounded.FitnessCenter,
                                    iconColor = Color(0xFFF97316), // Orange
                                    onClick = {
                                        viewModel.setPlaylist(Track.WORKOUT_MIX_TRACKS, 0)
                                        onNavigateToPlayer()
                                    }
                                )
                            }
                            item {
                                QuickPlaylistCard(
                                    title = "Deep Focus",
                                    icon = Icons.Rounded.SelfImprovement,
                                    iconColor = Color(0xFFA855F7), // Purple
                                    onClick = {
                                        viewModel.setPlaylist(Track.DEEP_FOCUS_TRACKS, 0)
                                        onNavigateToPlayer()
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Recently Played (Albums) Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recently Played",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.recentlyPlayed.isNotEmpty()) {
                            Text(
                                text = "Clear All",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.clearRecentlyPlayed() }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        }
                    }
                    Text(
                        text = if (state.recentlyPlayed.isNotEmpty()) "Songs:" else "Albums:",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (state.recentlyPlayed.isNotEmpty()) {
                             itemsIndexed(state.recentlyPlayed) { index, track ->
                                 RecentTrackCard(
                                     track = track,
                                     onClick = {
                                         viewModel.setPlaylist(state.recentlyPlayed, index)
                                         onNavigateToPlayer()
                                     },
                                     onDeleteClick = {
                                         viewModel.removeRecentlyPlayedTrack(track.id)
                                     }
                                 )
                             }
                        } else {
                            item {
                                AlbumFolderCard(
                                    title = "Night Drive",
                                    artworkUrl = "https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=500",
                                    onClick = {
                                        viewModel.setPlaylist(Track.NIGHT_DRIVE_TRACKS, 0)
                                        onNavigateToPlayer()
                                    }
                                )
                            }
                            item {
                                AlbumFolderCard(
                                    title = "Synthwave",
                                    artworkUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500",
                                    onClick = {
                                        viewModel.setPlaylist(Track.SYNTHWAVE_TRACKS, 0)
                                        onNavigateToPlayer()
                                    }
                                )
                            }
                            item {
                                AlbumFolderCard(
                                    title = "Ambient",
                                    artworkUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=500",
                                    onClick = {
                                        viewModel.setPlaylist(Track.AMBIENT_TRACKS, 0)
                                        onNavigateToPlayer()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sticky bottom larger visualizer/player card matching the mockup design
        if (state.currentTrack != null) {
            val track = state.currentTrack!!
            val currentPos = state.playbackProgress
            val totalDuration = state.trackDuration
            val progress = if (totalDuration > 0) currentPos.toFloat() / totalDuration else 0f
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                val bufferedProgress = if (totalDuration > 0) state.bufferedPosition.toFloat() / totalDuration else 0f

                LargePlayerPanel(
                    track = track,
                    isPlaying = state.isPlaying,
                    isLoading = state.isLoading,
                    progress = progress,
                    playbackProgressMs = currentPos,
                    durationMs = totalDuration,
                    bufferedProgress = bufferedProgress,
                    onPlayPauseToggle = { viewModel.togglePlayPause() },
                    onPreviousClick = { viewModel.skipToPrevious() },
                    onNextClick = { viewModel.skipToNext() },
                    onSliderSeek = { percent ->
                        viewModel.seekTo((percent * totalDuration).toLong())
                    },
                    onCardClick = onNavigateToPlayer
                )
            }
        }
    }
}

@Composable
fun QuickPlaylistCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(92.dp)
            .height(116.dp)
            .glassmorphic(cornerRadius = 20.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AlbumFolderCard(
    title: String,
    artworkUrl: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(115.dp)
            .clickable { onClick() }
    ) {
        Card(
            modifier = Modifier
                .size(115.dp)
                .glassmorphic(cornerRadius = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    placeholder = androidx.compose.ui.res.painterResource(id = com.example.glassmusic.R.drawable.default_cover),
                    error = androidx.compose.ui.res.painterResource(id = com.example.glassmusic.R.drawable.default_cover),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "'$title'",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecentTrackCard(
    track: Track,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(115.dp)
            .clickable { onClick() }
    ) {
        Card(
            modifier = Modifier
                .size(115.dp)
                .glassmorphic(cornerRadius = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                AsyncImage(
                    model = track.artworkModel,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    placeholder = androidx.compose.ui.res.painterResource(id = com.example.glassmusic.R.drawable.default_cover),
                    error = androidx.compose.ui.res.painterResource(id = com.example.glassmusic.R.drawable.default_cover),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
                
                // Small delete button to remove this track from recently played
                IconButton(
                    onClick = { onDeleteClick() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = track.displayArtist,
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LargePlayerPanel(
    track: Track,
    isPlaying: Boolean,
    isLoading: Boolean,
    progress: Float,
    playbackProgressMs: Long,
    durationMs: Long,
    bufferedProgress: Float,
    onPlayPauseToggle: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSliderSeek: (Float) -> Unit,
    onCardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neonGlow(color = NeonCyan, radius = 16.dp, alpha = 0.2f, borderRadius = 24.dp)
            .glassmorphic(cornerRadius = 24.dp, bgColor = Color(0x521A0F2B))
            .clickable { onCardClick() }
            .padding(16.dp)
    ) {
        Text(
            text = "Now Playing",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(68.dp)) {
                AsyncImage(
                    model = track.artworkModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = androidx.compose.ui.res.painterResource(id = com.example.glassmusic.R.drawable.default_cover),
                    error = androidx.compose.ui.res.painterResource(id = com.example.glassmusic.R.drawable.default_cover),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                )
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title.uppercase(),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.displayArtist,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Album: " + (if (track.title.contains("Lofi") || track.title.contains("Sunset")) "Midnight Melodies" else "Celestial Drift"),
                    color = NeonCyanGlow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Dynamic equalizer bars visualizer on the right side
            VisualizerView(
                isPlaying = isPlaying,
                modifier = Modifier
                    .width(44.dp)
                    .height(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontally centered playback controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = onPlayPauseToggle,
                modifier = Modifier
                    .size(44.dp)
                    .background(NeonPurple, CircleShape)
                    .neonGlow(color = NeonPurple, radius = 6.dp, alpha = 0.5f)
            ) {
                Icon(
                    imageVector = if (isPlaying || isLoading) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Seek Slider progress bar
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            LinearProgressIndicator(
                progress = bufferedProgress.coerceIn(0f, 1f),
                color = NeonCyan.copy(alpha = 0.35f),
                trackColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(horizontal = 6.dp)
                    .clip(RoundedCornerShape(2.dp))
            )

            Slider(
                value = progress,
                onValueChange = onSliderSeek,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = GlassBgColorLight.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(playbackProgressMs),
                color = TextSecondary,
                fontSize = 11.sp
            )
            Text(
                text = formatTime(durationMs),
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    return String.format(Locale.US, "%02d:%02d", min, sec)
}

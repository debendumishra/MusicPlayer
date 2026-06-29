package com.example.glassmusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.example.glassmusic.playback.PlaybackViewModel
import com.example.glassmusic.ui.components.VisualizerView
import com.example.glassmusic.ui.components.glassmorphic
import com.example.glassmusic.ui.components.neonGlow
import com.example.glassmusic.ui.theme.*
import java.util.Locale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun NowPlayingScreen(
    viewModel: PlaybackViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val track = state.currentTrack ?: return

    var lyricsExpanded by remember { mutableStateOf(false) }

    // Cover Art Rotation Animation
    val infiniteTransition = rememberInfiniteTransition(label = "now_playing_art_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBgStart, DarkBgEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Custom Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .glassmorphic(cornerRadius = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "D FAMILY MUSIC",
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp
                )

                IconButton(
                    onClick = { viewModel.toggleFavorite(track.id) },
                    modifier = Modifier
                        .size(44.dp)
                        .glassmorphic(cornerRadius = 12.dp)
                ) {
                    Icon(
                        imageVector = if (state.favorites.contains(track.id)) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (state.favorites.contains(track.id)) NeonPurple else TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Large rotating cover art with glowing neon background
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .neonGlow(
                        color = if (state.isPlaying) NeonPurple else Color.Transparent,
                        radius = 28.dp,
                        alpha = 0.45f
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = track.artworkModel,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .rotate(if (state.isPlaying) rotationAngle else 0f)
                )
                
                // Center pin for vinyl record aesthetic
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .glassmorphic(cornerRadius = 24.dp, bgColor = DarkBgStart.copy(alpha = 0.8f))
                )

                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NeonCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Loading audio...",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Title & Artist Text
            Text(
                text = track.title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = track.displayArtist,
                color = NeonCyanGlow,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Cyberpunk sine wave visualizer
            VisualizerView(isPlaying = state.isPlaying, modifier = Modifier.fillMaxWidth().height(100.dp))

            Spacer(modifier = Modifier.height(20.dp))

            // Time Slider / Seek bar
            val currentPos = state.playbackProgress
            val totalDuration = state.trackDuration
            val sliderPos = if (totalDuration > 0) currentPos.toFloat() / totalDuration else 0f

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = sliderPos,
                    onValueChange = { percent ->
                        viewModel.seekTo((percent * totalDuration).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = GlassBgColorLight
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPos),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTime(totalDuration),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Media control actions tray
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .glassmorphic(cornerRadius = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.shuffleModeEnabled) NeonCyan else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Previous Button
                IconButton(onClick = { viewModel.skipToPrevious() }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Central Play/Pause Button
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(NeonPurple, CircleShape)
                        .neonGlow(color = NeonPurple, radius = 10.dp, alpha = 0.5f)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying || state.isLoading) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Next Button
                IconButton(onClick = { viewModel.skipToNext() }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Repeat Button
                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    Icon(
                        imageVector = when (state.repeatMode) {
                            1 -> Icons.Rounded.RepeatOne
                            2 -> Icons.Rounded.Repeat
                            else -> Icons.Rounded.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode > 0) NeonCyan else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Lyrics Glass Drawer Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(cornerRadius = 20.dp)
                    .clickable { lyricsExpanded = !lyricsExpanded }
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lyrics",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Icon(
                    imageVector = if (lyricsExpanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Toggle Lyrics",
                    tint = TextSecondary
                )
            }

            AnimatedVisibility(visible = lyricsExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .glassmorphic(cornerRadius = 20.dp)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val lyricsText = getMockLyrics(track.title)
                    Text(
                        text = lyricsText,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Playlist Queue Section
            if (state.playlist.isNotEmpty()) {
                Text(
                    text = "PLAYING QUEUE",
                    color = NeonCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphic(cornerRadius = 24.dp)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.playlist.forEachIndexed { index, queueTrack ->
                        val isCurrent = state.currentTrack?.id == queueTrack.id
                        val isPlaying = state.isPlaying && isCurrent
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isCurrent) NeonPurple.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable {
                                    viewModel.playTrackAtIndex(index)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = queueTrack.artworkUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            
                            Spacer(modifier = Modifier.width(14.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = queueTrack.title,
                                    color = if (isCurrent) NeonCyan else TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = queueTrack.artist,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            if (isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "|||",
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// Utility for converting millisecond counts into human-readable track formats (MM:SS)
private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    return String.format(Locale.US, "%02d:%02d", min, sec)
}

// Generates dynamic mock lyrics aligned with track selection
private fun getMockLyrics(songName: String): String {
    return when (songName) {
        "Synthwave Cruise" -> {
            "Cruising down the grid tonight\n" +
            "Purple skies and neon lights\n" +
            "Feeling the bass begin to flow\n" +
            "Nowhere to be, just let it go...\n\n" +
            "In this dream, we fly away\n" +
            "Into the night, far from the day."
        }
        "Cyber City" -> {
            "Running through the circuits high\n" +
            "Underneath a static sky\n" +
            "Steel and shadows, cyan glow\n" +
            "Digital currents, watch them flow...\n\n" +
            "System boot up, take control\n" +
            "Feel the code inside your soul."
        }
        "Midnight Sunset" -> {
            "Chasing suns that never set\n" +
            "Visions that I can't forget\n" +
            "Lofi beats are spinning slow\n" +
            "Mellow whispers, soft and low...\n\n" +
            "Close your eyes, the world will fade\n" +
            "In this shelter we have made."
        }
        else -> {
            "Instrumental waves rolling in\n" +
            "Let the glowing sound begin\n" +
            "Lost inside this neon space\n" +
            "A calm and peaceful place..."
        }
    }
}

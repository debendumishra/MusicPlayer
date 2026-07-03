package com.example.glassmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.example.glassmusic.data.Track
import com.example.glassmusic.playback.PlaybackViewModel
import com.example.glassmusic.ui.components.glassmorphic
import com.example.glassmusic.ui.theme.*

@UnstableApi
@Composable
fun SearchScreen(
    viewModel: PlaybackViewModel,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    
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
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Centered small brand name
            Text(
                text = "D Family Music",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )
            
            Text(
                text = "YOUTUBE AUDIO SEARCH",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .glassmorphic(cornerRadius = 16.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search songs, artists...", color = TextSecondary) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = NeonCyan
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            viewModel.searchYouTube(searchQuery.trim())
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .glassmorphic(cornerRadius = 12.dp, bgColor = NeonPurple.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Enter a song name or artist to search directly from YouTube Audio proxy.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(searchResults) { index, track ->
                        SongRowItem(
                            track = track,
                            isPlaying = state.isPlaying && state.currentTrack?.id == track.id,
                            isCurrent = state.currentTrack?.id == track.id,
                            isFavorite = state.favorites.contains(track.id),
                            onClick = {
                                viewModel.setPlaylist(searchResults, index)
                                onNavigateToPlayer()
                            },
                            onFavoriteToggle = {
                                viewModel.toggleFavorite(track.id)
                            },
                            onAddToPlaylist = {
                                trackToAddToPlaylist = track
                            },
                            onDownload = {
                                viewModel.downloadTrack(track)
                            }
                        )
                    }
                }
            }
        }
        
        // Add to Playlist Dialog
        if (trackToAddToPlaylist != null) {
            val track = trackToAddToPlaylist!!
            AlertDialog(
                onDismissRequest = { trackToAddToPlaylist = null },
                title = {
                    Text(
                        text = "Add to Playlist",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    if (state.playlists.isEmpty()) {
                        Text(
                            text = "No playlists created yet. Create one in the Playlists tab!",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(state.playlists) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.addTrackToPlaylist(playlist.id, track)
                                            trackToAddToPlaylist = null
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QueueMusic,
                                        contentDescription = null,
                                        tint = NeonPurple,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = playlist.name,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { trackToAddToPlaylist = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkBgEnd.copy(alpha = 0.95f),
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

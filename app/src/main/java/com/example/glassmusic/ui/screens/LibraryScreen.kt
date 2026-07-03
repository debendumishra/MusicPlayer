package com.example.glassmusic.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.items
import com.example.glassmusic.data.Track
import com.example.glassmusic.playback.PlaybackViewModel
import com.example.glassmusic.playback.Playlist
import com.example.glassmusic.ui.components.glassmorphic
import com.example.glassmusic.ui.theme.*
import android.widget.Toast

@UnstableApi
@Composable
fun LibraryScreen(
    viewModel: PlaybackViewModel,
    onBackClick: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Favorites, 1 = Playlists, 2 = On Device
    var deviceTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    var selectedPlaylistDetails by remember { mutableStateOf<Playlist?>(null) }
    
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var localSearchQuery by remember { mutableStateOf("") }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            deviceTracks = scanLocalAudio(context)
            Toast.makeText(context, "Song deleted successfully", Toast.LENGTH_SHORT).show()
        }
    }

    val currentPlaylistDetails = state.playlists.find { it.id == selectedPlaylistDetails?.id }

    // Check permission state based on API version
    val permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context,
            permissionToCheck
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            deviceTracks = scanLocalAudio(context)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            deviceTracks = scanLocalAudio(context)
        }
    }

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

            Spacer(modifier = Modifier.height(8.dp))

            // Custom Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentPlaylistDetails != null) {
                            selectedPlaylistDetails = null
                        } else {
                            onBackClick()
                        }
                    },
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

                Spacer(modifier = Modifier.width(20.dp))

                Text(
                    text = if (currentPlaylistDetails != null) currentPlaylistDetails.name.uppercase() else "MUSIC LIBRARY",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (currentPlaylistDetails != null) {
                // Render Playlist Details View
                val playlistTracks = currentPlaylistDetails.tracks
                
                if (playlistTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No songs in this playlist.\nAdd songs from Favorites or Device Storage tabs!",
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
                        itemsIndexed(playlistTracks) { index, track ->
                            SongRowItem(
                                track = track,
                                isPlaying = state.isPlaying && state.currentTrack?.id == track.id,
                                isCurrent = state.currentTrack?.id == track.id,
                                isFavorite = state.favorites.contains(track.id),
                                onClick = {
                                    viewModel.setPlaylist(playlistTracks, index)
                                    onNavigateToPlayer()
                                },
                                onFavoriteToggle = {
                                    viewModel.toggleFavorite(track.id)
                                },
                                onRemoveFromPlaylist = {
                                    viewModel.removeTrackFromPlaylist(currentPlaylistDetails.id, track.id)
                                }
                            )
                        }
                    }
                }
            } else {
                // Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .glassmorphic(cornerRadius = 16.dp)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Favorites", "Playlists", "Device Storage").forEachIndexed { index, title ->
                        val isSelected = activeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NeonPurple.copy(alpha = 0.35f) else Color.Transparent)
                                .clickable {
                                    activeTab = index
                                    selectedFolder = null
                                    localSearchQuery = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) NeonCyanGlow else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Content List render
                when (activeTab) {
                    0 -> {
                        // Favorites tab
                        val favoriteTracks = (Track.DEFAULT_TRACKS + deviceTracks).filter { state.favorites.contains(it.id) }
                        
                        if (favoriteTracks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No favorited songs yet.\nTap the heart icon on any song to start building your library!",
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
                                itemsIndexed(favoriteTracks) { index, track ->
                                    SongRowItem(
                                        track = track,
                                        isPlaying = state.isPlaying && state.currentTrack?.id == track.id,
                                        isCurrent = state.currentTrack?.id == track.id,
                                        isFavorite = true,
                                        onClick = {
                                            viewModel.setPlaylist(favoriteTracks, index)
                                            onNavigateToPlayer()
                                        },
                                        onFavoriteToggle = {
                                            viewModel.toggleFavorite(track.id)
                                        },
                                        onAddToPlaylist = {
                                            trackToAddToPlaylist = track
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Playlists tab
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Create Playlist Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .glassmorphic(cornerRadius = 16.dp, bgColor = NeonPurple.copy(alpha = 0.15f))
                                    .clickable { showCreatePlaylistDialog = true }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Create Playlist",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Create New Playlist",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (state.playlists.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No custom playlists found.\nCreate one and add tracks from other tabs!",
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 22.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 100.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(state.playlists) { playlist ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .glassmorphic(cornerRadius = 20.dp)
                                                .clickable { selectedPlaylistDetails = playlist }
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(NeonPurple.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.QueueMusic,
                                                    contentDescription = null,
                                                    tint = NeonCyan,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.width(16.dp))
                                            
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = playlist.name,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 16.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${playlist.tracks.size} tracks",
                                                    color = TextSecondary,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            
                                            IconButton(
                                                onClick = { viewModel.deletePlaylist(playlist.id) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Delete,
                                                    contentDescription = "Delete Playlist",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // On device audio storage tab
                        if (!hasPermission) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        text = "Permission Required",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "We need audio read permissions to scan and play tracks stored locally on your device.",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { launcher.launch(permissionToCheck) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                                    ) {
                                        Text("Grant Permission", color = Color.White)
                                    }
                                }
                            }
                        } else if (deviceTracks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        text = "No Audio Files Found",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Add MP3 files to your device's Music folder, then tap Refresh.",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    IconButton(
                                        onClick = { deviceTracks = scanLocalAudio(context) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .glassmorphic(cornerRadius = 12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Refresh",
                                            tint = NeonCyan
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                // Search Box for Local Device Songs
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .glassmorphic(cornerRadius = 14.dp)
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = localSearchQuery,
                                        onValueChange = { 
                                            localSearchQuery = it
                                            if (it.isNotEmpty()) selectedFolder = null
                                        },
                                        placeholder = { Text("Search local songs...", color = TextSecondary, fontSize = 13.sp) },
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
                                    if (localSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { localSearchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Clear",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (localSearchQuery.isNotEmpty()) {
                                    // Search Results View
                                    val filteredTracks = deviceTracks.filter {
                                        it.title.contains(localSearchQuery, ignoreCase = true) ||
                                        it.artist.contains(localSearchQuery, ignoreCase = true)
                                    }

                                    if (filteredTracks.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No matching local songs found.", color = TextSecondary, fontSize = 14.sp)
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(bottom = 100.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            itemsIndexed(filteredTracks) { index, track ->
                                                SongRowItem(
                                                    track = track,
                                                    isPlaying = state.isPlaying && state.currentTrack?.id == track.id,
                                                    isCurrent = state.currentTrack?.id == track.id,
                                                    isFavorite = state.favorites.contains(track.id),
                                                    onClick = {
                                                        viewModel.setPlaylist(filteredTracks, index)
                                                        onNavigateToPlayer()
                                                    },
                                                    onFavoriteToggle = {
                                                        viewModel.toggleFavorite(track.id)
                                                    },
                                                    onAddToPlaylist = {
                                                        trackToAddToPlaylist = track
                                                    },
                                                    onDeleteLocal = {
                                                        deleteLocalTrack(context, track, deleteLauncher) {
                                                            deviceTracks = scanLocalAudio(context)
                                                            Toast.makeText(context, "Song deleted from device", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else if (selectedFolder != null) {
                                    // Drill-down songs in selected folder
                                    val folderName = selectedFolder!!
                                    val folderTracks = deviceTracks.filter { it.folderName == folderName }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedFolder = null }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowLeft,
                                            contentDescription = "Back",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$folderName /",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${folderTracks.size} songs",
                                            color = TextSecondary,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 100.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        itemsIndexed(folderTracks) { index, track ->
                                            SongRowItem(
                                                track = track,
                                                isPlaying = state.isPlaying && state.currentTrack?.id == track.id,
                                                isCurrent = state.currentTrack?.id == track.id,
                                                isFavorite = state.favorites.contains(track.id),
                                                onClick = {
                                                    viewModel.setPlaylist(folderTracks, index)
                                                    onNavigateToPlayer()
                                                },
                                                onFavoriteToggle = {
                                                    viewModel.toggleFavorite(track.id)
                                                },
                                                onAddToPlaylist = {
                                                    trackToAddToPlaylist = track
                                                },
                                                onDeleteLocal = {
                                                    deleteLocalTrack(context, track, deleteLauncher) {
                                                        deviceTracks = scanLocalAudio(context)
                                                        Toast.makeText(context, "Song deleted from device", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    // Folders List view
                                    val folders = deviceTracks.groupBy { it.folderName }
                                    
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 100.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(folders.keys.toList()) { folderName ->
                                            val count = folders[folderName]?.size ?: 0
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .glassmorphic(cornerRadius = 20.dp)
                                                    .clickable { selectedFolder = folderName }
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(NeonPurple.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Folder,
                                                        contentDescription = null,
                                                        tint = NeonCyan,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.width(16.dp))
                                                
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = folderName,
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 15.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "$count tracks",
                                                        color = TextSecondary,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                
                                                Icon(
                                                    imageVector = Icons.Rounded.ChevronRight,
                                                    contentDescription = null,
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Create Playlist Dialog
        if (showCreatePlaylistDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCreatePlaylistDialog = false
                    playlistNameInput = ""
                },
                title = {
                    Text(
                        text = "Create Playlist",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    OutlinedTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        label = { Text("Playlist Name", color = TextSecondary) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = NeonCyan,
                            unfocusedIndicatorColor = TextSecondary,
                            cursorColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (playlistNameInput.isNotBlank()) {
                                viewModel.createPlaylist(playlistNameInput.trim())
                                playlistNameInput = ""
                                showCreatePlaylistDialog = false
                            }
                        }
                    ) {
                        Text("Create", color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCreatePlaylistDialog = false
                            playlistNameInput = ""
                        }
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkBgEnd.copy(alpha = 0.95f),
                shape = RoundedCornerShape(24.dp)
            )
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

/**
 * Helper to delete a local track from internal/external storage.
 * Handles API differences for legacy path deletion, RecoverableSecurityException on Q,
 * and createDeleteRequest on R (Android 11+).
 */
private fun deleteLocalTrack(
    context: Context,
    track: Track,
    launcher: androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest>,
    onDeletedDirectly: () -> Unit
) {
    val resolver = context.contentResolver
    val uri = track.mediaUri
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val pendingIntent = MediaStore.createDeleteRequest(resolver, listOf(uri))
            val request = androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            launcher.launch(request)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error deleting file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } else {
        try {
            val deletedRows = resolver.delete(uri, null, null)
            if (deletedRows > 0) {
                onDeletedDirectly()
            } else {
                val projection = arrayOf(MediaStore.Audio.Media.DATA)
                resolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                        val path = cursor.getString(dataIndex)
                        val file = java.io.File(path)
                        if (file.exists() && file.delete()) {
                            onDeletedDirectly()
                            return
                        }
                    }
                }
                Toast.makeText(context, "Failed to delete file from device", Toast.LENGTH_SHORT).show()
            }
        } catch (securityException: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val recoverableSecurityException = securityException as? android.app.RecoverableSecurityException
                if (recoverableSecurityException != null) {
                    val request = androidx.activity.result.IntentSenderRequest.Builder(
                        recoverableSecurityException.userAction.actionIntent.intentSender
                    ).build()
                    launcher.launch(request)
                } else {
                    Toast.makeText(context, "Permission denied to delete file", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Permission denied: ${securityException.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Helper to query internal and external MediaStore directories for audio file metadata.
 */
private fun scanLocalAudio(context: Context): List<Track> {
    val trackList = mutableListOf<Track>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DATA
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    
    try {
        context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol)
                val artist = cursor.getString(artistCol)
                val duration = cursor.getLong(durationCol)
                val albumId = cursor.getLong(albumIdCol)
                val path = if (dataCol != -1) cursor.getString(dataCol) else ""
                
                val folderName = if (path.isNullOrBlank()) {
                    "Music"
                } else {
                    try {
                        java.io.File(path).parentFile?.name ?: "Music"
                    } catch (e: Exception) {
                        "Music"
                    }
                }
                
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                
                // Fetch cover art if available, otherwise fallback
                val artworkUri = "content://media/external/audio/albumart/$albumId"
                
                trackList.add(
                    Track(
                        id = id.toString(),
                        title = title,
                        artist = artist,
                        mediaUri = contentUri,
                        artworkUri = artworkUri,
                        duration = duration,
                        folderName = folderName
                    )
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return trackList
}

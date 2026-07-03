package com.example.glassmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.glassmusic.data.Track
import com.example.glassmusic.ui.components.glassmorphic
import com.example.glassmusic.ui.theme.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SongRowItem(
    track: Track,
    isPlaying: Boolean,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onDeleteLocal: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 6.dp)
            .glassmorphic(
                cornerRadius = 20.dp,
                bgColor = if (isCurrent) Color(0x2E7C3AED) else GlassBgColor
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.artworkModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = androidx.compose.ui.res.painterResource(id = com.example.glassmusic.R.drawable.default_cover),
            error = androidx.compose.ui.res.painterResource(id = com.example.glassmusic.R.drawable.default_cover),
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                color = if (isCurrent) NeonCyan else TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track.displayArtist,
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        var menuExpanded by remember { mutableStateOf(false) }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Options",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier
                    .background(DarkBgStart.copy(alpha = 0.95f))
                    .padding(vertical = 4.dp)
            ) {
                // Option 1: Favorite
                DropdownMenuItem(
                    text = { Text(if (isFavorite) "Remove Favorite" else "Favorite", color = TextPrimary, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) NeonPurple else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        onFavoriteToggle()
                        menuExpanded = false
                    }
                )

                // Option 2: Add to Playlist
                if (onAddToPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", color = TextPrimary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.PlaylistAdd,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            onAddToPlaylist()
                            menuExpanded = false
                        }
                    )
                }

                // Option 2.5: Download Song
                if (onDownload != null && !track.id.startsWith("ytlive_")) {
                    DropdownMenuItem(
                        text = { Text("Download Song", color = TextPrimary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            onDownload()
                            menuExpanded = false
                        }
                    )
                }

                // Option 3: Remove from Playlist
                if (onRemoveFromPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text("Remove from Playlist", color = Color.Red.copy(alpha = 0.8f), fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = Color.Red.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            onRemoveFromPlaylist()
                            menuExpanded = false
                        }
                    )
                }

                // Option 4: Delete local file from device
                if (onDeleteLocal != null) {
                    DropdownMenuItem(
                        text = { Text("Delete from Device", color = Color(0xFFEF4444), fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.DeleteForever,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            onDeleteLocal()
                            menuExpanded = false
                        }
                    )
                }
            }
        }

        if (isPlaying) {
            // Simulated animated audio ripples
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(24.dp)
                    .background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "|||",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

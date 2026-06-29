package com.example.glassmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@Composable
fun SongRowItem(
    track: Track,
    isPlaying: Boolean,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
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
                overflow = TextOverflow.Ellipsis
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

        if (onAddToPlaylist != null) {
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    imageVector = Icons.Rounded.PlaylistAdd,
                    contentDescription = "Add to Playlist",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (onRemoveFromPlaylist != null) {
            IconButton(onClick = onRemoveFromPlaylist) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Remove from Playlist",
                    tint = Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        IconButton(onClick = onFavoriteToggle) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) NeonPurple else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
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

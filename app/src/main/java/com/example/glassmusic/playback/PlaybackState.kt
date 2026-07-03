package com.example.glassmusic.playback

import com.example.glassmusic.data.Track

data class Playlist(
    val id: String,
    val name: String,
    val tracks: List<Track> = emptyList()
)

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val playbackProgress: Long = 0L,
    val bufferedPosition: Long = 0L,
    val trackDuration: Long = 0L,
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: Int = 0, // 0 = off, 1 = repeat one, 2 = repeat all
    val playlist: List<Track> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val playlists: List<Playlist> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList(),
    val isLoading: Boolean = false
)

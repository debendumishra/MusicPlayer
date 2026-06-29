package com.example.glassmusic.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@UnstableApi
class MusicService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        
        // Initialize ExoPlayer with default audio attributes and audio focus handling
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .build()
            
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .build()

        // Build the MediaSession wrapping the ExoPlayer
        mediaSession = MediaSession.Builder(this, player!!)
            .build()
    }

    // Return the MediaSession for controlling background audio
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

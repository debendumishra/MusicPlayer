package com.example.glassmusic.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import org.json.JSONArray
import org.json.JSONObject
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.glassmusic.data.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

@UnstableApi
class PlaybackViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PlaybackState())
    val uiState = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null
    private var appContext: Context? = null

    // Equalizer sliders (0 to 100 level) for 5 bands: 60Hz, 230Hz, 910Hz, 4kHz, 14kHz
    private val _eqBands = MutableStateFlow(listOf(50, 50, 50, 50, 50))
    val eqBands = _eqBands.asStateFlow()

    private val _eqPreset = MutableStateFlow("Off")
    val eqPreset = _eqPreset.asStateFlow()

    /**
     * Connects this ViewModel to the background MediaSessionService asynchronously.
     */
    fun initializeController(context: Context) {
        appContext = context.applicationContext
        loadPersistedData(context.applicationContext)
        fetchHealthyInstances()

        if (mediaController != null) return

        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                mediaController = controller
                if (controller != null) {
                    setupController(controller)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, { command -> command.run() }) // Inline Executor to avoid Guava dependencies
    }

    private fun setupController(controller: MediaController) {
        // Restore player flags to controller
        controller.shuffleModeEnabled = _uiState.value.shuffleModeEnabled
        controller.repeatMode = when (_uiState.value.repeatMode) {
            0 -> Player.REPEAT_MODE_OFF
            1 -> Player.REPEAT_MODE_ONE
            2 -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }

        _uiState.update { it.copy(
            isPlaying = controller.isPlaying
        )}

        // Sync track state or restore if empty
        if (controller.mediaItemCount > 0) {
            val currentMediaItem = controller.currentMediaItem
            val currentTrack = _uiState.value.playlist.find { it.id == currentMediaItem?.mediaId }
            _uiState.update { it.copy(
                currentTrack = currentTrack,
                trackDuration = controller.duration.coerceAtLeast(0L),
                playbackProgress = controller.currentPosition
            )}
        } else {
            val restoredPlaylist = _uiState.value.playlist
            val restoredTrack = _uiState.value.currentTrack
            val restoredPosition = _uiState.value.playbackProgress
            
            if (restoredPlaylist.isNotEmpty()) {
                val mediaItems = restoredPlaylist.map { track ->
                    MediaItem.Builder()
                        .setMediaId(track.id)
                        .setUri(track.mediaUri)
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artist)
                                .build()
                        )
                        .build()
                }
                val restoredIndex = restoredPlaylist.indexOfFirst { it.id == restoredTrack?.id }.coerceAtLeast(0)
                controller.setMediaItems(mediaItems, restoredIndex, restoredPosition)
                controller.prepare()
            }
        }

        // Listen for playback events from Media3 Player
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentTrack = _uiState.value.playlist.find { it.id == mediaItem?.mediaId }
                _uiState.update { state ->
                    val updatedRecents = if (currentTrack != null) {
                        val list = state.recentlyPlayed.toMutableList()
                        list.remove(currentTrack)
                        list.add(0, currentTrack)
                        if (list.size > 15) list.removeAt(list.size - 1)
                        list
                    } else {
                        state.recentlyPlayed
                    }
                    state.copy(
                        currentTrack = currentTrack,
                        recentlyPlayed = updatedRecents,
                        trackDuration = controller.duration.coerceAtLeast(0L),
                        isLoading = true
                    )
                }
                appContext?.let { context ->
                    savePlaybackState(context, controller.currentPosition)
                    saveRecentlyPlayed(context, _uiState.value.recentlyPlayed)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _uiState.update { it.copy(
                    trackDuration = controller.duration.coerceAtLeast(0L),
                    isLoading = playbackState == Player.STATE_BUFFERING
                ) }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                error.printStackTrace()
                
                val currentTrack = _uiState.value.currentTrack
                if (currentTrack != null && currentTrack.id.startsWith("yt_")) {
                    val oldUri = currentTrack.mediaUri.toString()
                    val oldHost = currentTrack.mediaUri.host ?: ""
                    val nextHost = getNextHealthyInstanceHost(oldHost)
                    
                    if (nextHost != null) {
                        val newUriStr = oldUri.replace(oldHost, nextHost)
                        val updatedTrack = currentTrack.copy(mediaUri = Uri.parse(newUriStr))
                        
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            appContext?.let {
                                android.widget.Toast.makeText(
                                    it,
                                    "Playback error. Retrying with backup server...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        
                        _uiState.update { state ->
                            val updatedPlaylist = state.playlist.map { 
                                if (it.id == currentTrack.id) updatedTrack else it 
                            }
                            state.copy(
                                playlist = updatedPlaylist,
                                currentTrack = updatedTrack,
                                isLoading = true
                            )
                        }
                        
                        mediaController?.let { controller ->
                            val currentPos = controller.currentPosition
                            val currentIndex = controller.currentMediaItemIndex
                            
                            controller.stop()
                            controller.clearMediaItems()
                            
                            val mediaItems = _uiState.value.playlist.map { track ->
                                val builder = MediaItem.Builder()
                                    .setMediaId(track.id)
                                    .setUri(track.mediaUri)
                                    .setMediaMetadata(
                                        androidx.media3.common.MediaMetadata.Builder()
                                            .setTitle(track.title)
                                            .setArtist(track.artist)
                                            .build()
                                    )
                                if (track.mediaUri.toString().contains("/live/")) {
                                    builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                                }
                                builder.build()
                            }
                            
                            controller.setMediaItems(mediaItems, currentIndex, currentPos)
                            controller.prepare()
                            controller.play()
                        }
                        return
                    }
                }
                
                _uiState.update { it.copy(isPlaying = false, isLoading = false) }
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    appContext?.let {
                        android.widget.Toast.makeText(
                            it,
                            "Unable to load song: ${error.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _uiState.update { it.copy(shuffleModeEnabled = shuffleModeEnabled) }
                appContext?.let { saveShuffleState(it, shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                val modeInt = when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> 0
                    Player.REPEAT_MODE_ONE -> 1
                    Player.REPEAT_MODE_ALL -> 2
                    else -> 0
                }
                _uiState.update { it.copy(repeatMode = modeInt) }
                appContext?.let { saveRepeatState(it, modeInt) }
            }
        })

        if (controller.isPlaying) {
            startProgressTracker()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            var saveCounter = 0
            while (true) {
                mediaController?.let { controller ->
                    val pos = controller.currentPosition
                    _uiState.update { it.copy(
                        playbackProgress = pos,
                        trackDuration = controller.duration.coerceAtLeast(0L)
                    ) }
                    
                    saveCounter++
                    if (saveCounter >= 8) { // Save every 2 seconds
                        saveCounter = 0
                        appContext?.let { savePlaybackState(it, pos) }
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    fun setPlaylist(tracks: List<Track>, playIndex: Int = 0) {
        _uiState.update { it.copy(playlist = tracks, isLoading = true) }
        appContext?.let { savePlaybackState(it, 0L) }
        
        val controller = mediaController ?: return
        controller.stop()
        controller.clearMediaItems()
        
        val mediaItems = tracks.map { track ->
            val builder = MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.mediaUri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .build()
                )
            if (track.mediaUri.toString().contains("/live/")) {
                builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            }
            builder.build()
        }
        
        controller.setMediaItems(mediaItems, playIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun playTrackAtIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekTo(index, 0L)
            controller.play()
        } else {
            setPlaylist(_uiState.value.playlist, index)
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE) {
                controller.prepare()
            }
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        controller.seekTo(positionMs)
        _uiState.update { it.copy(playbackProgress = positionMs) }
        appContext?.let { savePlaybackState(it, positionMs) }
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNext()
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.hasPreviousMediaItem()) {
            controller.seekToPrevious()
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val nextMode = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = nextMode
    }

    fun toggleRepeat() {
        val controller = mediaController ?: return
        val currentMode = _uiState.value.repeatMode
        val nextMode = (currentMode + 1) % 3
        controller.repeatMode = when (nextMode) {
            0 -> Player.REPEAT_MODE_OFF
            1 -> Player.REPEAT_MODE_ONE
            2 -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleFavorite(trackId: String) {
        _uiState.update { state ->
            val newFavorites = state.favorites.toMutableSet()
            if (newFavorites.contains(trackId)) {
                newFavorites.remove(trackId)
            } else {
                newFavorites.add(trackId)
            }
            state.copy(favorites = newFavorites)
        }
        appContext?.let { saveFavorites(it) }
    }

    fun updateEqBand(index: Int, value: Int) {
        _eqPreset.value = "Custom"
        val newList = _eqBands.value.toMutableList()
        if (index in newList.indices) {
            newList[index] = value.coerceIn(0, 100)
            _eqBands.value = newList
        }
        appContext?.let { saveEqualizerState(it) }
    }

    fun selectPreset(presetName: String) {
        _eqPreset.value = presetName
        _eqBands.value = when (presetName) {
            "Bass Booster" -> listOf(90, 75, 55, 45, 40)
            "Electronic" -> listOf(80, 65, 50, 70, 85)
            "Vocal" -> listOf(40, 50, 80, 75, 60)
            "Jazz" -> listOf(70, 60, 50, 65, 70)
            "Off" -> listOf(50, 50, 50, 50, 50)
            else -> _eqBands.value
        }
        appContext?.let { saveEqualizerState(it) }
    }

    fun clearRecentlyPlayed() {
        _uiState.update { it.copy(recentlyPlayed = emptyList()) }
        appContext?.let { saveRecentlyPlayed(it, emptyList()) }
    }

    fun removeRecentlyPlayedTrack(trackId: String) {
        _uiState.update { state ->
            val updated = state.recentlyPlayed.filterNot { it.id == trackId }
            state.copy(recentlyPlayed = updated)
        }
        appContext?.let { 
            saveRecentlyPlayed(it, _uiState.value.recentlyPlayed)
        }
    }

    fun createPlaylist(name: String) {
        _uiState.update { state ->
            val newPlaylists = state.playlists.toMutableList()
            val newPlaylist = Playlist(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                tracks = emptyList()
            )
            newPlaylists.add(newPlaylist)
            state.copy(playlists = newPlaylists)
        }
        appContext?.let { savePlaylists(it) }
    }

    fun deletePlaylist(playlistId: String) {
        _uiState.update { state ->
            val newPlaylists = state.playlists.filterNot { it.id == playlistId }
            state.copy(playlists = newPlaylists)
        }
        appContext?.let { savePlaylists(it) }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        _uiState.update { state ->
            val newPlaylists = state.playlists.map { playlist ->
                if (playlist.id == playlistId) {
                    if (!playlist.tracks.any { it.id == track.id }) {
                        playlist.copy(tracks = playlist.tracks + track)
                    } else {
                        playlist
                    }
                } else {
                    playlist
                }
            }
            state.copy(playlists = newPlaylists)
        }
        appContext?.let { savePlaylists(it) }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        _uiState.update { state ->
            val newPlaylists = state.playlists.map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(tracks = playlist.tracks.filterNot { it.id == trackId })
                } else {
                    playlist
                }
            }
            state.copy(playlists = newPlaylists)
        }
        appContext?.let { savePlaylists(it) }
    }

    // --- State Persistence & JSON Helpers ---

    private fun saveRecentlyPlayed(context: Context, tracks: List<Track>) {
        val prefs = context.getSharedPreferences("glass_music_prefs", Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        tracks.forEach { jsonArray.put(serializeTrack(it)) }
        prefs.edit().putString("recently_played", jsonArray.toString()).apply()
    }

    private fun serializeTrack(track: Track): JSONObject {
        val obj = JSONObject()
        obj.put("id", track.id)
        obj.put("title", track.title)
        obj.put("artist", track.artist)
        obj.put("mediaUri", track.mediaUri.toString())
        obj.put("artworkUri", track.artworkUri ?: "")
        obj.put("duration", track.duration)
        return obj
    }

    private fun deserializeTrack(obj: JSONObject): Track {
        val id = obj.getString("id")
        val title = obj.getString("title")
        val artist = obj.getString("artist")
        val mediaUriStr = obj.getString("mediaUri")
        val artworkUri = obj.optString("artworkUri", "").takeIf { it.isNotEmpty() } ?: "android.resource://com.example.glassmusic/drawable/default_cover"
        val duration = obj.getLong("duration")
        return Track(
            id = id,
            title = title,
            artist = artist,
            mediaUri = Uri.parse(mediaUriStr),
            artworkUri = artworkUri,
            duration = duration
        )
    }

    private fun saveFavorites(context: Context) {
        val prefs = context.getSharedPreferences("glass_music_prefs", Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        _uiState.value.favorites.forEach { jsonArray.put(it) }
        prefs.edit().putString("favorites", jsonArray.toString()).apply()
    }

    private fun savePlaylists(context: Context) {
        val prefs = context.getSharedPreferences("glass_music_prefs", Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        _uiState.value.playlists.forEach { playlist ->
            val playlistObj = JSONObject()
            playlistObj.put("id", playlist.id)
            playlistObj.put("name", playlist.name)
            
            val tracksArray = JSONArray()
            playlist.tracks.forEach { track ->
                tracksArray.put(serializeTrack(track))
            }
            playlistObj.put("tracks", tracksArray)
            jsonArray.put(playlistObj)
        }
        prefs.edit().putString("playlists", jsonArray.toString()).apply()
    }

    private fun savePlaybackState(context: Context, position: Long) {
        val prefs = context.getSharedPreferences("glass_music_prefs", Context.MODE_PRIVATE)
        val state = _uiState.value
        val editor = prefs.edit()
        
        val playlistArray = JSONArray()
        state.playlist.forEach { playlistArray.put(serializeTrack(it)) }
        editor.putString("last_playlist", playlistArray.toString())
        editor.putString("last_track_id", state.currentTrack?.id)
        editor.putLong("last_position", position)
        editor.apply()
    }

    private fun loadPersistedData(context: Context) {
        val prefs = context.getSharedPreferences("glass_music_prefs", Context.MODE_PRIVATE)
        
        // 1. Load Favorites
        val favoritesJson = prefs.getString("favorites", "[]") ?: "[]"
        val favoritesSet = mutableSetOf<String>()
        try {
            val jsonArray = JSONArray(favoritesJson)
            for (i in 0 until jsonArray.length()) {
                favoritesSet.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Load Playlists
        val playlistsJson = prefs.getString("playlists", "[]") ?: "[]"
        val playlistsList = mutableListOf<Playlist>()
        try {
            val jsonArray = JSONArray(playlistsJson)
            for (i in 0 until jsonArray.length()) {
                val playlistObj = jsonArray.getJSONObject(i)
                val id = playlistObj.getString("id")
                val name = playlistObj.getString("name")
                val tracksArray = playlistObj.getJSONArray("tracks")
                val tracksList = mutableListOf<Track>()
                for (j in 0 until tracksArray.length()) {
                    val trackObj = tracksArray.getJSONObject(j)
                    tracksList.add(deserializeTrack(trackObj))
                }
                playlistsList.add(Playlist(id, name, tracksList))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Load Last Played Playlist, Track & Position
        val lastPlaylistJson = prefs.getString("last_playlist", "[]") ?: "[]"
        val lastPlaylist = mutableListOf<Track>()
        try {
            val jsonArray = JSONArray(lastPlaylistJson)
            for (i in 0 until jsonArray.length()) {
                lastPlaylist.add(deserializeTrack(jsonArray.getJSONObject(i)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val lastTrackId = prefs.getString("last_track_id", null)
        val lastPosition = prefs.getLong("last_position", 0L)

        val recentlyPlayedJson = prefs.getString("recently_played", "[]") ?: "[]"
        val recentlyPlayedList = mutableListOf<Track>()
        try {
            val jsonArray = JSONArray(recentlyPlayedJson)
            for (i in 0 until jsonArray.length()) {
                recentlyPlayedList.add(deserializeTrack(jsonArray.getJSONObject(i)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val shuffleMode = prefs.getBoolean("shuffle_mode", false)
        val repeatMode = prefs.getInt("repeat_mode", 0)

        val eqPresetStr = prefs.getString("eq_preset", "Off") ?: "Off"
        val eqBandsJson = prefs.getString("eq_bands", "[50,50,50,50,50]") ?: "[50,50,50,50,50]"
        val eqBandsList = mutableListOf<Int>()
        try {
            val jsonArray = JSONArray(eqBandsJson)
            for (i in 0 until jsonArray.length()) {
                eqBandsList.add(jsonArray.getInt(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (eqBandsList.size != 5) {
            eqBandsList.clear()
            eqBandsList.addAll(listOf(50, 50, 50, 50, 50))
        }

        _eqPreset.value = eqPresetStr
        _eqBands.value = eqBandsList

        _uiState.update { state ->
            state.copy(
                favorites = favoritesSet,
                playlists = playlistsList,
                playlist = lastPlaylist,
                currentTrack = lastPlaylist.find { it.id == lastTrackId },
                playbackProgress = lastPosition,
                recentlyPlayed = recentlyPlayedList,
                shuffleModeEnabled = shuffleMode,
                repeatMode = repeatMode
            )
        }
    }

    private fun saveShuffleState(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("glass_music_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("shuffle_mode", enabled).apply()
    }

    private fun saveRepeatState(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences("glass_music_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("repeat_mode", mode).apply()
    }

    private fun saveEqualizerState(context: Context) {
        val prefs = context.getSharedPreferences("glass_music_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("eq_preset", _eqPreset.value)
        val bandsJson = JSONArray()
        _eqBands.value.forEach { bandsJson.put(it) }
        editor.putString("eq_bands", bandsJson.toString())
        editor.apply()
    }

    private var healthyStreamingInstances = listOf(
        "https://inv.zoomerville.com",
        "https://yewtu.be",
        "https://invidious.flokinet.to",
        "https://invidious.projectsegfaut.im"
    )

    private fun fetchHealthyInstances() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://api.invidious.io/instances.json")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(text)
                    val list = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONArray(i)
                        val metadata = item.getJSONObject(1)
                        val isApi = metadata.optBoolean("api", false)
                        val type = metadata.optString("type", "")
                        val uri = metadata.optString("uri", "")
                        val monitor = metadata.optJSONObject("monitor")
                        val isDown = monitor?.optBoolean("down", false) ?: false
                        val lastStatus = monitor?.optInt("last_status", 200) ?: 200
                        
                        if (isApi && type == "https" && !isDown && lastStatus == 200 && uri.isNotEmpty()) {
                            list.add(uri.trimEnd('/'))
                        }
                    }
                    if (list.isNotEmpty()) {
                        healthyStreamingInstances = list
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchYouTube(query: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val results = mutableListOf<Track>()
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            
            // Try direct HTML scraping of youtube results using ytInitialData
            try {
                val url = java.net.URL("https://www.youtube.com/results?search_query=$encodedQuery&sp=EgIQAQ%253D%253D")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                
                if (conn.responseCode == 200) {
                    val html = conn.inputStream.bufferedReader().use { it.readText() }
                    val prefix = "ytInitialData = "
                    val startIdx = html.indexOf(prefix)
                    if (startIdx >= 0) {
                        val dataStart = startIdx + prefix.length
                        var endIdx = html.indexOf(";</script>", dataStart)
                        if (endIdx < 0) {
                            endIdx = html.indexOf(";var ", dataStart)
                        }
                        if (endIdx >= 0) {
                            val jsonStr = html.substring(dataStart, endIdx).trim()
                            val json = JSONObject(jsonStr)
                            val contents = json.getJSONObject("contents")
                                .getJSONObject("twoColumnSearchResultsRenderer")
                                .getJSONObject("primaryContents")
                                .getJSONObject("sectionListRenderer")
                                .getJSONArray("contents")
                            
                            if (contents.length() > 0) {
                                val itemSection = contents.getJSONObject(0)
                                if (itemSection.has("itemSectionRenderer")) {
                                    val items = itemSection.getJSONObject("itemSectionRenderer").getJSONArray("contents")
                                    val streamingBase = healthyStreamingInstances.firstOrNull() ?: "https://inv.zoomerville.com"
                                    
                                    for (i in 0 until items.length()) {
                                        val item = items.getJSONObject(i)
                                        if (item.has("videoRenderer")) {
                                            val v = item.getJSONObject("videoRenderer")
                                            val videoId = v.getString("videoId")
                                            
                                            val titleObj = v.getJSONObject("title")
                                            val titleRuns = titleObj.getJSONArray("runs")
                                            val title = if (titleRuns.length() > 0) titleRuns.getJSONObject(0).getString("text") else "Unknown Title"
                                            
                                            val ownerTextObj = v.getJSONObject("ownerText")
                                            val ownerRuns = ownerTextObj.getJSONArray("runs")
                                            val author = if (ownerRuns.length() > 0) ownerRuns.getJSONObject(0).getString("text") else "Unknown Artist"
                                            
                                            val thumbObj = v.getJSONObject("thumbnail")
                                            val thumbnails = thumbObj.getJSONArray("thumbnails")
                                            val artworkUrl = if (thumbnails.length() > 0) thumbnails.getJSONObject(0).getString("url") else "android.resource://com.example.glassmusic/drawable/default_cover"
                                            
                                            var durationMs = 0L
                                            val isLive = v.optJSONArray("badges")?.toString()?.contains("LIVE") == true ||
                                                         v.optJSONObject("thumbnailOverlays")?.toString()?.contains("LIVE") == true ||
                                                         !v.has("lengthText")
                                            
                                            if (v.has("lengthText")) {
                                                val lengthTextObj = v.getJSONObject("lengthText")
                                                val simpleText = lengthTextObj.optString("simpleText", "")
                                                durationMs = parseDurationText(simpleText)
                                            }
                                            
                                            val audioUrl = if (isLive) {
                                                resolveLiveStreamUrl(videoId) ?: ""
                                            } else {
                                                "$streamingBase/latest_version?id=$videoId&itag=140&local=true"
                                            }
                                            
                                            if (isLive && audioUrl.isEmpty()) {
                                                continue
                                            }
                                            
                                            results.add(
                                                Track(
                                                    id = "yt_$videoId",
                                                    title = title,
                                                    artist = author,
                                                    mediaUri = Uri.parse(audioUrl),
                                                    artworkUri = artworkUrl,
                                                    duration = durationMs
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Fallback 1: Query dynamic Invidious search API
            if (results.isEmpty()) {
                val streamingBase = healthyStreamingInstances.firstOrNull() ?: "https://inv.zoomerville.com"
                for (instance in healthyStreamingInstances) {
                    try {
                        val url = java.net.URL("$instance/api/v1/search?q=$encodedQuery&type=video")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        conn.connectTimeout = 8000
                        conn.readTimeout = 8000
                        
                        if (conn.responseCode == 200) {
                            val text = conn.inputStream.bufferedReader().use { it.readText() }
                            val jsonArray = JSONArray(text)
                            for (i in 0 until jsonArray.length()) {
                                val videoObj = jsonArray.getJSONObject(i)
                                val videoId = videoObj.getString("videoId")
                                val title = videoObj.getString("title")
                                val author = videoObj.getString("author")
                                val thumbnails = videoObj.optJSONArray("videoThumbnails")
                                val artworkUrl = if (thumbnails != null && thumbnails.length() > 0) {
                                    thumbnails.getJSONObject(0).getString("url")
                                } else {
                                    "android.resource://com.example.glassmusic/drawable/default_cover"
                                }
                                val isLive = videoObj.optBoolean("liveNow", false)
                                val duration = videoObj.optLong("lengthSeconds", 0L) * 1000L
                                val audioUrl = if (isLive) {
                                    resolveLiveStreamUrl(videoId) ?: ""
                                } else {
                                    "$streamingBase/latest_version?id=$videoId&itag=140&local=true"
                                }
                                
                                if (isLive && audioUrl.isEmpty()) {
                                    continue
                                }
                                
                                results.add(
                                    Track(
                                        id = "yt_$videoId",
                                        title = title,
                                        artist = author,
                                        mediaUri = Uri.parse(audioUrl),
                                        artworkUri = artworkUrl,
                                        duration = duration
                                    )
                                )
                            }
                            break
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            // Fallback 2: Query Piped search API
            if (results.isEmpty()) {
                val streamingBase = healthyStreamingInstances.firstOrNull() ?: "https://inv.zoomerville.com"
                val pipedInstances = listOf(
                    "https://pipedapi.kavin.rocks",
                    "https://piped-api.garudalinux.org",
                    "https://pipedapi.tokhmi.xyz"
                )
                for (instance in pipedInstances) {
                    try {
                        val url = java.net.URL("$instance/search?q=$encodedQuery&filter=music")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        conn.connectTimeout = 8000
                        conn.readTimeout = 8000
                        
                        if (conn.responseCode == 200) {
                            val text = conn.inputStream.bufferedReader().use { it.readText() }
                            val jsonObj = JSONObject(text)
                            val items = jsonObj.getJSONArray("items")
                            for (i in 0 until items.length()) {
                                val item = items.getJSONObject(i)
                                val type = item.optString("type")
                                if (type == "stream" || type == "video" || type == "live") {
                                    val videoUrl = item.getString("url")
                                    val videoId = videoUrl.substringAfter("v=")
                                    val title = item.getString("title")
                                    val uploader = item.optString("uploaderName", "Unknown Artist")
                                    val artworkUrl = item.optString("thumbnail", "android.resource://com.example.glassmusic/drawable/default_cover").takeIf { it.isNotEmpty() } ?: "android.resource://com.example.glassmusic/drawable/default_cover"
                                    val isLive = item.optBoolean("isLive", false) || type == "live"
                                    val duration = item.optLong("duration", 0L) * 1000L
                                    val audioUrl = if (isLive) {
                                        resolveLiveStreamUrl(videoId) ?: ""
                                    } else {
                                        "$streamingBase/latest_version?id=$videoId&itag=140&local=true"
                                    }
                                    
                                    if (isLive && audioUrl.isEmpty()) {
                                        continue
                                    }
                                    
                                    results.add(
                                        Track(
                                            id = "yt_$videoId",
                                            title = title,
                                            artist = uploader,
                                            mediaUri = Uri.parse(audioUrl),
                                            artworkUri = artworkUrl,
                                            duration = duration
                                        )
                                    )
                                }
                            }
                            break
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            // Verify results in parallel using async/awaitAll to weed out unplayable links
            var verifiedResults = mutableListOf<Track>()
            if (results.isNotEmpty()) {
                val deferreds = results.map { track ->
                    async {
                        if (verifyStreamPlayable(track.mediaUri.toString())) track else null
                    }
                }
                verifiedResults.addAll(deferreds.awaitAll().filterNotNull())
            }
            
            // If all of them failed, the current instance might be down/blocking. Failover to a backup and re-verify!
            if (verifiedResults.isEmpty() && results.isNotEmpty()) {
                val primaryStreamingBase = healthyStreamingInstances.firstOrNull() ?: "https://inv.zoomerville.com"
                val backupInstance = healthyStreamingInstances.firstOrNull { it != primaryStreamingBase }
                if (backupInstance != null) {
                    val backupHost = Uri.parse(backupInstance).host ?: ""
                    val updatedResults = results.map { track ->
                        val oldUri = track.mediaUri.toString()
                        val oldHost = track.mediaUri.host ?: ""
                        val newUriStr = oldUri.replace(oldHost, backupHost)
                        track.copy(mediaUri = Uri.parse(newUriStr))
                    }
                    val deferreds = updatedResults.map { track ->
                        async {
                            if (verifyStreamPlayable(track.mediaUri.toString())) track else null
                        }
                    }
                    verifiedResults.addAll(deferreds.awaitAll().filterNotNull())
                }
            }
            
            _searchResults.value = verifiedResults
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun verifyStreamPlayable(urlStr: String): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        var conn: java.net.HttpURLConnection? = null
        try {
            val url = java.net.URL(urlStr)
            conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.instanceFollowRedirects = false // DO NOT FOLLOW REDIRECTS to Google Video
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            
            val responseCode = conn.responseCode
            // HTTP responses in the successful 2xx/3xx range indicate stream connection is active and playable
            if (responseCode in 200..399) {
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            return@withContext false
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseDurationText(simpleText: String): Long {
        if (simpleText.isEmpty()) return 0L
        val parts = simpleText.split(":")
        var seconds = 0L
        try {
            if (parts.size == 2) {
                seconds = parts[0].toLong() * 60 + parts[1].toLong()
            } else if (parts.size == 3) {
                seconds = parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return seconds * 1000L
    }

    private suspend fun resolveLiveStreamUrl(videoId: String): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val pipedInstances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://piped-api.garudalinux.org",
            "https://pipedapi.tokhmi.xyz"
        )
        for (instance in pipedInstances) {
            try {
                val url = java.net.URL("$instance/streams/$videoId")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonObj = JSONObject(text)
                    if (jsonObj.has("hls")) {
                        val hlsUrl = jsonObj.getString("hls")
                        if (hlsUrl.isNotEmpty()) {
                            return@withContext hlsUrl
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return@withContext null
    }

    private fun getNextHealthyInstanceHost(oldHost: String): String? {
        val hosts = healthyStreamingInstances.map { Uri.parse(it).host }.filterNotNull()
        if (hosts.isEmpty()) return null
        
        val currentIndex = hosts.indexOf(oldHost)
        if (currentIndex == -1) {
            return hosts.first()
        }
        
        val nextIndex = currentIndex + 1
        return if (nextIndex < hosts.size) {
            hosts[nextIndex]
        } else {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTracker()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}

package com.example.glassmusic.data

import android.net.Uri

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val mediaUri: Uri,
    val artworkUri: String?, // Web URL or local media store path
    val duration: Long = 0L,  // Track length in milliseconds
    val folderName: String = "Music"
) {
    val displayArtist: String
        get() = if (artist.isNullOrBlank() || artist.equals("<unknown>", ignoreCase = true) || artist.equals("unknown", ignoreCase = true) || artist.equals("unknown artist", ignoreCase = true)) {
            "D Family"
        } else {
            artist
        }

    val artworkModel: Any
        get() = when {
            artworkUri == null || artworkUri.isBlank() -> {
                com.example.glassmusic.R.drawable.default_cover
            }
            artworkUri.startsWith("http://") || artworkUri.startsWith("https://") -> {
                artworkUri
            }
            artworkUri.startsWith("//") -> {
                "https:$artworkUri"
            }
            artworkUri.startsWith("android.resource://") || artworkUri.startsWith("content://") -> {
                artworkUri
            }
            else -> {
                com.example.glassmusic.R.drawable.default_cover
            }
        }

    companion object {
        val DEFAULT_TRACKS = listOf(
            Track(
                id = "1",
                title = "Synthwave Cruise",
                artist = "Neon Horizon",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500",
                duration = 372000L
            ),
            Track(
                id = "2",
                title = "Cyber City",
                artist = "Glitch Mobster",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500",
                duration = 423000L
            ),
            Track(
                id = "3",
                title = "Midnight Sunset",
                artist = "Lofi Dreamer",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500",
                duration = 344000L
            ),
            Track(
                id = "4",
                title = "Neon Eclipse",
                artist = "Stargazer",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=500",
                duration = 302000L
            ),
            Track(
                id = "5",
                title = "Retro Overdrive",
                artist = "Arcade Fire",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1534796636912-3b95b3ab5986?w=500",
                duration = 368000L
            )
        )

        val CHILL_VIBES_TRACKS = listOf(
            Track(
                id = "chill_1",
                title = "Midnight Sunset",
                artist = "Lofi Dreamer",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500",
                duration = 344000L
            ),
            Track(
                id = "chill_2",
                title = "Lofi Rain",
                artist = "Cloud Beats",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1486572788966-cfd3df1f5b42?w=500",
                duration = 384000L
            ),
            Track(
                id = "chill_3",
                title = "Soft Breeze",
                artist = "Mellow Breeze",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500",
                duration = 412000L
            )
        )

        val WORKOUT_MIX_TRACKS = listOf(
            Track(
                id = "workout_1",
                title = "Synthwave Cruise",
                artist = "Neon Horizon",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500",
                duration = 372000L
            ),
            Track(
                id = "workout_2",
                title = "Retro Overdrive",
                artist = "Arcade Fire",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1534796636912-3b95b3ab5986?w=500",
                duration = 368000L
            ),
            Track(
                id = "workout_3",
                title = "Cyber City",
                artist = "Glitch Mobster",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500",
                duration = 423000L
            )
        )

        val DEEP_FOCUS_TRACKS = listOf(
            Track(
                id = "focus_1",
                title = "Neon Eclipse",
                artist = "Stargazer",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=500",
                duration = 302000L
            ),
            Track(
                id = "focus_2",
                title = "Zen Garden",
                artist = "Ambient Whisperer",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=500",
                duration = 398000L
            ),
            Track(
                id = "focus_3",
                title = "Cosmic Calm",
                artist = "Space Voyager",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500",
                duration = 432000L
            )
        )

        val NIGHT_DRIVE_TRACKS = listOf(
            Track(
                id = "night_1",
                title = "Night Drive",
                artist = "Tokyo Rider",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=500",
                duration = 388000L
            ),
            Track(
                id = "night_2",
                title = "City Lights",
                artist = "Metropolitan",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=500",
                duration = 365000L
            )
        )

        val SYNTHWAVE_TRACKS = listOf(
            Track(
                id = "synth_1",
                title = "Laser Beam",
                artist = "Outrun 84",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500",
                duration = 377000L
            ),
            Track(
                id = "synth_2",
                title = "Grid Runner",
                artist = "Vectorman",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500",
                duration = 399000L
            )
        )

        val AMBIENT_TRACKS = listOf(
            Track(
                id = "ambient_1",
                title = "Dreamy Echoes",
                artist = "Ether Dream",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=500",
                duration = 445000L
            ),
            Track(
                id = "ambient_2",
                title = "Cloud Floating",
                artist = "Sky Calm",
                mediaUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3"),
                artworkUri = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500",
                duration = 412000L
            )
        )
    }
}

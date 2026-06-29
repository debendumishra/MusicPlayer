package com.example.glassmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import com.example.glassmusic.ui.screens.SearchScreen
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.glassmusic.playback.PlaybackViewModel
import com.example.glassmusic.ui.components.glassmorphic
import com.example.glassmusic.ui.components.neonGlow
import com.example.glassmusic.ui.screens.EqualizerScreen
import com.example.glassmusic.ui.screens.HomeScreen
import com.example.glassmusic.ui.screens.LibraryScreen
import com.example.glassmusic.ui.screens.NowPlayingScreen
import com.example.glassmusic.ui.theme.GlassicMusicTheme
import com.example.glassmusic.ui.theme.NeonCyan
import com.example.glassmusic.ui.theme.NeonPurple
import com.example.glassmusic.ui.theme.NeonCyanGlow
import com.example.glassmusic.ui.theme.DarkBgEnd
import com.example.glassmusic.ui.theme.TextPrimary
import com.example.glassmusic.ui.theme.TextSecondary

@UnstableApi
class MainActivity : ComponentActivity() {

    private val viewModel: PlaybackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Connect the View-Model with our background Media3 service
        viewModel.initializeController(applicationContext)

        setContent {
            GlassicMusicTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                var showProfileDialog by remember { mutableStateOf(false) }
                val state by viewModel.uiState.collectAsState()

                Scaffold(
                    bottomBar = {
                        // Display bottom navbar on root screens: home, library, search, player
                        if (currentRoute == "home" || currentRoute == "library" || currentRoute == "search" || currentRoute == "player") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .neonGlow(color = NeonCyan, radius = 10.dp, alpha = 0.15f, borderRadius = 20.dp)
                                        .glassmorphic(cornerRadius = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Dashboard navigation clicker
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (currentRoute != "home") {
                                                    navController.navigate("home") {
                                                        popUpTo("home") { inclusive = false }
                                                    }
                                                }
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Home,
                                            contentDescription = "Home",
                                            tint = if (currentRoute == "home") NeonCyan else TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Home",
                                            color = if (currentRoute == "home") NeonCyan else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Search navigation clicker
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (currentRoute != "search") {
                                                    navController.navigate("search")
                                                }
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = "Search",
                                            tint = if (currentRoute == "search") NeonCyan else TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Search",
                                            color = if (currentRoute == "search") NeonCyan else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Library navigation clicker
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (currentRoute != "library") {
                                                    navController.navigate("library")
                                                }
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LibraryMusic,
                                            contentDescription = "Library",
                                            tint = if (currentRoute == "library") NeonCyan else TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Library",
                                            color = if (currentRoute == "library") NeonCyan else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Profile clicker (from the reference mock layout)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                showProfileDialog = true
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = "Profile",
                                            tint = if (showProfileDialog) NeonCyan else TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Profile",
                                            color = if (showProfileDialog) NeonCyan else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    },
                    containerColor = Color.Transparent
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToPlayer = { navController.navigate("player") },
                                onNavigateToEqualizer = { navController.navigate("equalizer") },
                                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                            )
                        }
                        composable("player") {
                            NowPlayingScreen(
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() },
                                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                            )
                        }
                        composable("equalizer") {
                            EqualizerScreen(
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable("search") {
                            SearchScreen(
                                viewModel = viewModel,
                                onNavigateToPlayer = { navController.navigate("player") },
                                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                            )
                        }
                        composable("library") {
                            LibraryScreen(
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() },
                                onNavigateToPlayer = { navController.navigate("player") },
                                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                            )
                        }
                    }
                }

                // Glassmorphic User Profile Info Sheet / Dialog
                if (showProfileDialog) {
                    val favoritesCount = state.favorites.size
                    val playlistsCount = state.playlists.size
                    val activeEq = viewModel.eqPreset.collectAsState().value
                    
                    AlertDialog(
                        onDismissRequest = { showProfileDialog = false },
                        title = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(NeonPurple.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "D Family Music Listener",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().glassmorphic(12.dp).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Favorited Songs", color = TextSecondary, fontSize = 13.sp)
                                    Text("$favoritesCount songs", color = NeonCyanGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().glassmorphic(12.dp).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Custom Playlists", color = TextSecondary, fontSize = 13.sp)
                                    Text("$playlistsCount playlists", color = NeonCyanGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().glassmorphic(12.dp).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Active Equalizer", color = TextSecondary, fontSize = 13.sp)
                                    Text(activeEq, color = NeonCyanGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().glassmorphic(12.dp).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Premium Client", color = TextSecondary, fontSize = 13.sp)
                                    Text("Active (v1.1)", color = NeonCyanGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showProfileDialog = false }) {
                                Text("Close", color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        },
                        containerColor = DarkBgEnd.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            }
        }
    }
}

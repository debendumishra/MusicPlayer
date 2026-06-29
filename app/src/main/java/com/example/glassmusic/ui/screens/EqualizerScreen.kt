package com.example.glassmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.example.glassmusic.playback.PlaybackViewModel
import com.example.glassmusic.ui.components.VisualizerView
import com.example.glassmusic.ui.components.glassmorphic
import com.example.glassmusic.ui.components.neonGlow
import com.example.glassmusic.ui.theme.*

@UnstableApi
@Composable
fun EqualizerScreen(
    viewModel: PlaybackViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val eqBands by viewModel.eqBands.collectAsState()
    val activePreset by viewModel.eqPreset.collectAsState()

    val presets = listOf("Off", "Bass Booster", "Electronic", "Vocal", "Jazz")
    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "4kHz", "14kHz")

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

            // Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.width(20.dp))

                Text(
                    text = "EQUALIZER",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Wave Visualizer Feedback box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .glassmorphic(cornerRadius = 24.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Real-time Profile Output",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    VisualizerView(isPlaying = state.isPlaying, modifier = Modifier.fillMaxWidth().height(100.dp))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Preset Chip list selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PRESETS",
                    color = NeonPurpleGlow,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(presets) { preset ->
                        val isSelected = activePreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectPreset(preset) },
                            label = { Text(preset) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = GlassBgColor,
                                labelColor = TextSecondary,
                                selectedContainerColor = NeonPurple.copy(alpha = 0.25f),
                                selectedLabelColor = NeonCyanGlow
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = GlassBorderColor,
                                selectedBorderColor = NeonCyan,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Equalizer Bands sliders
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(cornerRadius = 24.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "FREQUENCY BANDS (dB)",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )

                // Render each slider
                eqBands.forEachIndexed { index, value ->
                    // Map value [0..100] to db gain [-12dB..+12dB]
                    val dbValue = ((value - 50) * 0.24f).toInt()
                    val sign = if (dbValue > 0) "+" else ""

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bandLabels[index],
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "$sign${dbValue} dB",
                                color = if (value != 50) NeonCyan else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        
                        Slider(
                            value = value.toFloat(),
                            onValueChange = { newValue ->
                                viewModel.updateEqBand(index, newValue.toInt())
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonPurple,
                                activeTrackColor = NeonPurple,
                                inactiveTrackColor = GlassBgColorLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Preset Custom note
            if (activePreset == "Custom") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .glassmorphic(cornerRadius = 16.dp, bgColor = Color(0x1F06B6D4))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "You are currently using custom band profiles. Select any preset above to reset.",
                        color = NeonCyanGlow,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

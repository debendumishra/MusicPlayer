package com.example.glassmusic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.glassmusic.ui.theme.NeonCyan
import com.example.glassmusic.ui.theme.NeonPurple
import kotlin.math.sin

@Composable
fun VisualizerView(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_anim")
    
    // Animate phase when playing, freeze when paused
    val phase by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_phase"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Scale wave amplitude dynamically depending on active playback
    val amplitudeScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.08f,
        animationSpec = tween(800),
        label = "wave_amplitude"
    )

    Canvas(
        modifier = modifier
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        val path1 = Path()
        val path2 = Path()
        val path3 = Path()
        
        val points = 80
        val step = width / points
        
        for (i in 0..points) {
            val x = i * step
            val normalizedX = i.toFloat() / points
            
            // Hump/window function so the wave naturally dampens to 0 at the left and right edges
            val window = sin(normalizedX * Math.PI.toFloat())
            
            // Primary wave: fast, tall neon cyan wave
            val y1 = centerY + sin(phase * 2f + normalizedX * 8f) * (height * 0.4f) * window * amplitudeScale
            
            // Secondary wave: slower, medium neon purple wave
            val y2 = centerY + sin(-phase * 1.5f + normalizedX * 12f + 1f) * (height * 0.3f) * window * amplitudeScale
            
            // Ambient wave: high frequency background wave
            val y3 = centerY + sin(phase * 3f - normalizedX * 6f + 2f) * (height * 0.2f) * window * amplitudeScale
            
            if (i == 0) {
                path1.moveTo(x, y1)
                path2.moveTo(x, y2)
                path3.moveTo(x, y3)
            } else {
                path1.lineTo(x, y1)
                path2.lineTo(x, y2)
                path3.lineTo(x, y3)
            }
        }
        
        // Draw continuous layered waves
        drawPath(
            path = path3,
            color = NeonPurple.copy(alpha = 0.25f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
        
        drawPath(
            path = path2,
            color = NeonPurple.copy(alpha = 0.65f),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
        
        drawPath(
            path = path1,
            color = NeonCyan,
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

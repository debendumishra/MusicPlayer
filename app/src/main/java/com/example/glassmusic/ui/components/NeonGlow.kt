package com.example.glassmusic.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Extension modifier that draws a soft, high-fidelity neon color glow shadow behind
 * components like action buttons, play buttons, and album artwork cards.
 */
fun Modifier.neonGlow(
    color: Color,
    radius: Dp = 16.dp,
    alpha: Float = 0.5f,
    borderRadius: Dp = 0.dp
): Modifier = this.drawBehind {
    val glowColor = color.copy(alpha = alpha).toArgb()
    
    // Create a native Paint which supports shadow layers for glowing visual effects
    val paint = Paint().asFrameworkPaint().apply {
        this.color = android.graphics.Color.TRANSPARENT
        this.setShadowLayer(
            radius.toPx(),
            0f,
            0f,
            glowColor
        )
    }
    
    drawIntoCanvas { canvas ->
        if (borderRadius > 0.dp) {
            canvas.nativeCanvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                borderRadius.toPx(),
                borderRadius.toPx(),
                paint
            )
        } else {
            // Draw circle if aspect ratio is 1:1, otherwise rectangle
            if (size.width == size.height) {
                val center = size.width / 2f
                canvas.nativeCanvas.drawCircle(center, center, center, paint)
            } else {
                canvas.nativeCanvas.drawRect(
                    0f,
                    0f,
                    size.width,
                    size.height,
                    paint
                )
            }
        }
    }
}

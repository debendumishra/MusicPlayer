package com.example.glassmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.glassmusic.ui.theme.GlassBgColor
import com.example.glassmusic.ui.theme.GlassBorderColor
import com.example.glassmusic.ui.theme.GlassBorderColorEnd

/**
 * Custom glassmorphic modifier that sets up semi-transparent background,
 * and border gradient highlight.
 */
fun Modifier.glassmorphic(
    cornerRadius: Dp = 24.dp,
    bgColor: Color = GlassBgColor,
    borderBrush: Brush = Brush.linearGradient(
        colors = listOf(GlassBorderColor, GlassBorderColorEnd)
    )
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(bgColor)
    .border(0.75.dp, borderBrush, RoundedCornerShape(cornerRadius))

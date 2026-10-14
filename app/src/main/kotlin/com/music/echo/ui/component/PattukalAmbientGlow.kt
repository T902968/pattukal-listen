package com.music.echo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Lightweight, static ambient glass lighting.
 *
 * Deliberately avoids infinite transitions, animated gradients and rotation so
 * opening screens do not continuously invalidate/recompose this full-screen layer.
 */
@Composable
fun PattukalAmbientGlow(
    modifier: Modifier = Modifier,
    glowColor: Color = Color(0xFFFF4D6D),
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.10f),
                            glowColor.copy(alpha = 0.035f),
                            Color.Transparent
                        ),
                        radius = 720f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.018f),
                            Color.White.copy(alpha = 0.028f),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}

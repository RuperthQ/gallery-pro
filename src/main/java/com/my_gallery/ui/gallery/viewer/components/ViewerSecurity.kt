package com.my_gallery.ui.gallery.viewer.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Escudo Anti-Flicker para mayor seguridad en la Bóveda.
 */
@Composable
fun FlickerShield() {
    val infiniteTransition = rememberInfiniteTransition(label = "flicker")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(16, easing = LinearEasing), // ~60Hz flicker
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha))
    )
}

/**
 * Filtro de privacidad (líneas verticales) para dificultar capturas/fotos externas.
 */
@Composable
fun PrivacyFilter() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 4.dp.toPx()
        for (x in 0..size.width.toInt() step step.toInt()) {
            drawLine(
                color = Color.Black.copy(alpha = 0.05f),
                start = Offset(x.toFloat(), 0f),
                end = Offset(x.toFloat(), size.height),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

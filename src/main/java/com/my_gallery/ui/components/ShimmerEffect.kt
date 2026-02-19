package com.my_gallery.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    
    val translateAnim = transition.animateFloat(
        initialValue = -500f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "shimmerX"
    )

    val shimmerColors = listOf(
        Color(0xFFB8B8B8).copy(alpha = 0.2f),
        Color(0xFFDDDDDD).copy(alpha = 0.5f),
        Color(0xFFB8B8B8).copy(alpha = 0.2f),
    )

    return this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim.value, 0f),
            end = Offset(translateAnim.value + 300f, 300f)
        )
    )
}

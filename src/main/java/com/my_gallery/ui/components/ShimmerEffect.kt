package com.my_gallery.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.my_gallery.ui.theme.GalleryDesign

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    
    val translateAnim = transition.animateFloat(
        initialValue = GalleryDesign.ShimmerXStart,
        targetValue = GalleryDesign.ShimmerXEnd,
        animationSpec = infiniteRepeatable(
            animation = tween(GalleryDesign.ShimmerAnimDuration, easing = LinearEasing)
        ),
        label = "shimmerX"
    )

    val colorBase = MaterialTheme.colorScheme.surfaceVariant
    val shimmerColors = listOf(
        colorBase.copy(alpha = GalleryDesign.AlphaDisable),
        colorBase.copy(alpha = GalleryDesign.AlphaOverlay),
        colorBase.copy(alpha = GalleryDesign.AlphaDisable),
    )

    return this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim.value, 0f),
            end = Offset(translateAnim.value + GalleryDesign.ShimmerWidth, GalleryDesign.ShimmerWidth)
        )
    )
}

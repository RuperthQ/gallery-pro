package com.my_gallery.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sistema de Diseño Premium para la Galeria Pro.
 * Centraliza todos los valores de diseño (Tokens) para evitar hardcoding.
 */
object GalleryDesign {

    // --- Dimensiones (Spacing & Sizes) ---
    val PaddingLarge = 16.dp
    val PaddingMedium = 10.dp
    val PaddingSmall = 8.dp
    val PaddingTiny = 4.dp
    
    val IconSizeSmall = 20.dp
    val IconSizeNormal = 24.dp
    val IconSizeLarge = 28.dp
    
    val BorderWidthThin = 1.dp
    val BorderWidthNone = 0.dp
    
    val ThumbImageSize = 400 // Int para Coil
    val HeaderVerticalSpacing = 4.dp // Espacio interno compacto de la fila superior
    val ButtonHeight = 38.dp // Altura fija para consistencia de pills
    val BorderWidthBold = 2.dp // Borde solicitado

    // --- Formas (Shapes) ---
    val CornerRadiusLarge = 20.dp
    val CornerRadiusMedium = 12.dp
    val CornerRadiusSmall = 6.dp

    val CardShape = RoundedCornerShape(CornerRadiusMedium)
    val FilterShape = RoundedCornerShape(CornerRadiusLarge)
    val HeaderShape = RoundedCornerShape(bottomStart = CornerRadiusLarge, bottomEnd = CornerRadiusLarge)
    val HeaderFullShape = RoundedCornerShape(
        topStart = 0.dp, 
        topEnd = 0.dp, 
        bottomStart = CornerRadiusLarge, 
        bottomEnd = CornerRadiusLarge
    )
    val OverlayShape = RoundedCornerShape(CornerRadiusSmall)

    // --- Efectos Visuales (Alphas & Blur) ---
    val AlphaGlassHigh = 0.9f // Más denso para que se note el efecto cristal
    val AlphaGlassLow = 0.75f
    val AlphaBorderDefault = 0.4f // Más visible para el borde de 2dp
    val AlphaBorderLight = 0.15f
    val AlphaOverlay = 0.4f
    val AlphaSecondary = 0.05f
    
    val BlurRadius = 16.dp 

    // --- Viewer Specific (Tokens) ---
    val ViewerAnimNormal = 300
    val ViewerAnimFast = 200
    val ViewerAnimSlow = 400
    val ViewerAnimBorder = 2000
    
    val ViewerHeaderSafetyPadding = 56.dp
    val ViewerTitlePaddingH = 16.dp
    val ViewerTitlePaddingV = 8.dp
    
    val ViewerCarouselHeight = 82.dp
    val ViewerThumbSize = 72.dp
    val ViewerThumbSpacing = 8.dp
    val ViewerThumbScaleSelected = 1.1f
    val ViewerThumbScaleNormal = 0.9f
    
    val ViewerBorderWidth = 6f
    val ViewerBorderDash = 120f
    val ViewerBorderGap = 100f
    val ViewerBorderAlphaBase = 0.15f
    
    val ViewerScaleTransition = 0.92f
    val ViewerScaleOverlay = 0.95f
    val ViewerScalePagerDown = 0.88f
    @Composable
    fun primaryGradient() = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )

    // --- Modificadores Premium ---
    
    fun Modifier.glassBackground(
        alpha1: Float = AlphaGlassHigh,
        alpha2: Float = AlphaGlassLow
    ) = composed {
        this.then(
            Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = alpha1),
                        MaterialTheme.colorScheme.surface.copy(alpha = alpha2)
                    )
                )
            )
        )
    }

    fun Modifier.premiumBorder(
        width: Dp = BorderWidthThin,
        shape: RoundedCornerShape = CardShape,
        alpha: Float = AlphaBorderDefault
    ) = composed {
        this.then(
            Modifier.border(
                width = width,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha + 0.2f),
                        MaterialTheme.colorScheme.primary.copy(alpha = AlphaSecondary),
                        MaterialTheme.colorScheme.secondary.copy(alpha = alpha)
                    )
                ),
                shape = shape
            )
        )
    }
}

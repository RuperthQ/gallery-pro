package com.my_gallery.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val PaddingCarouselHorizontal = 12.dp
    val OffsetCarouselBase = 7.dp
    
    val PaddingViewerLockH = 64.dp
    val PaddingViewerLockV = 12.dp
    
    val IconSizeExtraSmall = 16.dp
    val IconSizeSmall = 20.dp
    val IconSizeNormal = 24.dp
    val IconSizeLarge = 28.dp
    val IconSizeAction = 40.dp
    
    val MenuItemHeight = 50.dp
    val DragIndicatorWidth = 36.dp
    val DragIndicatorHeight = 4.dp
    
    val BorderWidthThin = 1.dp
    val BorderWidthNone = 0.dp
    
    val ThumbImageSize = 400 // Int para Coil
    val HeaderVerticalSpacing = 4.dp // Espacio interno compacto de la fila superior
    val ButtonHeight = 38.dp // Altura fija para consistencia de pills
    val BorderWidthBold = 2.dp // Borde solicitado
    
    val CarouselItemWidth = 74.dp
    val CarouselImageSize = 68.dp
    val CarouselImagePadding = 3.dp
    
    val LetterSpacingSmall = 0.4.sp

    // --- Formas (Shapes) ---
    val CornerRadiusLarge = 20.dp
    val CornerRadiusExtraLarge = 24.dp
    val CornerRadiusMedium = 12.dp
    val CornerRadiusSmall = 6.dp

    val CardShape = RoundedCornerShape(CornerRadiusMedium)
    val FilterShape = RoundedCornerShape(CornerRadiusLarge)
    val HeaderShape = RoundedCornerShape(CornerRadiusLarge)
    val HeaderFullShape = RoundedCornerShape(
        topStart = BorderWidthNone,
        topEnd = BorderWidthNone,
        bottomStart = CornerRadiusLarge,
        bottomEnd = CornerRadiusLarge
    )
    val OverlayShape = RoundedCornerShape(CornerRadiusSmall)
    val BottomSheetShape = RoundedCornerShape(topStart = CornerRadiusExtraLarge, topEnd = CornerRadiusExtraLarge)

    // --- Efectos Visuales (Alphas & Blur) ---
    val AlphaGlassHigh = 0.9f // Más denso para que se note el efecto cristal
    val AlphaGlassLow = 0.75f
    val AlphaBorderDefault = 0.4f // Más visible para el borde de 2dp
    val AlphaBorderLight = 0.15f
    val AlphaOverlay = 0.4f
    val AlphaSecondary = 0.05f
    val AlphaDisable = 0.2f
    
    val BlurRadius = 16.dp 
    val ElevationSmall = 2.dp

    // --- Viewer Specific (Tokens) ---
    val ViewerAnimNormal = 300
    val ViewerAnimFast = 200
    val ViewerAnimSlow = 400
    val ViewerAnimBorder = 2000
    val ViewerAnimLong = 3000
    val ShimmerAnimDuration = 1000
    
    val ShimmerXStart = -500f
    val ShimmerXEnd = 1000f
    val ShimmerWidth = 300f
    
    val ViewerHeaderSafetyPadding = 56.dp
    val ViewerTitlePaddingH = 16.dp
    val ViewerTitlePaddingV = 12.dp
    
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
    val ViewerScaleMax = 3f
    val ViewerScaleLimit = 5f
    
    val ScaleCarouselSelected = 1.05f
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
        val isDark = isSystemInDarkTheme()
        val finalAlpha1 = if (!isDark && alpha1 == AlphaGlassHigh) 1.0f else alpha1
        val finalAlpha2 = if (!isDark && alpha2 == AlphaGlassLow) 0.98f else alpha2
        
        this.then(
            Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = finalAlpha1),
                        MaterialTheme.colorScheme.surface.copy(alpha = finalAlpha2)
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
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    )
                ),
                shape = shape
            )
        )
    }

    fun Modifier.bottomPremiumBorder(
        width: Dp = BorderWidthThin,
        cornerRadius: Dp = CornerRadiusLarge,
        alpha: Float = AlphaBorderDefault
    ) = composed {
        val colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = alpha + 0.2f),
            MaterialTheme.colorScheme.primary.copy(alpha = AlphaSecondary),
            MaterialTheme.colorScheme.primary.copy(alpha = alpha + 0.2f)
        )
        this.then(
            Modifier.drawWithCache {
                val strokeWidthPx = width.toPx()
                val cornerRadiusPx = cornerRadius.toPx()
                val widthPx = size.width
                val heightPx = size.height
                val inset = strokeWidthPx / 2f
                
                val brush = Brush.horizontalGradient(
                    colors = colors,
                    startX = 0f,
                    endX = widthPx
                )
                
                val path = Path().apply {
                    moveTo(inset, 0f)
                    lineTo(inset, heightPx - cornerRadiusPx)
                    if (cornerRadiusPx > 0f) {
                        arcTo(
                            rect = Rect(inset, heightPx - 2 * cornerRadiusPx + inset, 2 * cornerRadiusPx - inset, heightPx - inset),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = -90f,
                            forceMoveTo = false
                        )
                    }
                    lineTo(widthPx - cornerRadiusPx, heightPx - inset)
                    if (cornerRadiusPx > 0f) {
                        arcTo(
                            rect = Rect(widthPx - 2 * cornerRadiusPx + inset, heightPx - 2 * cornerRadiusPx + inset, widthPx - inset, heightPx - inset),
                            startAngleDegrees = 90f,
                            sweepAngleDegrees = -90f,
                            forceMoveTo = false
                        )
                    }
                    lineTo(widthPx - inset, 0f)
                }
                
                onDrawWithContent {
                    drawContent()
                    drawPath(
                        path = path,
                        brush = brush,
                        style = Stroke(width = strokeWidthPx)
                    )
                }
            }
        )
    }
}

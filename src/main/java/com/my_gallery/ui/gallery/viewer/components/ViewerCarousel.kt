package com.my_gallery.ui.gallery.viewer.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import com.my_gallery.ui.gallery.GalleryUiModel
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

@Composable
fun ViewerCarousel(
    items: LazyPagingItems<GalleryUiModel>,
    currentPage: Int,
    listState: LazyListState,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = GalleryDesign.PaddingMedium),
        modifier = modifier
            .fillMaxWidth()
            .height(GalleryDesign.ViewerCarouselHeight),
        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.ViewerThumbSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(items.itemCount) { idx ->
            val thumbModel = items[idx]
            if (thumbModel is GalleryUiModel.Media) {
                val isSelected = idx == currentPage
                
                // Animación de escala reactiva
                val thumbScale by animateFloatAsState(
                    targetValue = if (isSelected) GalleryDesign.ViewerThumbScaleSelected else GalleryDesign.ViewerThumbScaleNormal,
                    animationSpec = tween(GalleryDesign.ViewerAnimNormal, easing = FastOutSlowInEasing),
                    label = "thumbScale"
                )

                Box(
                    modifier = Modifier
                        .size(GalleryDesign.ViewerThumbSize)
                        .graphicsLayer {
                            scaleX = thumbScale
                            scaleY = thumbScale
                        }
                        .clip(GalleryDesign.CardShape)
                        .then(
                            if (isSelected) Modifier.rotatingPremiumBorder(GalleryDesign.CardShape)
                            else Modifier.premiumBorder(shape = GalleryDesign.CardShape)
                        )
                        .clickable { onItemSelected(idx) }
                ) {
                    AsyncImage(
                        model = thumbModel.item.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                            .alpha(if (isSelected) 1f else 0.5f)
                            .graphicsLayer {
                                rotationZ = thumbModel.item.rotation
                            }
                    )

                    if (thumbModel.item.mimeType.startsWith("video/")) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = GalleryDesign.AlphaGlassLow),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(GalleryDesign.IconSizeNormal)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modificador para un borde premium rotativo (Efecto "Snake Trace" matemático)
 */
fun Modifier.rotatingPremiumBorder(
    shape: Shape,
    borderWidth: Float = GalleryDesign.ViewerBorderWidth
) = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "borderRotation")
    
    val dashLength = GalleryDesign.ViewerBorderDash
    val gapLength = GalleryDesign.ViewerBorderGap
    val patternLength = dashLength + gapLength

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = patternLength, 
        animationSpec = infiniteRepeatable(
            animation = tween(GalleryDesign.ViewerAnimBorder, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorSecondary = MaterialTheme.colorScheme.secondary

    this.drawWithContent {
        drawContent()
        
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path().apply { addOutline(outline) }
        
        drawOutline(
            outline = outline,
            color = colorPrimary.copy(alpha = GalleryDesign.ViewerBorderAlphaBase),
            style = Stroke(width = GalleryDesign.BorderWidthThin.toPx())
        )
        
        drawPath(
            path = path,
            brush = Brush.sweepGradient(
                colors = listOf(colorPrimary, colorSecondary, colorPrimary),
                center = center
            ),
            style = Stroke(
                width = borderWidth,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(dashLength, gapLength),
                    phase = -phase 
                ),
                cap = StrokeCap.Round
            )
        )
    }
}

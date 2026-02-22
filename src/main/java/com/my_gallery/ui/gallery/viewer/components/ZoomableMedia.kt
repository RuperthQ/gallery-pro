package com.my_gallery.ui.gallery.viewer.components

import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.my_gallery.domain.model.MediaItem
import com.my_gallery.ui.theme.GalleryDesign
import kotlin.math.abs
import kotlin.math.min

@Composable
fun ZoomableMedia(
    item: MediaItem,
    rotation: Float = 0f,
    onScaleChange: (Float) -> Unit,
    onRotate: () -> Unit = {},
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var imageIntrinsicSize by remember { mutableStateOf(Size.Zero) }
    
    LaunchedEffect(scale) {
        onScaleChange(scale)
    }

    val isRotated = (rotation % 180f) != 0f
    val rotationScale = remember(rotation, viewportSize, imageIntrinsicSize) {
        if (viewportSize == IntSize.Zero || imageIntrinsicSize == Size.Zero || !isRotated) 1f
        else {
            val containerW = viewportSize.width.toFloat()
            val containerH = viewportSize.height.toFloat()
            val imgW = imageIntrinsicSize.width
            val imgH = imageIntrinsicSize.height
            val scaleFitOrig = min(containerW / imgW, containerH / imgH)
            val scaleFitRotated = min(containerW / imgH, containerH / imgW)
            scaleFitRotated / scaleFitOrig
        }
    }

    fun calculateBoundOffset(newOffset: Offset, currentScale: Float): Offset {
        val totalScale = currentScale * rotationScale
        if (totalScale <= 1f || viewportSize == IntSize.Zero || imageIntrinsicSize == Size.Zero) return Offset.Zero
        
        val imgW = if (isRotated) imageIntrinsicSize.height else imageIntrinsicSize.width
        val imgH = if (isRotated) imageIntrinsicSize.width else imageIntrinsicSize.height
        
        val ratio = min(viewportSize.width.toFloat() / imgW, viewportSize.height.toFloat() / imgH)
        val fittedWidth = imgW * ratio
        val fittedHeight = imgH * ratio
        
        val maxX = maxOf(0f, (fittedWidth * currentScale - viewportSize.width) / 2f)
        val maxY = maxOf(0f, (fittedHeight * currentScale - viewportSize.height) / 2f)
        return Offset(newOffset.x.coerceIn(-maxX, maxX), newOffset.y.coerceIn(-maxY, maxY))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = GalleryDesign.ViewerScaleMax
                            offset = Offset.Zero 
                        }
                        onScaleChange(scale)
                    }
                )
            }
            .pointerInput(scale) {
                awaitEachGesture {
                    var zoom = 1f
                    var pan = Offset.Zero
                    var rotationSum = 0f
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop

                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val rotationChange = event.calculateRotation()

                            if (!pastTouchSlop) {
                                zoom *= zoomChange
                                pan += panChange
                                rotationSum += rotationChange
                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                val zoomMotion = abs(1 - zoom) * centroidSize
                                val panMotion = pan.getDistance()
                                val rotationMotion = abs(rotationSum)

                                if (zoomMotion > touchSlop || panMotion > touchSlop || rotationMotion > 10f) {
                                    pastTouchSlop = true
                                }
                            }

                            if (pastTouchSlop) {
                                if (abs(rotationSum) > 35f) {
                                    onRotate() 
                                    rotationSum = 0f 
                                }

                                if (scale > 1.01f || zoomChange > 1.01f) {
                                    val newScale = (scale * zoomChange).coerceIn(1f, GalleryDesign.ViewerScaleLimit)
                                    val candidateOffset = offset + panChange
                                    
                                    scale = newScale
                                    offset = calculateBoundOffset(candidateOffset, scale)
                                    onScaleChange(scale)

                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed })
                    
                    if (scale <= 1.01f) {
                        scale = 1f
                        offset = Offset.Zero
                        onScaleChange(1f)
                    }
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.url)
                .setParameter("is_full_res", true)
                .crossfade(GalleryDesign.ViewerAnimNormal)
                .build(),
            contentDescription = null,
            onState = { if (it is AsyncImagePainter.State.Success) imageIntrinsicSize = it.painter?.intrinsicSize ?: Size.Zero },
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val finalScale = scale * rotationScale
                scaleX = finalScale
                scaleY = finalScale
                translationX = offset.x
                translationY = offset.y
                rotationZ = rotation
            },
            contentScale = ContentScale.Fit
        )
    }
}

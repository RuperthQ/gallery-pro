package com.my_gallery.ui.gallery.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.my_gallery.ui.theme.GalleryDesign
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Composable
fun VerticalScrollbar(
    gridState: LazyGridState,
    totalItems: Int,
    modifier: Modifier = Modifier
) {
    if (totalItems <= 0) return

    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }

    // Calcular visibilidad (mostrar al hacer scroll o arrastrar)
    val alpha by animateFloatAsState(
        targetValue = if (isDragging || gridState.isScrollInProgress) 1f else 0f,
        animationSpec = tween(if (isDragging || gridState.isScrollInProgress) 0 else 500),
        label = "scrollbarAlpha"
    )

    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        val maxHeightPx = constraints.maxHeight.toFloat()
        
        // Calcular posición del thumb
        val firstVisibleIndex = gridState.firstVisibleItemIndex
        val visibleItemsCount = gridState.layoutInfo.visibleItemsInfo.size
        
        if (visibleItemsCount >= totalItems) return@BoxWithConstraints

        val scrollThumbHeight = max(maxHeightPx * (visibleItemsCount.toFloat() / totalItems), 100f)
        val scrollThumbOffset = (maxHeightPx - scrollThumbHeight) * (firstVisibleIndex.toFloat() / (totalItems - visibleItemsCount))

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = (scrollThumbOffset / maxHeightPx * this.maxHeight.value).dp)
                .padding(end = 4.dp, top = 2.dp, bottom = 2.dp)
                .width(6.dp)
                .height((scrollThumbHeight / maxHeightPx * this.maxHeight.value).dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                .pointerInput(totalItems) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (scrollThumbOffset + dragAmount.y).coerceIn(0f, maxHeightPx - scrollThumbHeight)
                            val targetIndex = (newOffset / (maxHeightPx - scrollThumbHeight) * (totalItems - visibleItemsCount)).toInt()
                            coroutineScope.launch {
                                gridState.scrollToItem(targetIndex)
                            }
                        }
                    )
                }
        )
    }
}

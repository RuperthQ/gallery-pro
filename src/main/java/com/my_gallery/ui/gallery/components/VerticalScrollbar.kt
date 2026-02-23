package com.my_gallery.ui.gallery.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.my_gallery.ui.gallery.MenuStyle
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun VerticalScrollbar(
    gridState: LazyGridState,
    menuStyle: MenuStyle,
    modifier: Modifier = Modifier
) {
    val layoutInfo = gridState.layoutInfo
    val totalItemsCount = layoutInfo.totalItemsCount
    if (totalItemsCount <= 0) return

    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return

    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }

    // Calcular visibilidad
    val alpha by animateFloatAsState(
        targetValue = if (isDragging || gridState.isScrollInProgress) 1f else 0f,
        animationSpec = tween(if (isDragging || gridState.isScrollInProgress) 0 else 500),
        label = "scrollbarAlpha"
    )

    // Ajustamos el track para que coincida exactamente con el área de fotos
    val topPadding = if (menuStyle == MenuStyle.BOTTOM_FLOATING) 38.dp else 102.dp // Sincronizado con el Grid
    val bottomPadding = if (menuStyle == MenuStyle.BOTTOM_FLOATING) 126.dp else 76.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .padding(top = topPadding, bottom = bottomPadding)
    ) {
        val maxHeightPx = constraints.maxHeight.toFloat()
        val maxHeightDp = this.maxHeight
        
        // Cálculo de posición suave (incluyendo offset del primer item)
        val firstItem = visibleItems.first()
        val visibleCount = visibleItems.size
        
        val scrollPercent = if (totalItemsCount > visibleCount) {
            val index = firstItem.index
            val offset = -firstItem.offset.y.toFloat()
            val height = firstItem.size.height.toFloat()
            val partialProgress = if (height > 0) offset / height else 0f
            
            // Corregido: Dividir por el rango desplazable real (total - visibles) 
            // para que el 100% coincida con el final del track.
            (index.toFloat() + partialProgress) / (totalItemsCount - visibleCount).coerceAtLeast(1)
        } else 0f

        // Logica: mínimo 200px de altura para que se vea largo siempre
        val scrollThumbHeight = max(maxHeightPx * (visibleCount.toFloat() / totalItemsCount), 160f)
        val scrollThumbHeightDp = (scrollThumbHeight / maxHeightPx * maxHeightDp.value).dp
        val currentScrollOffset = (maxHeightPx - scrollThumbHeight) * scrollPercent
        
        // Usamos el dragOffset si estamos arrastrando para respuesta 1:1, si no el calculado por el grid
        val visualOffset = if (isDragging) dragOffset else currentScrollOffset

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = (visualOffset / maxHeightPx * maxHeightDp.value).dp)
                .padding(end = 12.dp) // Más adentro
                .width(6.dp)
                .height(scrollThumbHeightDp)
                .alpha(alpha)
                .zIndex(1f)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                        )
                    )
                )
                .pointerInput(totalItemsCount) {
                    detectDragGestures(
                        onDragStart = { 
                            isDragging = true
                            dragOffset = currentScrollOffset 
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Actualizamos el dragOffset inmediatamente para el feedback visual 1:1
                            dragOffset = (dragOffset + dragAmount.y).coerceIn(0f, maxHeightPx - scrollThumbHeight)
                            
                            // Mapeamos el offset del thumb al índice del grid
                            val targetPercent = dragOffset / (maxHeightPx - scrollThumbHeight)
                            val targetIndex = (targetPercent * (totalItemsCount - visibleCount)).toInt()
                            
                            coroutineScope.launch {
                                // Usamos scrollToItem para saltar rápidamente a la posición
                                gridState.scrollToItem(max(0, targetIndex))
                            }
                        }
                    )
                }
        )
    }
}

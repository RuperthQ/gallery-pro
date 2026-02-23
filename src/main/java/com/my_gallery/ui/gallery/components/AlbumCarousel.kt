package com.my_gallery.ui.gallery.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.my_gallery.ui.components.PremiumAlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.my_gallery.domain.model.AlbumItem
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder


@Composable
fun PremiumAlbumCarousel(
    albums: List<AlbumItem>,
    selectedAlbumId: String?,
    modifier: Modifier = Modifier,
    lockedAlbums: Set<String> = emptySet(),
    onAlbumClick: (AlbumItem) -> Unit,
    onAlbumLongClick: (AlbumItem) -> Unit = {},
    onDeleteAlbum: ((AlbumItem) -> Unit)? = null
) {
    val density = LocalDensity.current
    
    // El Carousel se expande lo justo para que el zoom sutil (1.05x) no se corte.
    // Usamos un offset menor para que no se sienta que se sale de la pantalla.
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall),
        contentPadding = PaddingValues(horizontal = GalleryDesign.PaddingCarouselHorizontal), // Suficiente para zoom 1.05x
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                // Alineación Pro: Queremos el borde del círculo a 8dp del borde de la pantalla.
                // 1. Padding 12dp + margen interno item 3dp (74w - 68circle / 2) = 15dp.
                // 2. Queremos 8dp. Desplazamiento = 8dp - 15dp = -7dp.
                val offsetPx = with(density) { GalleryDesign.OffsetCarouselBase.roundToPx() }
                val extendedWidth = constraints.maxWidth + (offsetPx * 2)
                val placeable = measurable.measure(constraints.copy(maxWidth = extendedWidth))
                
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(-offsetPx, 0)
                }
            }
            .graphicsLayer(clip = false)
            .padding(vertical = GalleryDesign.PaddingSmall)
    ) {
        items(albums) { album ->
            AlbumCircleItem(
                album = album,
                isSelected = (album.id == "ALL_VIRTUAL_ALBUM" && selectedAlbumId == null) || (album.id == selectedAlbumId),
                isLocked = lockedAlbums.contains(album.id),
                onClick = { onAlbumClick(album) },
                onLongClick = { onAlbumLongClick(album) },
                onDeleteAlbum = onDeleteAlbum
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumCircleItem(
    album: AlbumItem,
    isSelected: Boolean,
    isLocked: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDeleteAlbum: ((AlbumItem) -> Unit)? = null
) {
    val scale by animateFloatAsState(if (isSelected) GalleryDesign.ScaleCarouselSelected else 1f, label = "scale")

    // Álbumes virtuales no se pueden eliminar
    val virtualAlbumIds = setOf("ALL_VIRTUAL_ALBUM", "FAVORITES_VIRTUAL_ALBUM", "SECURE_VAULT", "TRASH_VIRTUAL_ALBUM")
    val isDeletable = album.id !in virtualAlbumIds && onDeleteAlbum != null

    // --- ESTADO DEL GESTO ---
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val DRAG_THRESHOLD = 40f     // px a partir del cual se considera "abierto"
    val MAX_DRAG = 70f           // desplazamiento máximo en px
    val trashRevealed by remember(dragOffsetY) { derivedStateOf { dragOffsetY > DRAG_THRESHOLD } }

    val animatedDrag by animateFloatAsState(
        targetValue = dragOffsetY.coerceIn(0f, MAX_DRAG),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "drag"
    )

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        PremiumAlertDialog(
            onDismissRequest = { showDeleteDialog = false; dragOffsetY = 0f },
            title = "Eliminar álbum",
            titleColor = MaterialTheme.colorScheme.error,
            text = {
                Text(
                    "¿Eliminar el álbum \"${album.name}\"?\n\nLos archivos no se eliminarán — solo la carpeta.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    dragOffsetY = 0f
                    onDeleteAlbum?.invoke(album)
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; dragOffsetY = 0f }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(GalleryDesign.CarouselItemWidth)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier.size(GalleryDesign.CarouselImageSize)
        ) {
            // --- CÍRCULO DEL ÁLBUM (estático, no se desplaza) ---
            Box(
                modifier = Modifier
                    .size(GalleryDesign.CarouselImageSize)
                    .then(
                        if (isDeletable) {
                            Modifier.pointerInput(isDeletable) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    var isDragging = false

                                    do {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull() ?: break
                                        val delta = change.position - change.previousPosition
                                        val dy = delta.y

                                        if (!isDragging && kotlin.math.abs(dy) > 8f) {
                                            isDragging = if (kotlin.math.abs(dy) > kotlin.math.abs(delta.x) * 1.5f) {
                                                true
                                            } else break // horizontal → dejar al LazyRow
                                        }

                                        if (isDragging) {
                                            change.consume()
                                            dragOffsetY = (dragOffsetY + dy).coerceIn(0f, MAX_DRAG)
                                        }
                                    } while (event.changes.any { it.pressed })

                                    if (isDragging) {
                                        dragOffsetY = if (dragOffsetY > DRAG_THRESHOLD) MAX_DRAG else 0f
                                    }
                                }
                            }
                        } else Modifier
                    )
                    .premiumBorder(
                        shape = CircleShape,
                        width = if (isSelected) GalleryDesign.BorderWidthBold else GalleryDesign.BorderWidthThin,
                        alpha = if (isSelected) 1f else 0.4f
                    )
                    .padding(GalleryDesign.CarouselImagePadding)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = {
                            if (trashRevealed && isDeletable) {
                                showDeleteDialog = true
                            } else {
                                dragOffsetY = 0f
                                onClick()
                            }
                        },
                        onLongClick = {
                            dragOffsetY = 0f
                            onLongClick()
                        },
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    )

            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(album.thumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isLocked) Modifier.blur(GalleryDesign.BlurRadius) else Modifier)
                        .graphicsLayer {
                            rotationZ = album.rotation
                        }
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                }

                // --- OVERLAY TRASH ---
                if (isDeletable && animatedDrag > 0f) {
                    val overlayAlpha = (animatedDrag / MAX_DRAG).coerceIn(0f, 0.55f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFD32F2F).copy(alpha = overlayAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = (animatedDrag / MAX_DRAG).coerceIn(0f, 1f)),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Álbum bloqueado",
                            tint = Color.White,
                            modifier = Modifier.size(GalleryDesign.IconSizeSmall)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(GalleryDesign.PaddingTiny))
        Text(
            text = if (trashRevealed && isDeletable) "Eliminar" else album.name,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = GalleryDesign.LetterSpacingSmall
            ),
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = if (trashRevealed && isDeletable)
                MaterialTheme.colorScheme.error
            else if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontWeight = if (isSelected || trashRevealed) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}


package com.my_gallery.ui.gallery.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.my_gallery.domain.model.AlbumItem
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

@Composable
fun PremiumAlbumCarousel(
    albums: List<AlbumItem>,
    selectedAlbumId: String?,
    onAlbumClick: (AlbumItem) -> Unit
) {
    val density = LocalDensity.current
    
    // El Carousel se expande lo justo para que el zoom sutil (1.05x) no se corte.
    // Usamos un offset menor para que no se sienta que se sale de la pantalla.
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall),
        contentPadding = PaddingValues(horizontal = 12.dp), // Suficiente para zoom 1.05x
        modifier = Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                // Alineación Pro: Queremos el borde del círculo a 8dp del borde de la pantalla.
                // 1. Padding 12dp + margen interno item 3dp (74w - 68circle / 2) = 15dp.
                // 2. Queremos 8dp. Desplazamiento = 8dp - 15dp = -7dp.
                val offsetPx = with(density) { 7.dp.roundToPx() }
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
                onClick = { onAlbumClick(album) }
            )
        }
    }
}

@Composable
fun AlbumCircleItem(
    album: AlbumItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f, label = "scale")
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(74.dp) // Más compacto en X
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .premiumBorder(
                    shape = CircleShape,
                    width = if (isSelected) 2.dp else 1.dp,
                    alpha = if (isSelected) 1f else 0.4f
                )
                .padding(3.dp)
                .clip(CircleShape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                )
            }
        }
        
        Spacer(modifier = Modifier.height(GalleryDesign.PaddingTiny))
        Text(
            text = album.name,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.4.sp
            ),
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}

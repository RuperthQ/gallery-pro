package com.my_gallery.ui.gallery.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.my_gallery.domain.model.MediaItem
import com.my_gallery.ui.components.shimmerEffect
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

@Composable
fun SectionHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = GalleryDesign.PaddingSmall,
                bottom = GalleryDesign.PaddingSmall,
                start = GalleryDesign.PaddingSmall
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryItem(
    item: MediaItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var isLoaded by remember { mutableStateOf(false) }

    val imageRequest = remember(item.thumbnail) {
        ImageRequest.Builder(context)
            .data(item.thumbnail)
            .size(GalleryDesign.ThumbImageSize, GalleryDesign.ThumbImageSize)
            .precision(coil.size.Precision.INEXACT)
            .crossfade(true)
            .allowHardware(true)
            .build()
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(GalleryDesign.CardShape)
            .premiumBorder(shape = GalleryDesign.CardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = GalleryDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = GalleryDesign.ElevationSmall)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isLoaded) {
                Box(Modifier.fillMaxSize().shimmerEffect())
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    if (state is coil.compose.AsyncImagePainter.State.Success) {
                        isLoaded = true
                    }
                }
            )

            if (item.mimeType.startsWith("video")) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(GalleryDesign.PaddingSmall)
                        .size(GalleryDesign.IconSizeLarge)
                        .background(
                            Color.Black.copy(alpha = GalleryDesign.AlphaOverlay),
                            GalleryDesign.OverlayShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Video",
                        modifier = Modifier.size(GalleryDesign.IconSizeSmall),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryPlaceholder() {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(GalleryDesign.CardShape)
            .shimmerEffect()
    )
}

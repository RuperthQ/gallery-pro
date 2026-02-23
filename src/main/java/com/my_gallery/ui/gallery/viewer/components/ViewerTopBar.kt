package com.my_gallery.ui.gallery.viewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.my_gallery.ui.theme.GalleryDesign

@Composable
fun ViewerTopBar(
    title: String,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClose: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(GalleryDesign.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallFloatingActionButton(
            onClick = onClose,
            containerColor = Color.Black.copy(alpha = GalleryDesign.AlphaOverlay),
            contentColor = Color.White,
            shape = GalleryDesign.CardShape
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cerrar")
        }

        Spacer(modifier = Modifier.width(GalleryDesign.PaddingMedium))

        Surface(
            color = Color.Black.copy(alpha = GalleryDesign.AlphaBorderLight),
            shape = GalleryDesign.FilterShape,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = GalleryDesign.ViewerTitlePaddingH,
                        vertical = GalleryDesign.ViewerTitlePaddingV
                    )
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (isFavorite) {
                    Spacer(modifier = Modifier.width(GalleryDesign.PaddingSmall))
                    Box(modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(Color.White.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.width(GalleryDesign.PaddingSmall))
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorito",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(GalleryDesign.PaddingMedium))

        Row(
            horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallFloatingActionButton(
                onClick = onMoreOptions,
                containerColor = Color.Black.copy(alpha = GalleryDesign.AlphaOverlay),
                contentColor = Color.White,
                shape = GalleryDesign.CardShape
            ) {
                Icon(Icons.Default.MoreVert, "Opciones")
            }
        }
    }
}

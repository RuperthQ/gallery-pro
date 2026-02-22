package com.my_gallery.ui.gallery.viewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.my_gallery.ui.theme.GalleryDesign

@Composable
fun ViewerTopBar(
    title: String,
    onClose: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(GalleryDesign.PaddingMedium)
    ) {
        SmallFloatingActionButton(
            onClick = onClose,
            containerColor = Color.Black.copy(alpha = GalleryDesign.AlphaOverlay),
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.CenterStart),
            shape = GalleryDesign.CardShape
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cerrar")
        }

        Surface(
            color = Color.Black.copy(alpha = GalleryDesign.AlphaBorderLight),
            shape = GalleryDesign.FilterShape,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = GalleryDesign.ViewerHeaderSafetyPadding)
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = GalleryDesign.ViewerTitlePaddingH,
                        vertical = GalleryDesign.ViewerTitlePaddingV
                    )
            )
        }

        SmallFloatingActionButton(
            onClick = onMoreOptions,
            containerColor = Color.Black.copy(alpha = GalleryDesign.AlphaOverlay),
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.CenterEnd),
            shape = GalleryDesign.CardShape
        ) {
            Icon(Icons.Default.MoreVert, "Opciones")
        }
    }
}

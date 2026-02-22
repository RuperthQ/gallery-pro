package com.my_gallery.ui.gallery.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.my_gallery.domain.model.AlbumItem
import com.my_gallery.ui.gallery.AlbumBehavior
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.gallery.MenuStyle
import com.my_gallery.ui.security.SecurityViewModel
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.glassBackground
import com.my_gallery.ui.theme.GalleryDesign.bottomPremiumBorder

@Composable
fun GalleryHeaderOrchestrator(
    viewModel: GalleryViewModel,
    securityViewModel: SecurityViewModel,
    menuStyle: MenuStyle,
    albumBehavior: AlbumBehavior,
    showFilters: Boolean,
    albums: List<AlbumItem>,
    selectedAlbum: String?,
    lockedAlbums: Set<String>,
    isViewerOpen: Boolean,
    onAlbumClick: (AlbumItem) -> Unit,
    onAlbumLongClick: (AlbumItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // --- 1. CABECERA SUPERIOR (Modo TOP_HEADER) ---
        AnimatedVisibility(
            visible = !isViewerOpen && menuStyle == MenuStyle.TOP_HEADER,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GalleryDesign.HeaderFullShape)
                    .bottomPremiumBorder(alpha = GalleryDesign.AlphaBorderLight)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .glassBackground()
                        .blur(GalleryDesign.BlurRadius)
                )

                HeaderLayout(
                    showFilters = showFilters,
                    viewModel = viewModel,
                    securityViewModel = securityViewModel
                )
            }
        }

        // --- 2. CARRUSEL DE ÁLBUMES (Modo BOTTOM_FLOATING + TOP Behaviors) ---
        AnimatedVisibility(
            visible = !isViewerOpen && menuStyle == MenuStyle.BOTTOM_FLOATING && (albumBehavior == AlbumBehavior.FLOATING_TOP || albumBehavior == AlbumBehavior.STATIC_TOP),
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (albumBehavior == AlbumBehavior.FLOATING_TOP) {
                            Modifier
                                .clip(GalleryDesign.HeaderFullShape)
                                .bottomPremiumBorder(alpha = GalleryDesign.AlphaBorderLight)
                        } else Modifier
                    )
            ) {
                if (albumBehavior == AlbumBehavior.FLOATING_TOP) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .glassBackground()
                            .blur(GalleryDesign.BlurRadius)
                    )
                } else if (albumBehavior == AlbumBehavior.STATIC_TOP) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }

                PremiumAlbumCarousel(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(all = GalleryDesign.PaddingSmall),
                    albums = albums,
                    selectedAlbumId = selectedAlbum,
                    onAlbumClick = onAlbumClick,
                    lockedAlbums = lockedAlbums,
                    onAlbumLongClick = onAlbumLongClick
                )
            }
        }
    }
}

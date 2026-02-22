package com.my_gallery.ui.gallery.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.my_gallery.domain.model.AlbumItem
import com.my_gallery.ui.gallery.AlbumBehavior
import com.my_gallery.ui.gallery.GalleryUiModel
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.gallery.MenuStyle
import com.my_gallery.ui.security.SecurityViewModel
import com.my_gallery.ui.theme.GalleryDesign

@Composable
fun GalleryMainGrid(
    items: LazyPagingItems<GalleryUiModel>,
    viewModel: GalleryViewModel,
    securityViewModel: SecurityViewModel,
    columnCount: Int,
    menuStyle: MenuStyle,
    albumBehavior: AlbumBehavior,
    albums: List<AlbumItem>,
    selectedAlbum: String?,
    lockedAlbums: Set<String>,
    isEditPermissionGranted: Boolean,
    isSelectionMode: Boolean,
    selectedMediaIds: Set<String>,
    onAlbumClick: (AlbumItem) -> Unit,
    onAlbumLongClick: (AlbumItem) -> Unit,
    onMediaClick: (com.my_gallery.domain.model.MediaItem, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedColumns by animateIntAsState(targetValue = columnCount, label = "columnAnim")
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(animatedColumns),
        contentPadding = PaddingValues(
            bottom = if (menuStyle == MenuStyle.BOTTOM_FLOATING) {
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 90.dp
            } else GalleryDesign.PaddingLarge,
            start = GalleryDesign.PaddingSmall,
            end = GalleryDesign.PaddingSmall
        ),
        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall),
        verticalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall),
        modifier = modifier.fillMaxSize()
    ) {
        // --- ESPACIADO INICIAL ---
        if (menuStyle == MenuStyle.TOP_HEADER) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                // Spacer para compensar el header flotante/fijo superior en modo TOP_HEADER
                HeaderLayout(
                    showFilters = false,
                    viewModel = viewModel,
                    securityViewModel = securityViewModel,
                    modifier = Modifier.alpha(0f) // Invisible pero reserva el espacio si es necesario
                )
            }
        } else if (menuStyle == MenuStyle.BOTTOM_FLOATING && (albumBehavior == AlbumBehavior.FLOATING_TOP || albumBehavior == AlbumBehavior.STATIC_TOP)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.statusBarsPadding().height(GalleryDesign.PaddingSmall + 110.dp))
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.statusBarsPadding().height(GalleryDesign.PaddingSmall))
            }
        }

        // --- CARRUSEL DE ÁLBUMES (Si está en el Grid) ---
        if (albums.isNotEmpty() && (menuStyle == MenuStyle.TOP_HEADER || albumBehavior == AlbumBehavior.FIXED_IN_GRID)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                PremiumAlbumCarousel(
                    albums = albums,
                    selectedAlbumId = selectedAlbum,
                    onAlbumClick = onAlbumClick,
                    lockedAlbums = lockedAlbums,
                    onAlbumLongClick = onAlbumLongClick
                )
            }
        }

        // --- BANNER DE PERMISOS ---
        if (!isEditPermissionGranted) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                PermissionBanner(onGrantClick = { viewModel.requestEditPermission(context) })
            }
        }

        // --- ITEMS DE LA GALERÍA ---
        items(
            count = items.itemCount,
            key = items.itemKey { 
                when(it) {
                    is GalleryUiModel.Separator -> "sep_${it.dateLabel}"
                    is GalleryUiModel.Media -> it.item.id
                }
            },
            span = { index ->
                val span = when (items[index]) {
                    is GalleryUiModel.Separator -> maxLineSpan
                    else -> 1
                }
                GridItemSpan(span)
            }
        ) { index ->
            items[index]?.let { model ->
                when (model) {
                    is GalleryUiModel.Separator -> {
                        val metadata by viewModel.sectionMetadata.collectAsStateWithLifecycle()
                        val isChecked = remember(selectedMediaIds, model.dateLabel) {
                            viewModel.isGroupSelected(model.dateLabel, model.period, selectedMediaIds)
                        }
                        SectionHeader(
                            label = model.dateLabel,
                            metadata = metadata[model.dateLabel],
                            isChecked = isChecked,
                            onToggleCheck = { viewModel.toggleGroupSelection(model.dateLabel, model.period) }
                        )
                    }
                    is GalleryUiModel.Media -> {
                        GalleryItem(
                            item = model.item,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedMediaIds.contains(model.item.id),
                            onClick = { onMediaClick(model.item, index) },
                            onLongClick = { viewModel.selectItem(model.item) },
                            onToggleSelection = { viewModel.toggleMediaSelection(model.item.id) }
                        )
                    }
                }
            }
        }

        // --- ESTADOS DE CARGA Y VACÍO ---
        if (items.itemCount == 0 && items.loadState.refresh is LoadState.NotLoading) {
             item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(GalleryDesign.PaddingLarge), contentAlignment = Alignment.Center) {
                    Text("No hay elementos para mostrar", color = Color.Gray)
                }
             }
        }

        if (items.loadState.refresh is LoadState.Loading) {
            items(20) { GalleryPlaceholder() }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(GalleryDesign.PaddingLarge),
                contentAlignment = Alignment.Center
            ) {
                if (items.loadState.append is LoadState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(GalleryDesign.IconSizeNormal))
                }
            }
        }
    }
}

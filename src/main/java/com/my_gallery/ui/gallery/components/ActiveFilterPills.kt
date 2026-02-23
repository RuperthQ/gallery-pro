package com.my_gallery.ui.gallery.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.glassBackground
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

@Composable
fun ActiveFilterPills(
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    val selectedFilters by viewModel.selectedFilters.collectAsState()
    val selectedTypes by viewModel.selectedTypes.collectAsState()
    val selectedExtensions by viewModel.selectedExtensions.collectAsState()
    val selectedResolutions by viewModel.selectedResolutions.collectAsState()
    val selectedAlbumId by viewModel.selectedAlbum.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val isShortDate by viewModel.shortDateFilters.collectAsState()

    val activeFilters = remember(selectedFilters, selectedTypes, selectedExtensions, selectedResolutions, selectedAlbumId, albums, isShortDate) {
        mutableListOf<FilterPillData>().apply {
            // Filtros de fecha
            selectedFilters.forEach { dateLabel ->
                val finalLabel = if (isShortDate) {
                    val parts = dateLabel.split(" ")
                    if (parts.size >= 2) {
                        val shortMonth = parts[0].take(3).replaceFirstChar { it.uppercase() }
                        "$shortMonth ${parts[1]}"
                    } else dateLabel
                } else dateLabel
                
                add(FilterPillData(finalLabel, Icons.Default.CalendarMonth) { viewModel.onFilterSelected(dateLabel) })
            }

            // Filtros de tipo
            selectedTypes.forEach { type ->
                add(FilterPillData(type, if (type == "Imágenes") Icons.Default.Image else Icons.Default.Videocam) { viewModel.onTypeFilterSelected(type) })
            }

            // Filtros de extensión
            selectedExtensions.forEach { ext ->
                add(FilterPillData(ext, Icons.Default.Image) { viewModel.onExtensionFilterSelected(ext) })
            }

            // Filtros de video (Resolución)
            selectedResolutions.forEach { res ->
                add(FilterPillData(res, Icons.Default.Videocam) { viewModel.onResolutionFilterSelected(res) })
            }

            // Filtro de álbum
            selectedAlbumId?.let { id ->
                val albumName = albums.find { it.id == id }?.name ?: "Álbum"
                if (id != "ALL_VIRTUAL_ALBUM") {
                    add(FilterPillData(albumName, Icons.Default.Folder) { viewModel.toggleAlbum(null) })
                }
            }
        }
    }

    AnimatedVisibility(
        visible = activeFilters.isNotEmpty(),
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall),
            contentPadding = PaddingValues(horizontal = GalleryDesign.PaddingLarge),
            modifier = Modifier.padding(bottom = GalleryDesign.PaddingSmall)
        ) {
            items(activeFilters) { filter ->
                FilterPill(filter)
            }
        }
    }
}

data class FilterPillData(
    val label: String,
    val icon: ImageVector,
    val onClear: () -> Unit
)

@Composable
fun FilterPill(filter: FilterPillData) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .glassBackground(alpha1 = 0.8f, alpha2 = 0.6f)
            .premiumBorder(shape = CircleShape, width = 1.dp, alpha = 0.3f),
        color = Color.Transparent,
        shape = CircleShape
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = GalleryDesign.PillPaddingHorizontal, 
                vertical = GalleryDesign.PillPaddingVertical
            ),
            horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall)
        ) {
            Icon(
                imageVector = filter.icon,
                contentDescription = null,
                modifier = Modifier.size(GalleryDesign.PillIconSize),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = filter.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = GalleryDesign.PillFontSize,
                    letterSpacing = 0.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .size(GalleryDesign.PillClearBoxSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .clickable { filter.onClear() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Limpiar",
                    modifier = Modifier.size(GalleryDesign.PillClearIconSize),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

package com.my_gallery.ui.gallery.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

@Composable
fun HeaderLayout(
    showFilters: Boolean,
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .padding(bottom = GalleryDesign.PaddingTiny)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GalleryDesign.PaddingLarge, vertical = GalleryDesign.PaddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Mi Galería",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Archivos locales",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(GalleryDesign.IconSizeAction)
                        .clip(GalleryDesign.CardShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { viewModel.toggleFilters() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showFilters) Icons.Default.ExpandLess else Icons.Default.FilterList,
                        contentDescription = "Filtros",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(GalleryDesign.IconSizeNormal)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showFilters,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FilterRow(viewModel)
        }
    }
}

@Composable
fun FilterSection(
    label: String,
    viewModel: GalleryViewModel,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GalleryDesign.PaddingTiny)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GalleryDesign.PaddingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(GalleryDesign.PaddingSmall))
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        }
        content()
    }
}

@Composable
fun FilterRow(viewModel: GalleryViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GalleryDesign.PaddingSmall)
    ) {
        FilterSection(label = "FECHA", viewModel = viewModel) {
            val filters by viewModel.availableFilters.collectAsStateWithLifecycle()
            val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

            LazyRow(
                contentPadding = PaddingValues(horizontal = GalleryDesign.PaddingLarge),
                horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall),
                modifier = Modifier.padding(vertical = GalleryDesign.PaddingSmall)
            ) {
                items(filters) { filter ->
                    GalleryFilterChip(
                        label = filter,
                        isSelected = filter == (selectedFilter ?: "Todos"),
                        onClick = { viewModel.onFilterSelected(filter) }
                    )
                }
            }
        }

        ImageFilterRow(viewModel)
        VideoFilterRow(viewModel)
    }
}

@Composable
fun ImageFilterRow(viewModel: GalleryViewModel) {
    val extensions by viewModel.availableImageExtensions.collectAsStateWithLifecycle()
    val selectedMime by viewModel.selectedImageFilter.collectAsStateWithLifecycle()

    if (extensions.isNotEmpty()) {
        FilterSection(label = "FOTOS", viewModel = viewModel) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = GalleryDesign.PaddingLarge),
                horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall),
                modifier = Modifier.padding(vertical = GalleryDesign.PaddingSmall)
            ) {
                items(extensions) { ext ->
                    GalleryFilterChip(
                        label = ext,
                        isSelected = (ext == "Todas" && selectedMime == null) || (ext == selectedMime),
                        onClick = { viewModel.onImageFilterSelected(ext) }
                    )
                }
            }
        }
    }
}

@Composable
fun VideoFilterRow(viewModel: GalleryViewModel) {
    val resolutions by viewModel.availableVideoResolutions.collectAsStateWithLifecycle()
    val selectedVideoFilter by viewModel.selectedVideoFilter.collectAsStateWithLifecycle()

    if (resolutions.isNotEmpty()) {
        FilterSection(label = "VIDEOS", viewModel = viewModel) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = GalleryDesign.PaddingLarge),
                horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall),
                modifier = Modifier.padding(vertical = GalleryDesign.PaddingSmall)
            ) {
                items(resolutions) { res ->
                    GalleryFilterChip(
                        label = res,
                        isSelected = (res == "Todas" && selectedVideoFilter == null) || (res == selectedVideoFilter),
                        onClick = { viewModel.onVideoFilterSelected(res) }
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(GalleryDesign.ButtonHeight)
            .premiumBorder(
                shape = GalleryDesign.FilterShape,
                width = if (isSelected) GalleryDesign.BorderWidthBold else GalleryDesign.BorderWidthThin,
                alpha = if (isSelected) 1f else 0.3f
            )
            .clip(GalleryDesign.FilterShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(onClick = onClick)
            .padding(horizontal = GalleryDesign.PaddingMedium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

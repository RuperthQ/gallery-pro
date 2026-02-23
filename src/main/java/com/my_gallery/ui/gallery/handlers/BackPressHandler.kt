package com.my_gallery.ui.gallery.handlers

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my_gallery.domain.model.MediaItem
import com.my_gallery.ui.gallery.GalleryViewModel

@Composable
fun setupGalleryBackHandlers(
    viewModel: GalleryViewModel,
    isSelectionMode: Boolean,
    showSettings: Boolean,
    viewerItem: MediaItem?
) {
    BackHandler(enabled = isSelectionMode && viewerItem == null) { viewModel.exitSelection() }
    BackHandler(enabled = showSettings) { viewModel.hideSettings() }
}

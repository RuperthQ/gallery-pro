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
    showTrash: Boolean,
    selectedAlbumId: String?,
    viewerItem: MediaItem?
) {
    // El orden importa: el último BackHandler habilitado es el que se ejecuta.
    // Priorizamos los estados "más internos" o temporales.

    // 1. Si hay un álbum seleccionado, volver a "Todos"
    BackHandler(enabled = selectedAlbumId != null && !isSelectionMode && !showSettings && !showTrash && viewerItem == null) {
        viewModel.toggleAlbum(null)
    }

    // 2. Si la papelera está abierta, cerrarla
    BackHandler(enabled = showTrash && viewerItem == null) {
        viewModel.hideTrash()
    }

    // 3. Si los ajustes están abiertos, cerrarlos
    BackHandler(enabled = showSettings) {
        viewModel.hideSettings()
    }

    // 4. Si hay selección activa, cancelarla
    BackHandler(enabled = isSelectionMode && viewerItem == null) {
        viewModel.exitSelection()
    }
}


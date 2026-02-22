package com.my_gallery.ui.gallery.header_actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import com.my_gallery.ui.gallery.GalleryViewModel

class RefreshGalleryAction(private val viewModel: GalleryViewModel) {
    operator fun invoke(): HeaderAction {
        return HeaderAction(
            icon = Icons.Default.Refresh,
            description = "Actualizar Galería",
            onClick = { viewModel.forceSyncGallery() },
            isSelected = false
        )
    }
}

package com.my_gallery.ui.gallery.header_actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import com.my_gallery.ui.gallery.GalleryViewModel

class ToggleSelectionAction(private val viewModel: GalleryViewModel) {
    operator fun invoke(): HeaderAction {
        return HeaderAction(
            icon = Icons.Default.CheckCircle,
            description = "Seleccionar",
            onClick = { viewModel.toggleSelectionMode() }
        )
    }
}

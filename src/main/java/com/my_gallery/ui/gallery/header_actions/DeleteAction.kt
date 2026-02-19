package com.my_gallery.ui.gallery.header_actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import com.my_gallery.ui.gallery.GalleryViewModel

class DeleteAction(private val viewModel: GalleryViewModel) {
    operator fun invoke(): HeaderAction {
        return HeaderAction(
            icon = Icons.Default.Delete,
            description = "Eliminar",
            onClick = { viewModel.showDeleteConfirmation() }
        )
    }
}

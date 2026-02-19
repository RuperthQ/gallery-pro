package com.my_gallery.ui.gallery.header_actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.my_gallery.ui.gallery.GalleryViewModel

class CancelSelectionAction(private val viewModel: GalleryViewModel) {
    operator fun invoke(): HeaderAction {
        return HeaderAction(
            icon = Icons.Default.Close,
            description = "Cancelar",
            onClick = { viewModel.exitSelection() }
        )
    }
}

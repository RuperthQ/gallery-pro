package com.my_gallery.ui.gallery.header_actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import com.my_gallery.ui.gallery.GalleryViewModel

class ChangeGridAction(private val viewModel: GalleryViewModel) {
    operator fun invoke(): HeaderAction {
        return HeaderAction(
            icon = Icons.Default.GridView,
            description = "Cambiar Columnas",
            onClick = { viewModel.changeColumns() }
        )
    }
}

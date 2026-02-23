package com.my_gallery.ui.gallery.header_actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import com.my_gallery.ui.gallery.GalleryViewModel

class ToggleFavoriteAction(
    private val viewModel: GalleryViewModel
) {
    operator fun invoke(): HeaderAction {
        return HeaderAction(
            icon = Icons.Default.Favorite,
            description = "Añadir a favoritos",
            onClick = {
                viewModel.toggleFavoriteSelection()
            }
        )
    }
}

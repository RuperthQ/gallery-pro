package com.my_gallery.ui.gallery.header_actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import com.my_gallery.ui.gallery.GalleryViewModel

class ToggleEmptyAlbumsAction(
    private val viewModel: GalleryViewModel,
    private val isEnabled: Boolean
) {
    operator fun invoke(): HeaderAction {
        return HeaderAction(
            icon = Icons.Default.FolderSpecial,
            description = "Álbumes Vacíos",
            onClick = { viewModel.toggleShowEmptyAlbums() },
            isSelected = isEnabled
        )
    }
}

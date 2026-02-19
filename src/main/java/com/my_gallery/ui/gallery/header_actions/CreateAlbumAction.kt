package com.my_gallery.ui.gallery.header_actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryAdd
import com.my_gallery.ui.gallery.GalleryViewModel

class CreateAlbumAction(private val viewModel: GalleryViewModel) {
    operator fun invoke(): HeaderAction {
        return HeaderAction(
            icon = Icons.Default.LibraryAdd,
            description = "Nuevo Álbum",
            onClick = { viewModel.showCreateAlbumDialog() }
        )
    }
}

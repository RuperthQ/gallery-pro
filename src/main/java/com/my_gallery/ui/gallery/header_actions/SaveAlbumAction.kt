package com.my_gallery.ui.gallery.header_actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import com.my_gallery.ui.gallery.GalleryViewModel

class SaveAlbumAction(private val viewModel: GalleryViewModel) {
    operator fun invoke(): HeaderAction {
        return HeaderAction(
            icon = Icons.Default.Save,
            description = "Guardar",
            onClick = { viewModel.saveSelectedToNewAlbum() }
        )
    }
}

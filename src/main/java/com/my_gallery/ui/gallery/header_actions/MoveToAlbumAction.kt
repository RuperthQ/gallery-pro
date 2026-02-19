package com.my_gallery.ui.gallery.header_actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import com.my_gallery.ui.gallery.GalleryViewModel

class MoveToAlbumAction(private val viewModel: GalleryViewModel) {
    operator fun invoke(): HeaderAction {
        return HeaderAction(
            icon = Icons.AutoMirrored.Filled.DriveFileMove,
            description = "Mover",
            onClick = { viewModel.showMoveToAlbumDialog() }
        )
    }
}

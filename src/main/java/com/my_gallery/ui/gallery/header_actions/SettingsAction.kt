package com.my_gallery.ui.gallery.header_actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.my_gallery.ui.gallery.GalleryViewModel

class SettingsAction(
    private val viewModel: GalleryViewModel
) {
    operator fun invoke(): HeaderAction {
        return HeaderAction(
            icon = Icons.Default.Settings,
            description = "Ajustes",
            onClick = { viewModel.showSettings() }
        )
    }
}

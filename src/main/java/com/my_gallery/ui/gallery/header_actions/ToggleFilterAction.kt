package com.my_gallery.ui.gallery.header_actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FilterList
import com.my_gallery.ui.gallery.GalleryViewModel

class ToggleFilterAction(
    private val viewModel: GalleryViewModel,
    private val isFiltersVisible: Boolean
) {
    operator fun invoke(): HeaderAction {
        return HeaderAction(
            icon = if (isFiltersVisible) Icons.Default.ExpandLess else Icons.Default.FilterList,
            description = "Filtros",
            onClick = { viewModel.toggleFilters() }
        )
    }
}

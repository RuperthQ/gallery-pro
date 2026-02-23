package com.my_gallery.ui.gallery.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my_gallery.ui.gallery.GalleryViewModel

class ResolutionFilter(private val viewModel: GalleryViewModel) : GalleryFilter {
    override val title: String = "RESOLUCIÓN"

    @Composable
    override fun getOptions(): State<List<String>> = viewModel.availableResolutions.collectAsStateWithLifecycle()

    @Composable
    override fun getSelectedOptions(): State<Set<String>> = viewModel.selectedResolutions.collectAsStateWithLifecycle()

    override fun onOptionSelected(option: String) {
        viewModel.onResolutionFilterSelected(option)
    }
}

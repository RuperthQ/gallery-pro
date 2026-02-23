package com.my_gallery.ui.gallery.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my_gallery.ui.gallery.GalleryViewModel

class ExtensionFilter(private val viewModel: GalleryViewModel) : GalleryFilter {
    override val title: String = "EXTENSIÓN"

    @Composable
    override fun getOptions(): State<List<String>> = viewModel.availableExtensions.collectAsStateWithLifecycle()

    @Composable
    override fun getSelectedOptions(): State<Set<String>> = viewModel.selectedExtensions.collectAsStateWithLifecycle()

    override fun onOptionSelected(option: String) {
        viewModel.onExtensionFilterSelected(option)
    }
}

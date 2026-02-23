package com.my_gallery.ui.gallery.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my_gallery.ui.gallery.GalleryViewModel

class TypeFilter(private val viewModel: GalleryViewModel) : GalleryFilter {
    override val title: String = "TIPO"

    @Composable
    override fun getOptions(): State<List<String>> = viewModel.availableTypes.collectAsStateWithLifecycle()

    @Composable
    override fun getSelectedOptions(): State<Set<String>> = viewModel.selectedTypes.collectAsStateWithLifecycle()

    override fun onOptionSelected(option: String) {
        viewModel.onTypeFilterSelected(option)
    }
}

package com.my_gallery.ui.gallery.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my_gallery.ui.gallery.GalleryViewModel

class DateFilter(private val viewModel: GalleryViewModel) : GalleryFilter {
    override val title: String = "FECHA"

    @Composable
    override fun getOptions(): State<List<String>> = viewModel.availableFilters.collectAsStateWithLifecycle()

    @Composable
    override fun getSelectedOption(): State<String?> = viewModel.selectedFilter.collectAsStateWithLifecycle()

    override fun onOptionSelected(option: String) {
        viewModel.onFilterSelected(option)
    }
}

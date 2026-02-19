package com.my_gallery.ui.gallery.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my_gallery.ui.gallery.GalleryViewModel

class PhotoFilter(private val viewModel: GalleryViewModel) : GalleryFilter {
    override val title: String = "FOTOS"

    @Composable
    override fun getOptions(): State<List<String>> = viewModel.availableImageExtensions.collectAsStateWithLifecycle()

    @Composable
    override fun getSelectedOption(): State<String?> = viewModel.selectedImageFilter.collectAsStateWithLifecycle()

    override fun onOptionSelected(option: String) {
        viewModel.onImageFilterSelected(option)
    }
}

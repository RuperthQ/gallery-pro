package com.my_gallery.ui.gallery.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my_gallery.ui.gallery.GalleryViewModel

class VideoFilter(private val viewModel: GalleryViewModel) : GalleryFilter {
    override val title: String = "VIDEOS"

    @Composable
    override fun getOptions(): State<List<String>> = viewModel.availableVideoResolutions.collectAsStateWithLifecycle()

    @Composable
    override fun getSelectedOption(): State<String?> = viewModel.selectedVideoFilter.collectAsStateWithLifecycle()

    override fun onOptionSelected(option: String) {
        viewModel.onVideoFilterSelected(option)
    }
}

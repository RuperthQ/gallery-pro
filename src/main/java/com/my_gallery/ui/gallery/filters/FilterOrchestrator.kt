package com.my_gallery.ui.gallery.filters

import com.my_gallery.ui.gallery.GalleryViewModel

class FilterOrchestrator(private val viewModel: GalleryViewModel) {
    fun getFilters(): List<GalleryFilter> {
        return listOf(
            DateFilter(viewModel),
            PhotoFilter(viewModel),
            VideoFilter(viewModel)
        )
    }
}

package com.my_gallery.ui.gallery.filters

import com.my_gallery.ui.gallery.GalleryViewModel

class FilterOrchestrator(private val viewModel: GalleryViewModel) {
    fun getFilters(): List<GalleryFilter> {
        val filters = mutableListOf<GalleryFilter>()
        filters.add(DateFilter(viewModel))

        if (viewModel.showFilterType.value) {
            filters.add(TypeFilter(viewModel))
        }
        if (viewModel.showFilterRes.value) {
            filters.add(ResolutionFilter(viewModel))
        }
        if (viewModel.showFilterExt.value) {
            filters.add(ExtensionFilter(viewModel))
        }

        return filters
    }
}

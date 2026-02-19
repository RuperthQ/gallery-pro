package com.my_gallery.ui.gallery.header_actions

import com.my_gallery.ui.gallery.GalleryViewModel

class HeaderActionsOrchestrator(
    private val viewModel: GalleryViewModel
) {
    fun getNormalActions(showFilters: Boolean): List<HeaderAction> {
        return listOf(
            CreateAlbumAction(viewModel)(),
            ChangeGridAction(viewModel)(),
            ToggleFilterAction(viewModel, showFilters)(),
            ToggleSelectionAction(viewModel)()
        )
    }

    fun getSelectionActions(isAlbumCreationPending: Boolean, selectedCount: Int): List<HeaderAction> {
        return if (isAlbumCreationPending) {
            listOf(SaveAlbumAction(viewModel)())
        } else if (selectedCount > 0) {
            listOf(
                MoveToAlbumAction(viewModel)(),
                DeleteAction(viewModel)()
            )
        } else {
            emptyList()
        }
    }

    fun getCancelAction(): HeaderAction {
        return CancelSelectionAction(viewModel)()
    }
}

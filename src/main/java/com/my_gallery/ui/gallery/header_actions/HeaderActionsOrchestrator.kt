package com.my_gallery.ui.gallery.header_actions

import com.my_gallery.ui.gallery.GalleryViewModel

class HeaderActionsOrchestrator(
    private val viewModel: GalleryViewModel
) {
    fun getNormalActions(showFilters: Boolean, showEmptyAlbums: Boolean): List<HeaderAction> {
        return listOf(
            RefreshGalleryAction(viewModel)(),
            CreateAlbumAction(viewModel)(),
            ChangeGridAction(viewModel)(),
            ToggleFilterAction(viewModel, showFilters)(),
            ToggleEmptyAlbumsAction(viewModel, showEmptyAlbums)(),
            ToggleSelectionAction(viewModel)(),
            SettingsAction(viewModel)()
        )
    }

    fun getSelectionActions(isAlbumCreationPending: Boolean, selectedCount: Int, areAllSecured: Boolean): List<HeaderAction> {
        return if (isAlbumCreationPending) {
            listOf(SaveAlbumAction(viewModel)())
        } else if (selectedCount > 0) {
            listOf(
                if (areAllSecured) UnsecureAction(viewModel)() else SecureAction(viewModel)(),
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

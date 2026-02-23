package com.my_gallery.ui.gallery

data class GalleryUiState(
    val isSelectionMode: Boolean = false,
    val isAlbumCreationPending: Boolean = false,
    val showFilters: Boolean = false,
    val showSettings: Boolean = false,
    val showTrash: Boolean = false,
    val showCreateAlbumDialog: Boolean = false,
    val showMoveToAlbumDialog: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val isMovingMedia: Boolean = false,
    val isSecuringMedia: Boolean = false,
    val isUnsecuringMedia: Boolean = false,
    val isForceSyncing: Boolean = false,
    val isEditPermissionGranted: Boolean = false,
    val targetAlbumName: String? = null,
    val targetAlbumId: String? = null
)

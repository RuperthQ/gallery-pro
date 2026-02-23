package com.my_gallery.domain.usecase.selection

import javax.inject.Inject

class ToggleMediaSelectionUseCase @Inject constructor() {
    operator fun invoke(currentSelection: Set<String>, mediaId: String): Set<String> {
        return if (currentSelection.contains(mediaId)) {
            currentSelection - mediaId
        } else {
            currentSelection + mediaId
        }
    }
}

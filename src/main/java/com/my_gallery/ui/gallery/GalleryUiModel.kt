package com.my_gallery.ui.gallery

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.my_gallery.domain.model.MediaItem

@Keep
@Immutable
sealed class GalleryUiModel {
    data class Media(val item: MediaItem) : GalleryUiModel()
    data class Separator(val dateLabel: String) : GalleryUiModel()
}

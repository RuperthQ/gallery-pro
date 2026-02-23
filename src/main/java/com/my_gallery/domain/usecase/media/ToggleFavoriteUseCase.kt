package com.my_gallery.domain.usecase.media

import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.domain.model.MediaItem
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(id: String) {
        repository.toggleFavorite(id)
    }

    suspend operator fun invoke(ids: List<String>) {
        ids.forEach { repository.toggleFavorite(it) }
    }
}

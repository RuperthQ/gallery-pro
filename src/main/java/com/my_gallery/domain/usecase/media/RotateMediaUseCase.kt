package com.my_gallery.domain.usecase.media

import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.domain.model.MediaItem
import javax.inject.Inject

class RotateMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(item: MediaItem) {
        val nextRotation = (item.rotation + 90f) % 360f
        repository.updateMediaRotation(item, nextRotation)
    }
}

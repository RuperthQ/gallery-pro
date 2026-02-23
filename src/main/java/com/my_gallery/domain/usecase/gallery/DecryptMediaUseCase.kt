package com.my_gallery.domain.usecase.gallery

import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.domain.model.MediaItem
import javax.inject.Inject

class DecryptMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(item: MediaItem): String? {
        val file = repository.decryptMediaToCache(item.id, item.mimeType)
        return file?.absolutePath
    }
}

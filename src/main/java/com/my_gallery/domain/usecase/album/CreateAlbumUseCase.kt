package com.my_gallery.domain.usecase.album

import com.my_gallery.data.repository.MediaRepository
import javax.inject.Inject

class CreateAlbumUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(name: String, mediaIds: List<String>): Boolean {
        return if (mediaIds.isEmpty()) {
            repository.createAlbum(name) != null
        } else {
            val items = repository.getMediaByIds(mediaIds)
            repository.moveMediaToAlbum(items, name)
        }
    }
}

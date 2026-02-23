package com.my_gallery.domain.usecase.gallery

import com.my_gallery.data.repository.MediaRepository
import javax.inject.Inject

class SyncGalleryUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(force: Boolean = false) {
        repository.syncLocalGallery(force = force)
    }
}

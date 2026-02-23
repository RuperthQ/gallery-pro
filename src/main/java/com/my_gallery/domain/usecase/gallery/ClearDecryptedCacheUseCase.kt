package com.my_gallery.domain.usecase.gallery

import com.my_gallery.data.repository.MediaRepository
import javax.inject.Inject

class ClearDecryptedCacheUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke() {
        repository.clearDecryptedCache()
    }
}

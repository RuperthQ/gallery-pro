package com.my_gallery.domain.usecase.media

import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.data.repository.media.DeleteResult
import timber.log.Timber
import javax.inject.Inject

class UnsecureMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(selectedIds: List<String>): DeleteResult? {
        if (selectedIds.isEmpty()) return null
        Timber.d("Unsecuring media items: $selectedIds")
        val toUnsecure = repository.getMediaByIds(selectedIds)
            .filter { it.albumId == "SECURE_VAULT" }
        if (toUnsecure.isEmpty()) return null
        return repository.unsecureMediaItems(toUnsecure)
    }
}

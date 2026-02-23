package com.my_gallery.domain.usecase.media

import android.os.Build
import androidx.annotation.RequiresApi
import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.data.repository.media.DeleteResult
import timber.log.Timber
import javax.inject.Inject

class SecureMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend operator fun invoke(selectedIds: List<String>): DeleteResult? {
        if (selectedIds.isEmpty()) return null
        Timber.d("Securing media items: $selectedIds")
        val toSecure = repository.getMediaByIds(selectedIds)
        if (toSecure.isEmpty()) return null
        return repository.secureMediaItems(toSecure)
    }
}

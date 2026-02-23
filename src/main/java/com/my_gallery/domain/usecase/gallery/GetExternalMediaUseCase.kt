package com.my_gallery.domain.usecase.gallery

import android.net.Uri
import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.domain.model.MediaItem
import javax.inject.Inject

class GetExternalMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(uri: Uri, mimeType: String?): MediaItem? {
        return repository.getMediaFromExternalUri(uri, mimeType)
    }
}

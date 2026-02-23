package com.my_gallery.domain.usecase.media

import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.domain.model.MediaItem
import javax.inject.Inject

class SetWallpaperUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(item: MediaItem): Boolean {
        return repository.setWallpaper(item)
    }
}

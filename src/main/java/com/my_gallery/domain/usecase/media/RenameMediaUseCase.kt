package com.my_gallery.domain.usecase.media

import android.os.Build
import androidx.annotation.RequiresApi
import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.data.repository.media.RenameResult
import com.my_gallery.domain.model.MediaItem
import timber.log.Timber
import javax.inject.Inject

class RenameMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend operator fun invoke(item: MediaItem, newName: String): RenameResult {
        Timber.d("Renaming media: ${item.id} to $newName")
        val lastDotIndex = item.title.lastIndexOf('.')
        val extension = if (lastDotIndex != -1) item.title.substring(lastDotIndex) else ""
        val cleanedNewName = newName.removeSuffix(extension).removeSuffix(".")
        val fullNewName = "$cleanedNewName$extension"
        
        return repository.renameMedia(item, fullNewName)
    }
}

package com.my_gallery.domain.usecase.album

import android.os.Environment
import com.my_gallery.data.repository.MediaRepository
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class SaveMediaToAlbumUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(selectedIds: List<String>, albumName: String, targetAlbumId: String? = null): SaveResult {
        Timber.d("Saving to album: $albumName, selected: ${selectedIds.size}")
        if (selectedIds.isEmpty()) {
            repository.createAlbum(albumName)
            return SaveResult.AlbumCreatedOnly
        }
        
        val toMove = repository.getMediaByIds(selectedIds)
        val finalAlbumId = targetAlbumId ?: File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), 
            "Gallery_Pro/$albumName"
        ).absolutePath.lowercase().hashCode().toString()

        val success = repository.moveMediaToAlbum(toMove, albumName, targetAlbumId)
        return if (success) SaveResult.SuccessMove(finalAlbumId) else SaveResult.Error
    }
}

sealed class SaveResult {
    object AlbumCreatedOnly : SaveResult()
    data class SuccessMove(val finalAlbumId: String) : SaveResult()
    object Error : SaveResult()
}

package com.my_gallery.domain.usecase.album

import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.domain.model.AlbumItem
import javax.inject.Inject

/**
 * Elimina la carpeta de un álbum del sistema de archivos.
 * Los archivos dentro del álbum NO se borran, solo se elimina la carpeta vacía.
 * Si la carpeta tiene archivos, el MediaStore dejará de agruparlos bajo ese álbum.
 */
class DeleteAlbumUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(album: AlbumItem): Boolean =
        repository.deleteAlbumFolder(album.id)
}

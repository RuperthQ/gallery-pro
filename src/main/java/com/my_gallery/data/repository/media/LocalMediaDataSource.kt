package com.my_gallery.data.repository.media

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.my_gallery.data.local.dao.MediaDao
import com.my_gallery.domain.model.AlbumItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos encargada de las consultas directas a MediaStore.
 */
@Singleton
class LocalMediaDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao
) {
    /**
     * Obtiene los álbumes (carpetas) locales con su miniatura más reciente.
     */
    fun getLocalAlbums(includeEmpty: Boolean = false): Flow<List<AlbumItem>> = flow {
        val albums = mutableMapOf<String, AlbumItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns._ID
        )

        val uris = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )

        uris.forEach { uri ->
            context.contentResolver.query(
                uri,
                projection,
                null, null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)

                while (cursor.moveToNext()) {
                    val bucketId = cursor.getString(bucketIdCol)
                    if (!albums.containsKey(bucketId)) {
                        val name = cursor.getString(bucketNameCol) ?: "Otros"
                        val id = cursor.getLong(idCol)
                        val contentUri = ContentUris.withAppendedId(uri, id).toString()
                        val rot = mediaDao.getRotationByUrl(contentUri) ?: 0f
                        albums[bucketId] = AlbumItem(bucketId, name, contentUri, 1, rot)
                    } else {
                        val current = albums[bucketId]!!
                        albums[bucketId] = current.copy(count = current.count + 1)
                    }
                }
            }
        }

        if (includeEmpty) {
            try {
                val dcim = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM)
                val galleryPro = java.io.File(dcim, "Gallery_Pro")
                if (galleryPro.exists() && galleryPro.isDirectory) {
                    galleryPro.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
                        val bucketId = dir.absolutePath.lowercase().hashCode().toString()
                        if (!albums.containsKey(bucketId)) {
                            albums[bucketId] = AlbumItem(
                                id = bucketId,
                                name = dir.name,
                                thumbnail = "",
                                count = 0
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        emit(albums.values.toList().sortedBy { it.name })
    }.flowOn(Dispatchers.IO)

    /**
     * Busca la ruta física de un álbum por su ID de MediaStore.
     */
    fun getAlbumPathById(albumId: String): java.io.File? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        val selection = "${MediaStore.MediaColumns.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(albumId)
        
        val uris = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )

        uris.forEach { uri ->
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val path = cursor.getString(0)
                    if (path != null) {
                        val file = java.io.File(path)
                        return file.parentFile
                    }
                }
            }
        }
        return null
    }
}

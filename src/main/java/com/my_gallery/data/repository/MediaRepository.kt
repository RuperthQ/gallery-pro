package com.my_gallery.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.paging.PagingSource
import com.my_gallery.data.local.dao.MediaDao
import com.my_gallery.data.local.entity.MediaEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao
) {

    private val syncMutex = Mutex()

    /**
     * Sincroniza la galería local (Fotos y Videos) con Room en segundo plano.
     */
    suspend fun syncLocalGallery() = withContext(Dispatchers.IO) {
        if (syncMutex.isLocked) return@withContext
        syncMutex.withLock {
            try {
                val mediaUris = listOf(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI to "img",
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI to "vid"
                )

                val allEntities = mutableListOf<MediaEntity>()

                mediaUris.forEach { (uri, typePrefix) ->
                    val cursor = context.contentResolver.query(
                        uri, 
                        arrayOf(
                            MediaStore.MediaColumns._ID,
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            MediaStore.MediaColumns.DATE_ADDED,
                            MediaStore.MediaColumns.MIME_TYPE,
                            MediaStore.MediaColumns.SIZE,
                            MediaStore.MediaColumns.WIDTH,
                            MediaStore.MediaColumns.HEIGHT
                        ), 
                        null, null,
                        "${MediaStore.MediaColumns.DATE_ADDED} DESC"
                    )

                    cursor?.use {
                        val idCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        val nameCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                        val dateCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                        val mimeCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                        val sizeCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                        val widthCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                        val heightCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)

                        while (it.moveToNext()) {
                            val id = it.getLong(idCol)
                            val contentUri = ContentUris.withAppendedId(uri, id).toString()
                            allEntities.add(MediaEntity(
                                id = "${typePrefix}_$id",
                                url = contentUri,
                                thumbnail = contentUri,
                                title = it.getString(nameCol) ?: "Local Media",
                                dateAdded = it.getLong(dateCol) * 1000L,
                                mimeType = it.getString(mimeCol) ?: "image/jpeg",
                                size = it.getLong(sizeCol),
                                width = it.getInt(widthCol),
                                height = it.getInt(heightCol),
                                source = "LOCAL"
                            ))
                        }
                    }
                }
                
                // Un solo golpe a la DB para evitar saturar el canal de invalidación de Paging
                if (allEntities.isNotEmpty()) {
                    mediaDao.insertAll(allEntities)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Sincroniza la galería Cloud (Mock) con Room.
     */
    suspend fun syncCloudGallery() = withContext(Dispatchers.IO) {
        val count = mediaDao.countBySource("CLOUD")
        if (count > 0) return@withContext 

        val allEntities = mutableListOf<MediaEntity>()
        // Reducimos a 100 items: suficiente para demo, ideal para velocidad
        for (i in 0 until 100) {
            allEntities.add(MediaEntity(
                id = "cloud_$i",
                url = "https://picsum.photos/seed/$i/1920/1080",
                thumbnail = "https://picsum.photos/seed/$i/400/400",
                title = "Cloud Image #$i",
                dateAdded = System.currentTimeMillis() - (i * 1000L * 60 * 60 * 5),
                mimeType = "image/jpeg",
                size = (1024 * 1024 * 2.5).toLong() + (i * 1024),
                width = 1920,
                height = 1080,
                source = "CLOUD"
            ))
        }
        
        if (allEntities.isNotEmpty()) {
            mediaDao.insertAll(allEntities)
        }
    }

    /**
     * Obtiene el PagingSource desde Room con soporte para filtros de fecha, tipo y resolución.
     */
    fun getPagedItems(
        source: String, 
        start: Long, 
        end: Long,
        mimeType: String?,
        minWidth: Int = 0,
        minHeight: Int = 0
    ): PagingSource<Int, MediaEntity> {
        val searchMime = mimeType?.let { if (it.contains("/")) it else "%/$it%" } ?: "%"
        return mediaDao.pagingSourceAdvanced(source, start, end, searchMime, minWidth, minHeight)
    }

    /**
     * Obtiene todos los MIME types únicos.
     */
    fun getDistinctMimeTypes(source: String): Flow<List<String>> = mediaDao.getDistinctMimeTypes(source)

    /**
     * Obtiene resoluciones de video únicas.
     */
    fun getAvailableVideoResolutions(source: String): Flow<List<com.my_gallery.data.local.dao.MediaResolution>> = 
        mediaDao.getDistinctVideoResolutions(source)

    /**
     * Limpia la caché local de Room.
     */
    suspend fun clearCache() {
        mediaDao.clearAll()
    }
}

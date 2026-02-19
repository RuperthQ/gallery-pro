package com.my_gallery.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.paging.PagingSource
import com.my_gallery.data.local.dao.MediaDao
import com.my_gallery.data.local.entity.MediaEntity
import com.my_gallery.domain.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

import com.my_gallery.domain.model.AlbumItem
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao
) {

    /**
     * Obtiene los álbumes (carpetas) locales con su miniatura más reciente.
     */
    fun getLocalAlbums(): Flow<List<AlbumItem>> = flow {
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
                        albums[bucketId] = AlbumItem(bucketId, name, contentUri, 0)
                    }
                }
            }
        }
        emit(albums.values.toList().sortedBy { it.name })
    }.flowOn(Dispatchers.IO)

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
                            MediaStore.MediaColumns.HEIGHT,
                            MediaStore.MediaColumns.DATA,
                            MediaStore.MediaColumns.BUCKET_ID
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
                        val dataCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                        val bucketIdCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)

                        while (it.moveToNext()) {
                            val id = it.getLong(idCol)
                            val contentUri = ContentUris.withAppendedId(uri, id).toString()
                            val absolutePath = it.getString(dataCol)
                            val bucketId = it.getString(bucketIdCol)
                            
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
                                source = "LOCAL",
                                path = absolutePath,
                                albumId = bucketId
                            ))
                        }
                    }
                }
                
                // Limpiamos la caché local antes de re-sincronizar para evitar duplicados persistentes
                mediaDao.clearBySource("LOCAL")
                
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
        albumId: String? = null,
        minWidth: Int = 0,
        minHeight: Int = 0
    ): PagingSource<Int, MediaEntity> {
        val searchMime = when {
            mimeType == null || mimeType == "%" || mimeType == "Todas" -> "%"
            mimeType.startsWith("image/") || mimeType.startsWith("video/") -> "$mimeType%"
            !mimeType.contains("/") -> "%${mimeType.lowercase()}%"
            else -> mimeType
        }
        return mediaDao.pagingSourceAdvanced(source, start, end, searchMime, albumId, minWidth, minHeight)
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
     * Renombra un archivo de media.
     * Si es local, intenta actualizar el MediaStore.
     * Siempre actualiza la base de datos local (Room).
     */
    suspend fun renameMedia(item: MediaItem, newNameWithExt: String): RenameResult = withContext(Dispatchers.IO) {
        try {
            if (item.source == "LOCAL") {
                val isManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager()
                } else true

                if (isManager && item.path != null) {
                    // MODO PRO: Renombrado directo vía File (100% Silencioso)
                    val oldFile = java.io.File(item.path)
                    val newFile = java.io.File(oldFile.parent, newNameWithExt)
                    
                    if (oldFile.exists() && oldFile.renameTo(newFile)) {
                        // Notificar al MediaStore para que se entere del cambio físico
                        val values = android.content.ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, newNameWithExt)
                            put(MediaStore.MediaColumns.DATA, newFile.absolutePath)
                        }
                        context.contentResolver.update(android.net.Uri.parse(item.url), values, null, null)
                        
                        // Escaneo forzado para asegurar que MediaStore se actualice
                        android.media.MediaScannerConnection.scanFile(context, arrayOf(newFile.absolutePath), null, null)
                    } else {
                        throw Exception("No se pudo renombrar el archivo físico")
                    }
                } else {
                    // MODO NORMAL: Vía MediaStore (Podría pedir permiso individual)
                    val contentUri = android.net.Uri.parse(item.url)
                    val values = android.content.ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, newNameWithExt)
                    }
                    try {
                        context.contentResolver.update(contentUri, values, null, null)
                    } catch (securityException: SecurityException) {
                        val recoverable = securityException as? android.app.RecoverableSecurityException
                        if (recoverable != null) {
                            return@withContext RenameResult.PermissionRequired(recoverable.userAction.actionIntent.intentSender)
                        } else throw securityException
                    }
                }
            }
            
            mediaDao.updateTitle(item.id, newNameWithExt)
            RenameResult.Success
        } catch (e: Exception) {
            e.printStackTrace()
            RenameResult.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Limpia la caché local de Room.
     */
    suspend fun clearCache() {
        mediaDao.clearAll()
    }
}

sealed class RenameResult {
    object Success : RenameResult()
    data class Error(val message: String) : RenameResult()
    data class PermissionRequired(val intentSender: android.content.IntentSender) : RenameResult()
}

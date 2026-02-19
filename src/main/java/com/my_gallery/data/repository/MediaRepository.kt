package com.my_gallery.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
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
                        albums[bucketId] = AlbumItem(bucketId, name, contentUri, 1)
                    } else {
                        val current = albums[bucketId]!!
                        albums[bucketId] = current.copy(count = current.count + 1)
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
                
                mediaDao.clearBySource("LOCAL")
                
                if (allEntities.isNotEmpty()) {
                    mediaDao.insertAll(allEntities)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

    suspend fun getMediaIds(
        source: String, 
        start: Long, 
        end: Long,
        mimeType: String?,
        albumId: String? = null,
        minWidth: Int = 0,
        minHeight: Int = 0
    ): List<String> {
        val searchMime = when {
            mimeType == null || mimeType == "%" || mimeType == "Todas" -> "%"
            mimeType.startsWith("image/") || mimeType.startsWith("video/") -> "$mimeType%"
            !mimeType.contains("/") -> "%${mimeType.lowercase()}%"
            else -> mimeType
        }
        return mediaDao.getMediaIds(source, start, end, searchMime, albumId, minWidth, minHeight)
    }

    fun getDistinctMimeTypes(source: String): Flow<List<String>> = mediaDao.getDistinctMimeTypes(source)

    fun getAvailableVideoResolutions(source: String): Flow<List<com.my_gallery.data.local.dao.MediaResolution>> = 
        mediaDao.getDistinctVideoResolutions(source)

    fun getAllSectionsMetadata(
        source: String, 
        mimeType: String?,
        albumId: String? = null,
        minWidth: Int = 0,
        minHeight: Int = 0
    ): Flow<List<com.my_gallery.data.local.dao.SectionMetadataRow>> {
        val searchMime = when {
            mimeType == null || mimeType == "%" || mimeType == "Todas" -> "%"
            mimeType.startsWith("image/") || mimeType.startsWith("video/") -> "$mimeType%"
            !mimeType.contains("/") -> "%${mimeType.lowercase()}%"
            else -> mimeType
        }
        return mediaDao.getAllSectionsMetadata(source, searchMime, albumId, minWidth, minHeight)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun renameMedia(item: MediaItem, newNameWithExt: String): RenameResult = withContext(Dispatchers.IO) {
        try {
            if (item.source == "LOCAL") {
                val isManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager()
                } else true

                if (isManager && item.path != null) {
                    val oldFile = java.io.File(item.path)
                    val newFile = java.io.File(oldFile.parent, newNameWithExt)
                    
                    if (oldFile.exists() && oldFile.renameTo(newFile)) {
                        val values = android.content.ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, newNameWithExt)
                            put(MediaStore.MediaColumns.DATA, newFile.absolutePath)
                        }
                        context.contentResolver.update(android.net.Uri.parse(item.url), values, null, null)
                        android.media.MediaScannerConnection.scanFile(context, arrayOf(newFile.absolutePath), null, null)
                    } else {
                        throw Exception("No se pudo renombrar el archivo físico")
                    }
                } else {
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

    suspend fun createAlbum(albumName: String): java.io.File? = withContext(Dispatchers.IO) {
        try {
            val dcim = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM)
            val galleryPro = java.io.File(dcim, "Gallery_Pro")
            val newAlbum = java.io.File(galleryPro, albumName)
            if (!newAlbum.exists()) {
                if (newAlbum.mkdirs()) newAlbum else null
            } else newAlbum
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun moveMediaToAlbum(items: List<MediaItem>, albumName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetDir = createAlbum(albumName) ?: return@withContext false
            var allSuccess = true

            items.forEach { item ->
                if (item.path != null) {
                    val oldFile = java.io.File(item.path)
                    val newFile = java.io.File(targetDir, oldFile.name)

                    if (oldFile.exists() && oldFile.renameTo(newFile)) {
                        // Actualizar MediaStore
                        val values = android.content.ContentValues().apply {
                            put(MediaStore.MediaColumns.DATA, newFile.absolutePath)
                            put(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, albumName)
                            put(MediaStore.MediaColumns.BUCKET_ID, targetDir.absolutePath.hashCode().toString())
                        }
                        context.contentResolver.update(
                            android.net.Uri.parse(item.url),
                            values,
                            null,
                            null
                        )
                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(newFile.absolutePath),
                            null,
                            null
                        )
                        
                        // Actualizar base de datos local
                        mediaDao.updatePathAndAlbum(
                            id = item.id,
                            newPath = newFile.absolutePath,
                            newAlbumId = targetDir.absolutePath.hashCode().toString()
                        )
                    } else {
                        allSuccess = false
                    }
                }
            }
            allSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun deleteMedia(items: List<MediaItem>): DeleteResult = withContext(Dispatchers.IO) {
        try {
            val isManageAllFilesGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else {
                false
            }

            if (isManageAllFilesGranted) {
                var deletedCount = 0
                for (item in items) {
                    try {
                        val deleted = context.contentResolver.delete(android.net.Uri.parse(item.url), null, null)
                        if (deleted > 0) {
                            mediaDao.deleteById(item.id)
                            deletedCount++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return@withContext DeleteResult.Success(deletedCount)
            }

            val uris = items.map { android.net.Uri.parse(it.url) }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+: Batch delete request
                val pi = MediaStore.createDeleteRequest(context.contentResolver, uris)
                return@withContext DeleteResult.PermissionRequired(pi.intentSender)
            } else {
                // API 29 and below: Delete one by one (or batch if possible but usually 1 by 1 w/ recover)
                // For simplicity in this codebase, we try to delete. If exception, return permission (only for the first one for now or loop).
                // But typically API 29 requires permission per file if not owner.
                // Assuming we are owners or have legacy storage.
                
                var deletedCount = 0
                for (item in items) {
                    try {
                        context.contentResolver.delete(android.net.Uri.parse(item.url), null, null)
                        mediaDao.deleteById(item.id)
                        deletedCount++
                    } catch (e: SecurityException) {
                        val recoverable = e as? android.app.RecoverableSecurityException
                        if (recoverable != null) {
                            return@withContext DeleteResult.PermissionRequired(recoverable.userAction.actionIntent.intentSender)
                        } else throw e
                    }
                }
                return@withContext DeleteResult.Success(deletedCount)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DeleteResult.Error(e.message ?: "Error desconocido")
        }
    }

    suspend fun clearCache() {
        mediaDao.clearAll()
    }
}

sealed class RenameResult {
    object Success : RenameResult()
    data class Error(val message: String) : RenameResult()
    data class PermissionRequired(val intentSender: android.content.IntentSender) : RenameResult()
}

sealed class DeleteResult {
    data class Success(val count: Int) : DeleteResult()
    data class Error(val message: String) : DeleteResult()
    data class PermissionRequired(val intentSender: android.content.IntentSender) : DeleteResult()
}

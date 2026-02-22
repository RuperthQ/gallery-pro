package com.my_gallery.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import androidx.paging.PagingSource
import com.my_gallery.data.local.dao.MediaDao
import com.my_gallery.data.local.entity.MediaEntity
import com.my_gallery.data.security.VaultMediaRepository
import com.my_gallery.domain.model.AlbumItem
import com.my_gallery.domain.model.MediaItem
import com.my_gallery.utils.MediaDateParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao,
    private val vaultRepository: VaultMediaRepository
) {

    val mediaChanges: Flow<Unit> = callbackFlow {
        val observer = object : android.database.ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

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
                        val bucketId = dir.absolutePath.hashCode().toString()
                        if (!albums.containsKey(bucketId)) {
                            // Es un álbum vacío
                            albums[bucketId] = AlbumItem(
                                id = bucketId,
                                name = dir.name,
                                thumbnail = "", // No thumbnail
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
                            MediaStore.MediaColumns.BUCKET_ID,
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA
                        ).filterNotNull().toTypedArray(), 
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
                            
                            val originalDate = it.getLong(dateCol) * 1000L
                            val name = it.getString(nameCol) ?: "Local Media"
                            val finalDate = MediaDateParser.parseDateFromFileName(name, originalDate)

                            allEntities.add(MediaEntity(
                                id = "${typePrefix}_$id",
                                url = contentUri,
                                thumbnail = contentUri,
                                title = name,
                                dateAdded = finalDate,
                                mimeType = it.getString(mimeCol) ?: "image/jpeg",
                                size = it.getLong(sizeCol),
                                width = it.getInt(widthCol),
                                height = it.getInt(heightCol),
                                source = "LOCAL",
                                path = absolutePath,
                                albumId = bucketId,
                                relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH))
                                } else {
                                    // Fallback para versiones antiguas: extraer de DATA
                                    try {
                                        val file = java.io.File(absolutePath)
                                        val parent = file.parent ?: ""
                                        if (parent.contains("/0/")) {
                                            parent.substringAfter("/0/").ensureTrailingSlash()
                                        } else parent.ensureTrailingSlash()
                                    } catch (e: Exception) { null }
                                }
                            ))
                        }
                    }
                }
                
                // Sincronización Incremental:
                // 1. Obtener IDs actuales de Room para esta fuente
                val roomIds = mediaDao.getAllIdsBySource("LOCAL").toSet()
                
                // 2. IDs que tenemos ahora desde MediaStore
                val mediaStoreIds = allEntities.map { it.id }.toSet()
                
                // 3. Identificar huérfanos (están en Room pero ya no en MediaStore)
                // Ojo: No borrar los de la bóveda
                val vaultIds = mediaDao.getVaultIds().toSet()
                val orphans = roomIds.filter { it !in mediaStoreIds && it !in vaultIds }
                
                // 4. Recobrar archivos que están en la bóveda física pero no en Room
                syncVaultItems(vaultIds)
                
                // 5. Borrar huérfanos y Upsert de los nuevos/cambiados
                if (orphans.isNotEmpty()) {
                    mediaDao.deleteByIds(orphans)
                }
                
                if (allEntities.isNotEmpty()) {
                    val filteredEntities = allEntities.filterNot { it.id in vaultIds }
                    // Dividir en trozos para evitar límites de SQLITE_MAX_VARIABLE_NUMBER
                    filteredEntities.chunked(500).forEach { chunk ->
                        mediaDao.insertAll(chunk)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getMediaByIds(ids: List<String>): List<MediaItem> = withContext(Dispatchers.IO) {
        mediaDao.getMediaByIds(ids).map { it.toDomain() }
    }

    suspend fun getMediaIdsByPeriod(
        source: String,
        period: String,
        mimeType: String,
        albumId: String? = null
    ): List<String> = withContext(Dispatchers.IO) {
        mediaDao.getMediaIdsByPeriod(source, period, mimeType, albumId)
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

    suspend fun getMediaRank(
        targetId: String,
        source: String, 
        start: Long, 
        end: Long,
        mimeType: String?,
        albumId: String? = null,
        minWidth: Int = 0,
        minHeight: Int = 0
    ): Int {
        val searchMime = when {
            mimeType == null || mimeType == "%" || mimeType == "Todas" -> "%"
            mimeType.startsWith("image/") || mimeType.startsWith("video/") -> "$mimeType%"
            !mimeType.contains("/") -> "%${mimeType.lowercase()}%"
            else -> mimeType
        }
        return mediaDao.getMediaRank(targetId, source, start, end, searchMime, albumId, minWidth, minHeight)
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

    suspend fun updateMediaRotation(item: MediaItem, rotation: Float) = withContext(Dispatchers.IO) {
        // 1. Persistencia en Base de Datos Local
        mediaDao.updateRotation(item.id, rotation)

        // 2. Persistencia Física (EXIF para Imágenes)
        if (item.source == "LOCAL" && item.mimeType.startsWith("image/")) {
            try {
                val filePath = item.path ?: return@withContext
                val file = java.io.File(filePath)
                if (file.exists()) {
                    val exifInterface = ExifInterface(filePath)
                    val exifOrientation = when (rotation) {
                        90f -> ExifInterface.ORIENTATION_ROTATE_90
                        180f -> ExifInterface.ORIENTATION_ROTATE_180
                        270f -> ExifInterface.ORIENTATION_ROTATE_270
                        else -> ExifInterface.ORIENTATION_NORMAL
                    }
                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation.toString())
                    exifInterface.saveAttributes()

                    // 3. Notificar al sistema para actualizar la miniatura de MediaStore
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Images.Media.ORIENTATION, rotation.toInt())
                    }
                    val uri = android.net.Uri.parse(item.url)
                    context.contentResolver.update(uri, contentValues, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    suspend fun moveMediaToAlbum(items: List<MediaItem>, albumName: String, targetAlbumId: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetDir = if (targetAlbumId != null) {
                getAlbumPathById(targetAlbumId) ?: createAlbum(albumName)
            } else {
                createAlbum(albumName)
            } ?: return@withContext false
            
            var allSuccess = true

            items.forEach { item ->
                if (item.albumId == "SECURE_VAULT") {
                    // Si está en la bóveda, la acción de "mover" es en realidad "descifrar hacia la carpeta destino"
                    val restoredUri = vaultRepository.restoreMedia(
                        mediaId = item.id,
                        fileName = if (item.title.startsWith("Recuperado_")) "${item.id}.${if(item.mimeType.contains("video")) "mp4" else "jpg"}" else item.title,
                        mimeType = item.mimeType,
                        originalAlbumId = item.originalAlbumId,
                        targetAlbumName = albumName,
                        originalDate = item.dateAdded
                    )
                    if (restoredUri != null) {
                        mediaDao.deleteById(item.id)
                    } else {
                        allSuccess = false
                    }
                } else if (item.path != null) {
                    val oldFile = java.io.File(item.path)
                    val newFile = java.io.File(targetDir, oldFile.name)

                    if (oldFile.exists() && oldFile.renameTo(newFile)) {
                        // Actualizar MediaStore
                        val values = android.content.ContentValues().apply {
                            put(MediaStore.MediaColumns.DATA, newFile.absolutePath)
                            put(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, albumName)
                            put(MediaStore.MediaColumns.BUCKET_ID, targetDir.absolutePath.hashCode().toString())
                        }
                        try {
                            context.contentResolver.update(
                                android.net.Uri.parse(item.url),
                                values,
                                null,
                                null
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
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
    suspend fun deleteMedia(items: List<MediaItem>, removeFromRoom: Boolean = true): DeleteResult = withContext(Dispatchers.IO) {
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
                            if (removeFromRoom) mediaDao.deleteById(item.id)
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
                        if (removeFromRoom) mediaDao.deleteById(item.id)
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

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun secureMediaItems(items: List<MediaItem>): DeleteResult = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            try {
                val successfullySecured = mutableListOf<MediaItem>()
            
            for (item in items) {
                // 1. Encriptar y guardar en Bóveda Privada
                val vaultFile = vaultRepository.secureMedia(android.net.Uri.parse(item.url), item.id)
                if (vaultFile != null && vaultFile.exists()) {
                    successfullySecured.add(item)
                    // 2. Actualizar BD local apuntando al archivo en la bóveda
                    // Y muy importante: cambiar la URL a vault:// para que Coil la intercepte
                    mediaDao.updatePathAlbumAndUrl(
                        id = item.id,
                        newPath = vaultFile.absolutePath,
                        newAlbumId = "SECURE_VAULT",
                        newUrl = "vault://${item.id}",
                        oldAlbumId = item.albumId,
                        relativePath = item.relativePath
                    )
                }
            }

            if (successfullySecured.isEmpty()) {
                return@withContext DeleteResult.Error("No se pudo asegurar ningún archivo.")
            }

            // 3. Eliminar los archivos públicos originales de MediaStore (sin eliminarlos de Room, porque ahora son SECURE_VAULT)
            return@withContext deleteMedia(successfullySecured, removeFromRoom = false)
            
            } catch (e: Exception) {
                e.printStackTrace()
                return@withLock DeleteResult.Error(e.message ?: "Error al encriptar medios")
            }
        }
    }

    suspend fun unsecureMediaItems(items: List<MediaItem>): DeleteResult = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            try {
                var restoredCount = 0
            
            for (item in items) {
                if (item.albumId == "SECURE_VAULT") {
                    val restoredUri = vaultRepository.restoreMedia(
                        mediaId = item.id, 
                        fileName = item.title, 
                        mimeType = item.mimeType, 
                        originalAlbumId = item.originalAlbumId,
                        targetRelativePath = item.relativePath,
                        originalDate = item.dateAdded
                    )
                    if (restoredUri != null) {
                        restoredCount++
                        // Para simplificar, lo borramos de Room y un resync(o carga lazy) lo actualizará en la UI.
                        mediaDao.deleteById(item.id)
                    }
                }
            }
            
            if (restoredCount == 0) {
                return@withContext DeleteResult.Error("No se pudo restaurar ningún archivo.")
            }
            
                return@withLock DeleteResult.Success(restoredCount)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withLock DeleteResult.Error(e.message ?: "Error al restaurar medios")
            }
        }
    }

    suspend fun clearCache() {
        mediaDao.clearAll()
    }

    suspend fun getSecureVaultCount(): Int = withContext(Dispatchers.IO) {
        mediaDao.getVaultCount()
    }

    suspend fun getSecureVaultThumbnail(): String? = withContext(Dispatchers.IO) {
        mediaDao.getVaultLatestThumbnail()
    }

    suspend fun getLatestPublicThumbnail(): String? = withContext(Dispatchers.IO) {
        mediaDao.getLatestPublicThumbnail()
    }

    private suspend fun syncVaultItems(existingVaultIds: Set<String>) = withContext(Dispatchers.IO) {
        try {
            val files = vaultRepository.getEncryptedFiles()
            val missing = files.filter { it.name.removeSuffix(".enc") !in existingVaultIds }
            
            if (missing.isNotEmpty()) {
                val recoveredEntities = missing.map { file ->
                    val id = file.name.removeSuffix(".enc")
                    val isVideo = id.startsWith("vid")
                    val originalDate = file.lastModified()
                    val finalDate = MediaDateParser.parseDateFromFileName(id, originalDate)

                    MediaEntity(
                        id = id,
                        url = "vault://$id",
                        thumbnail = "vault://$id",
                        title = "Recuperado_$id",
                        dateAdded = finalDate,
                        mimeType = if (isVideo) "video/mp4" else "image/jpeg",
                        size = file.length(),
                        width = 0,
                        height = 0,
                        source = "LOCAL",
                        path = file.absolutePath,
                        albumId = "SECURE_VAULT"
                    )
                }
                mediaDao.insertAll(recoveredEntities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAlbumPathById(albumId: String): java.io.File? {
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

    suspend fun decryptMediaToCache(mediaId: String, mimeType: String): java.io.File? {
        val ext = if (mimeType.contains("/")) mimeType.substringAfterLast("/") else "mp4"
        return vaultRepository.decryptMediaToCache(mediaId, ext)
    }

    suspend fun clearDecryptedCache() {
        withContext(Dispatchers.IO) {
            context.cacheDir.listFiles { file -> file.name.startsWith("decrypted_") }?.forEach {
                it.delete()
            }
        }
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

private fun String.ensureTrailingSlash(): String {
    return if (this.endsWith("/")) this else "$this/"
}

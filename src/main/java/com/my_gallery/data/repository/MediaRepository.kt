package com.my_gallery.data.repository

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.paging.PagingSource
import com.my_gallery.data.local.dao.MediaDao
import com.my_gallery.data.local.dao.FavoriteDao
import com.my_gallery.data.local.entity.FavoriteEntity
import com.my_gallery.data.local.entity.MediaEntity
import com.my_gallery.data.repository.media.*
import com.my_gallery.data.security.VaultMediaRepository
import com.my_gallery.domain.model.AlbumItem
import com.my_gallery.domain.model.MediaItem
import com.my_gallery.domain.model.TrashedMediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio Principal de Medios.
 * Actúa como orquestador delegando en fuentes de datos especializadas.
 */
@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao,
    private val vaultRepository: VaultMediaRepository,
    private val localDataSource: LocalMediaDataSource,
    private val syncHelper: LocalMediaSyncHelper,
    private val fileOperations: LocalMediaFileOperations,
    private val favoriteDao: FavoriteDao,
    private val trashDataSource: TrashDataSource
) {

    /**
     * Observa cambios en el MediaStore para disparar sincronizaciones.
     */
    val mediaChanges: Flow<Unit> = callbackFlow {
        val observer = object : android.database.ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        context.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)

        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.flowOn(Dispatchers.IO)

    // --- CONSULTAS ---

    fun getLocalAlbums(includeEmpty: Boolean = false): Flow<List<AlbumItem>> = 
        localDataSource.getLocalAlbums(includeEmpty)

    /**
     * Elimina la carpeta física de un álbum.
     * Los archivos NO se borran — solo la carpeta si está vacía.
     * Si la carpeta tiene archivos, el MediaStore simplemente deja de agruparlos.
     */
    suspend fun deleteAlbumFolder(albumId: String): Boolean = withContext(Dispatchers.IO) {
        val folder = localDataSource.getAlbumPathById(albumId) ?: return@withContext false
        try {
            folder.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }



    suspend fun getMediaByIds(ids: List<String>): List<MediaItem> = withContext(Dispatchers.IO) {
        mediaDao.getMediaByIds(ids).map { it.toDomain() }
    }

    suspend fun getMediaByUrl(url: String): MediaItem? = withContext(Dispatchers.IO) {
        mediaDao.getMediaByUrl(url)?.toDomain()
    }

    suspend fun getMediaFromExternalUri(uri: android.net.Uri, mimeType: String?): MediaItem? = withContext(Dispatchers.IO) {
        // 1. Intentar buscar en DB primero
        val existing = mediaDao.getMediaByUrl(uri.toString())?.toDomain()
        if (existing != null) return@withContext existing

        // 2. Si no está en DB, consultar ContentResolver (MediaStore)
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DATE_ADDED
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                    val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                    val sizeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    val widthIdx = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                    val heightIdx = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                    val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    val dateAddedIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)

                    val id = if (idIdx != -1) cursor.getLong(idIdx) else uri.hashCode().toLong()
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) else "External Media"
                    val mime = if (mimeIdx != -1) cursor.getString(mimeIdx) ?: mimeType ?: "image/jpeg" else mimeType ?: "image/jpeg"
                    val path = if (dataIdx != -1) cursor.getString(dataIdx) else null
                    val dateAdded = if (dateAddedIdx != -1) cursor.getLong(dateAddedIdx) * 1000L else System.currentTimeMillis()

                    return@withContext MediaItem(
                        id = "ext_$id",
                        url = uri.toString(),
                        thumbnail = uri.toString(),
                        title = name,
                        dateAdded = dateAdded,
                        mimeType = mime,
                        size = if (sizeIdx != -1) cursor.getLong(sizeIdx) else 0L,
                        width = if (widthIdx != -1) cursor.getInt(widthIdx) else 0,
                        height = if (heightIdx != -1) cursor.getInt(heightIdx) else 0,
                        path = path,
                        source = "EXTERNAL"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Fallback: Crear item básico si el cursor falla pero tenemos el URI
        return@withContext MediaItem(
            id = "ext_${uri.hashCode()}",
            url = uri.toString(),
            thumbnail = uri.toString(),
            title = "External Media",
            dateAdded = System.currentTimeMillis(),
            mimeType = mimeType ?: "image/jpeg",
            source = "EXTERNAL"
        )
    }

    suspend fun getMediaIdsByPeriod(
        source: String,
        period: String,
        mimeType: String,
        albumId: String? = null
    ): List<String> = withContext(Dispatchers.IO) {
        mediaDao.getMediaIdsByPeriod(source, period, mimeType, albumId)
    }

    fun getPagedItemsMultiFilter(
        source: String, 
        periods: List<String>,
        types: List<String>,
        extensions: List<String>,
        resolutions: List<String>,
        albumId: String? = null
    ): PagingSource<Int, MediaEntity> =
        mediaDao.pagingSourceMultiFilter(
            source = source,
            useDateFilter = if (periods.isNotEmpty()) 1 else 0,
            periods = periods,
            useTypeFilter = if (types.isNotEmpty()) 1 else 0,
            includeImages = if (types.contains("Imágenes")) 1 else 0,
            includeVideos = if (types.contains("Videos")) 1 else 0,
            useMimeFilter = if (extensions.isNotEmpty()) 1 else 0,
            mimeTypes = extensions.map { formatMimeForIn(it) },
            useResFilter = if (resolutions.isNotEmpty()) 1 else 0,
            use4K = if (resolutions.contains("4K")) 1 else 0,
            use2K = if (resolutions.contains("2K")) 1 else 0,
            use1080P = if (resolutions.contains("1080P")) 1 else 0,
            use720P = if (resolutions.contains("720P")) 1 else 0,
            useSD = if (resolutions.contains("SD")) 1 else 0,
            albumId = albumId
        )

    fun getAllSectionsMetadataMultiFilter(
        source: String, 
        types: List<String>,
        extensions: List<String>,
        resolutions: List<String>,
        albumId: String? = null
    ): Flow<List<com.my_gallery.data.local.dao.SectionMetadataRow>> =
        mediaDao.getAllSectionsMetadataMultiFilter(
            source = source,
            useTypeFilter = if (types.isNotEmpty()) 1 else 0,
            includeImages = if (types.contains("Imágenes")) 1 else 0,
            includeVideos = if (types.contains("Videos")) 1 else 0,
            useMimeFilter = if (extensions.isNotEmpty()) 1 else 0,
            mimeTypes = extensions.map { formatMimeForIn(it) },
            useResFilter = if (resolutions.isNotEmpty()) 1 else 0,
            use4K = if (resolutions.contains("4K")) 1 else 0,
            use2K = if (resolutions.contains("2K")) 1 else 0,
            use1080P = if (resolutions.contains("1080P")) 1 else 0,
            use720P = if (resolutions.contains("720P")) 1 else 0,
            useSD = if (resolutions.contains("SD")) 1 else 0,
            albumId = albumId
        )

    suspend fun getMediaRankMultiFilter(
        targetId: String,
        source: String, 
        periods: List<String>,
        types: List<String>,
        extensions: List<String>,
        resolutions: List<String>,
        albumId: String? = null
    ): Int =
        mediaDao.getMediaRankMultiFilter(
            targetId = targetId,
            source = source,
            useDateFilter = if (periods.isNotEmpty()) 1 else 0,
            periods = periods,
            useTypeFilter = if (types.isNotEmpty()) 1 else 0,
            includeImages = if (types.contains("Imágenes")) 1 else 0,
            includeVideos = if (types.contains("Videos")) 1 else 0,
            useMimeFilter = if (extensions.isNotEmpty()) 1 else 0,
            mimeTypes = extensions.map { formatMimeForIn(it) },
            useResFilter = if (resolutions.isNotEmpty()) 1 else 0,
            use4K = if (resolutions.contains("4K")) 1 else 0,
            use2K = if (resolutions.contains("2K")) 1 else 0,
            use1080P = if (resolutions.contains("1080P")) 1 else 0,
            use720P = if (resolutions.contains("720P")) 1 else 0,
            useSD = if (resolutions.contains("SD")) 1 else 0,
            albumId = albumId
        )

    fun getDistinctMimeTypes(source: String): Flow<List<String>> = 
        mediaDao.getDistinctMimeTypes(source)

    fun getAvailableVideoResolutions(source: String) = 
        mediaDao.getDistinctVideoResolutions(source)

    suspend fun getMediaIdsMultiFilter(
        source: String, 
        periods: List<String>,
        types: List<String>,
        extensions: List<String>,
        resolutions: List<String>,
        albumId: String? = null
    ): List<String> =
        mediaDao.getMediaIdsMultiFilter(
            source = source,
            useDateFilter = if (periods.isNotEmpty()) 1 else 0,
            periods = periods,
            useTypeFilter = if (types.isNotEmpty()) 1 else 0,
            includeImages = if (types.contains("Imágenes")) 1 else 0,
            includeVideos = if (types.contains("Videos")) 1 else 0,
            useMimeFilter = if (extensions.isNotEmpty()) 1 else 0,
            mimeTypes = extensions.map { formatMimeForIn(it) },
            useResFilter = if (resolutions.isNotEmpty()) 1 else 0,
            use4K = if (resolutions.contains("4K")) 1 else 0,
            use2K = if (resolutions.contains("2K")) 1 else 0,
            use1080P = if (resolutions.contains("1080P")) 1 else 0,
            use720P = if (resolutions.contains("720P")) 1 else 0,
            useSD = if (resolutions.contains("SD")) 1 else 0,
            albumId = albumId
        )

    private fun formatMimeForIn(ext: String): String {
        val videoExts = listOf("MP4", "MKV", "WEBM", "TS", "3GP", "MOV", "AVI")
        return when {
            ext.startsWith("image/") || ext.startsWith("video/") -> ext
            videoExts.contains(ext.uppercase()) -> "video/${ext.lowercase()}"
            else -> "image/${ext.lowercase()}"
        }
    }

    // --- OPERACIONES DE SINCRONIZACIÓN ---

    suspend fun syncLocalGallery(force: Boolean = false) = 
        syncHelper.syncLocalGallery(force)

    // --- OPERACIONES DE ARCHIVOS ---

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun renameMedia(item: MediaItem, newName: String) = 
        fileOperations.renameMedia(item, newName)

    suspend fun updateMediaRotation(item: MediaItem, rotation: Float) = 
        fileOperations.updateMediaRotation(item, rotation)

    suspend fun setWallpaper(item: MediaItem): Boolean = 
        fileOperations.setWallpaper(item)

    suspend fun createAlbum(name: String) = 
        fileOperations.createAlbum(name)

    suspend fun moveMediaToAlbum(items: List<MediaItem>, name: String, targetId: String? = null) = 
        fileOperations.moveMediaToAlbum(items, name, targetId)

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun deleteMedia(items: List<MediaItem>, removeFromRoom: Boolean = true) = 
        fileOperations.deleteMedia(items, removeFromRoom)

    // --- FAVORITOS ---

    suspend fun toggleFavorite(id: String) = withContext(Dispatchers.IO) {
        if (favoriteDao.isFavorite(id)) {
            favoriteDao.delete(id)
        } else {
            favoriteDao.insert(FavoriteEntity(id, System.currentTimeMillis()))
        }
    }

    suspend fun getFavoritesCount(): Int = favoriteDao.getCount()
    
    fun getFavoritesCountFlow(): Flow<Int> = favoriteDao.getCountFlow()

    suspend fun getFavoritesThumbnail(): String? = favoriteDao.getLatestThumbnail()

    fun getFavoritesThumbnailFlow(): Flow<String?> = favoriteDao.getLatestThumbnailFlow()
    
    fun isFavorite(id: String): Flow<Boolean> = favoriteDao.isFavoriteFlow(id)

    // --- PAPELERA ---

    fun getTrashedMedia() = trashDataSource.getTrashedMedia()
    val isTrashedSupported: Boolean get() = trashDataSource.isSupported
    suspend fun getTrashedCount(): Int = trashDataSource.getTrashedCount()
    suspend fun getTrashedThumbnail(): String? = trashDataSource.getTrashedThumbnail()
    suspend fun restoreFromTrash(item: TrashedMediaItem): Boolean = trashDataSource.restoreFromTrash(item)
    suspend fun deletePermanently(item: TrashedMediaItem): Boolean = trashDataSource.deletePermanently(item)
    suspend fun deletePermanentlyAll(items: List<TrashedMediaItem>): Int =
        items.count { trashDataSource.deletePermanently(it) }

    // --- SEGURIDAD / BÓVEDA ---

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun secureMediaItems(items: List<MediaItem>): DeleteResult = withContext(Dispatchers.IO) {
        try {
            val successfullySecured = mutableListOf<MediaItem>()
            for (item in items) {
                val vaultFile = vaultRepository.secureMedia(android.net.Uri.parse(item.url), item.id)
                if (vaultFile != null && vaultFile.exists()) {
                    successfullySecured.add(item)
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
            if (successfullySecured.isEmpty()) return@withContext DeleteResult.Error("No se pudo asegurar ningún archivo.")
            fileOperations.deleteMedia(successfullySecured, removeFromRoom = false)
        } catch (e: Exception) {
            e.printStackTrace()
            DeleteResult.Error(e.message ?: "Error al encriptar medios")
        }
    }

    suspend fun unsecureMediaItems(items: List<MediaItem>): DeleteResult = withContext(Dispatchers.IO) {
        try {
            var restoredCount = 0
            for (item in items) {
                if (item.albumId == "SECURE_VAULT") {
                    val restoredUri = vaultRepository.restoreMedia(
                        mediaId = item.id, fileName = item.title, mimeType = item.mimeType, 
                        originalAlbumId = item.originalAlbumId, targetRelativePath = item.relativePath,
                        originalDate = item.dateAdded
                    )
                    if (restoredUri != null) {
                        restoredCount++
                        mediaDao.deleteById(item.id)
                    }
                }
            }
            if (restoredCount == 0) return@withContext DeleteResult.Error("No se pudo restaurar ningún archivo.")
            DeleteResult.Success(restoredCount)
        } catch (e: Exception) {
            e.printStackTrace()
            DeleteResult.Error(e.message ?: "Error al restaurar medios")
        }
    }

    suspend fun getSecureVaultCount(): Int = mediaDao.getVaultCount()

    suspend fun getSecureVaultThumbnail(): String? = mediaDao.getVaultLatestThumbnail()

    suspend fun getLatestPublicThumbnail(): String? = mediaDao.getLatestPublicThumbnail()

    suspend fun decryptMediaToCache(mediaId: String, mimeType: String): java.io.File? {
        val ext = if (mimeType.contains("/")) mimeType.substringAfterLast("/") else "mp4"
        return vaultRepository.decryptMediaToCache(mediaId, ext)
    }

    suspend fun clearDecryptedCache() = withContext(Dispatchers.IO) {
        context.cacheDir.listFiles { file -> file.name.startsWith("decrypted_") }?.forEach { it.delete() }
    }

    suspend fun clearCache() = mediaDao.clearAll()

    // --- UTILIDADES ---

    private fun formatMime(mimeType: String?): String {
        return when {
            mimeType == null || mimeType == "%" || mimeType == "Todas" -> "%"
            mimeType.startsWith("image/") || mimeType.startsWith("video/") -> "$mimeType%"
            !mimeType.contains("/") -> "%${mimeType.lowercase()}%"
            else -> mimeType
        }
    }
}

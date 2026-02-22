package com.my_gallery.data.repository

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.paging.PagingSource
import com.my_gallery.data.local.dao.MediaDao
import com.my_gallery.data.local.entity.MediaEntity
import com.my_gallery.data.repository.media.*
import com.my_gallery.data.security.VaultMediaRepository
import com.my_gallery.domain.model.AlbumItem
import com.my_gallery.domain.model.MediaItem
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
    private val fileOperations: LocalMediaFileOperations
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
        source: String, start: Long, end: Long, mimeType: String?,
        albumId: String? = null, minWidth: Int = 0, minHeight: Int = 0
    ): PagingSource<Int, MediaEntity> =
        mediaDao.pagingSourceAdvanced(source, start, end, formatMime(mimeType), albumId, minWidth, minHeight)

    suspend fun getMediaIds(
        source: String, start: Long, end: Long, mimeType: String?,
        albumId: String? = null, minWidth: Int = 0, minHeight: Int = 0
    ): List<String> =
        mediaDao.getMediaIds(source, start, end, formatMime(mimeType), albumId, minWidth, minHeight)

    suspend fun getMediaRank(
        targetId: String, source: String, start: Long, end: Long, mimeType: String?,
        albumId: String? = null, minWidth: Int = 0, minHeight: Int = 0
    ): Int =
        mediaDao.getMediaRank(targetId, source, start, end, formatMime(mimeType), albumId, minWidth, minHeight)

    fun getDistinctMimeTypes(source: String): Flow<List<String>> = 
        mediaDao.getDistinctMimeTypes(source)

    fun getAvailableVideoResolutions(source: String) = 
        mediaDao.getDistinctVideoResolutions(source)

    fun getAllSectionsMetadata(
        source: String, mimeType: String?, albumId: String? = null,
        minWidth: Int = 0, minHeight: Int = 0
    ): Flow<List<com.my_gallery.data.local.dao.SectionMetadataRow>> =
        mediaDao.getAllSectionsMetadata(source, formatMime(mimeType), albumId, minWidth, minHeight)

    // --- OPERACIONES DE SINCRONIZACIÓN ---

    suspend fun syncLocalGallery(force: Boolean = false) = 
        syncHelper.syncLocalGallery(force)

    // --- OPERACIONES DE ARCHIVOS ---

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun renameMedia(item: MediaItem, newName: String) = 
        fileOperations.renameMedia(item, newName)

    suspend fun updateMediaRotation(item: MediaItem, rotation: Float) = 
        fileOperations.updateMediaRotation(item, rotation)

    suspend fun createAlbum(name: String) = 
        fileOperations.createAlbum(name)

    suspend fun moveMediaToAlbum(items: List<MediaItem>, name: String, targetId: String? = null) = 
        fileOperations.moveMediaToAlbum(items, name, targetId)

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun deleteMedia(items: List<MediaItem>, removeFromRoom: Boolean = true) = 
        fileOperations.deleteMedia(items, removeFromRoom)

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

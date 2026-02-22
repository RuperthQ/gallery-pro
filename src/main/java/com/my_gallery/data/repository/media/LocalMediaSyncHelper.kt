package com.my_gallery.data.repository.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.my_gallery.data.local.dao.MediaDao
import com.my_gallery.data.local.entity.MediaEntity
import com.my_gallery.data.security.VaultMediaRepository
import com.my_gallery.utils.MediaDateParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ayudante encargado de la sincronización entre MediaStore, la Bóveda y Room.
 */
@Singleton
class LocalMediaSyncHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao,
    private val vaultRepository: VaultMediaRepository
) {
    private val syncMutex = Mutex()

    /**
     * Sincroniza la galería local con la base de datos Room.
     */
    suspend fun syncLocalGallery(force: Boolean = false) = withContext(Dispatchers.IO) {
        if (syncMutex.isLocked) return@withContext
        syncMutex.withLock {
            try {
                if (force) {
                    mediaDao.clearBySource("LOCAL")
                }
                
                val mediaUris = listOf(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI to "img",
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI to "vid"
                )

                val allEntities = mutableListOf<MediaEntity>()

                mediaUris.forEach { (uri, typePrefix) ->
                    val projection = mutableListOf(
                        MediaStore.MediaColumns._ID,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.DATE_ADDED,
                        MediaStore.MediaColumns.DATE_MODIFIED,
                        MediaStore.MediaColumns.MIME_TYPE,
                        MediaStore.MediaColumns.SIZE,
                        MediaStore.MediaColumns.WIDTH,
                        MediaStore.MediaColumns.HEIGHT,
                        MediaStore.MediaColumns.DATA,
                        MediaStore.MediaColumns.BUCKET_ID
                    )
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        projection.add(MediaStore.MediaColumns.RELATIVE_PATH)
                    }

                    // Intentar añadir DATE_TAKEN si existe, si no, fallar amistosamente
                    val dateTakenKey = if (uri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                        "datetaken"
                    } else {
                        "datetaken" // En videos suele llamarse igual o similar
                    }
                    projection.add(dateTakenKey)

                    val cursor = try {
                        context.contentResolver.query(
                            uri, 
                            projection.toTypedArray(), 
                            null, null,
                            "$dateTakenKey DESC, ${MediaStore.MediaColumns.DATE_ADDED} DESC"
                        )
                    } catch (e: Exception) {
                        // Re-intentar sin datetaken si falla
                        projection.remove(dateTakenKey)
                        context.contentResolver.query(
                            uri, projection.toTypedArray(), null, null,
                            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
                        )
                    }

                    cursor?.use {
                        val idCol = it.getColumnIndex(MediaStore.MediaColumns._ID)
                        val nameCol = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                        val mimeCol = it.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                        val sizeCol = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
                        val widthCol = it.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                        val heightCol = it.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                        val dataCol = it.getColumnIndex(MediaStore.MediaColumns.DATA)
                        val bucketIdCol = it.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
                        val dateAddedCol = it.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                        val dateModCol = it.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                        val dateTakenCol = it.getColumnIndex(dateTakenKey)
                        val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            it.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                        } else -1

                        while (it.moveToNext()) {
                            val id = if (idCol != -1) it.getLong(idCol) else continue
                            val contentUri = ContentUris.withAppendedId(uri, id).toString()
                            val absolutePath = if (dataCol != -1) it.getString(dataCol) else null
                            val bucketId = if (bucketIdCol != -1) it.getString(bucketIdCol) else null
                            
                            val dateAddedSec = if (dateAddedCol != -1) it.getLong(dateAddedCol) else 0L
                            val dateModifiedSec = if (dateModCol != -1) it.getLong(dateModCol) else 0L
                            val dateTakenMs = if (dateTakenCol != -1) it.getLong(dateTakenCol) else 0L
                            
                            val timestamps = listOf(
                                dateTakenMs,
                                dateAddedSec * 1000L,
                                dateModifiedSec * 1000L
                            )
                            
                            var rawTimestampMs = timestamps.find { it > 946684800000L } ?: 0L
                            
                            if (rawTimestampMs < 946684800000L && absolutePath != null) {
                                try {
                                    val file = java.io.File(absolutePath)
                                    if (file.exists()) {
                                        val lastMod = file.lastModified()
                                        if (lastMod > 946684800000L) rawTimestampMs = lastMod
                                    }
                                } catch (e: Exception) {}
                            }

                            val name = if (nameCol != -1) it.getString(nameCol) ?: "Local Media" else "Local Media"
                            val finalDate = MediaDateParser.parseDateFromFileName(name, rawTimestampMs)
                            val resultDate = if (finalDate < 946684800000L && dateAddedSec > 0) dateAddedSec * 1000L else finalDate

                            allEntities.add(MediaEntity(
                                id = "${typePrefix}_$id",
                                url = contentUri,
                                thumbnail = contentUri,
                                title = name,
                                dateAdded = resultDate,
                                mimeType = if (mimeCol != -1) it.getString(mimeCol) ?: "image/jpeg" else "image/jpeg",
                                size = if (sizeCol != -1) it.getLong(sizeCol) else 0L,
                                width = if (widthCol != -1) it.getInt(widthCol) else 0,
                                height = if (heightCol != -1) it.getInt(heightCol) else 0,
                                source = "LOCAL",
                                path = absolutePath,
                                albumId = bucketId,
                                relativePath = if (relPathCol != -1) {
                                    it.getString(relPathCol)
                                } else {
                                    extractRelativePath(absolutePath)
                                }
                            ))
                        }
                    }
                }
                
                val roomIds = mediaDao.getAllIdsBySource("LOCAL").toSet()
                val mediaStoreIds = allEntities.map { it.id }.toSet()
                val vaultIds = mediaDao.getVaultIds().toSet()
                val orphans = roomIds.filter { it !in mediaStoreIds && it !in vaultIds }
                
                syncVaultItems(vaultIds)
                
                if (orphans.isNotEmpty()) {
                    mediaDao.deleteByIds(orphans)
                }
                
                if (allEntities.isNotEmpty()) {
                    val filteredEntities = allEntities.filterNot { it.id in vaultIds }
                    filteredEntities.chunked(500).forEach { chunk ->
                        mediaDao.insertAll(chunk)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

    private fun extractRelativePath(absolutePath: String?): String? {
        if (absolutePath == null) return null
        return try {
            val file = java.io.File(absolutePath)
            val parent = file.parent ?: ""
            if (parent.contains("/0/")) {
                parent.substringAfter("/0/").ensureTrailingSlash()
            } else parent.ensureTrailingSlash()
        } catch (e: Exception) { null }
    }

    private fun String.ensureTrailingSlash(): String {
        return if (this.endsWith("/")) this else "$this/"
    }
}

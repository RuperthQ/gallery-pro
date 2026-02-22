package com.my_gallery.data.repository.media

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import com.my_gallery.data.local.dao.MediaDao
import com.my_gallery.data.security.VaultMediaRepository
import com.my_gallery.domain.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encargado de las operaciones de escritura y manipulación de archivos (IO + MediaStore + DB).
 */
@Singleton
class LocalMediaFileOperations @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao,
    private val vaultRepository: VaultMediaRepository,
    private val localDataSource: LocalMediaDataSource
) {
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun renameMedia(item: MediaItem, newNameWithExt: String): RenameResult = withContext(Dispatchers.IO) {
        try {
            if (item.source == "LOCAL") {
                val isManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager()
                } else true

                if (isManager && item.path != null) {
                    val oldFile = java.io.File(item.path)
                    val newFile = java.io.File(oldFile.parent, newNameWithExt)
                    
                    if (oldFile.exists() && oldFile.renameTo(newFile)) {
                        val values = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, newNameWithExt)
                            put(MediaStore.MediaColumns.DATA, newFile.absolutePath)
                        }
                        context.contentResolver.update(Uri.parse(item.url), values, null, null)
                        MediaScannerConnection.scanFile(context, arrayOf(newFile.absolutePath), null, null)
                    } else {
                        throw Exception("No se pudo renombrar el archivo físico")
                    }
                } else {
                    val contentUri = Uri.parse(item.url)
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, newNameWithExt)
                    }
                    try {
                        context.contentResolver.update(contentUri, values, null, null)
                    } catch (securityException: SecurityException) {
                        val recoverable = securityException as? RecoverableSecurityException
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
        mediaDao.updateRotation(item.id, rotation)

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

                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.ORIENTATION, rotation.toInt())
                    }
                    val uri = Uri.parse(item.url)
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
                localDataSource.getAlbumPathById(targetAlbumId) ?: createAlbum(albumName)
            } else {
                createAlbum(albumName)
            } ?: return@withContext false
            
            var allSuccess = true

            items.forEach { item ->
                if (item.albumId == "SECURE_VAULT") {
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
                        val values = ContentValues().apply {
                            put(MediaStore.MediaColumns.DATA, newFile.absolutePath)
                            put(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, targetDir.name)
                            put(MediaStore.MediaColumns.BUCKET_ID, targetDir.absolutePath.lowercase().hashCode().toString())
                        }
                        try {
                            context.contentResolver.update(Uri.parse(item.url), values, null, null)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        MediaScannerConnection.scanFile(context, arrayOf(newFile.absolutePath), null, null)
                        
                        mediaDao.updatePathAndAlbum(
                            id = item.id,
                            newPath = newFile.absolutePath,
                            newAlbumId = targetDir.absolutePath.lowercase().hashCode().toString()
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
            } else false

            if (isManageAllFilesGranted) {
                var deletedCount = 0
                for (item in items) {
                    try {
                        val deleted = context.contentResolver.delete(Uri.parse(item.url), null, null)
                        if (deleted > 0) {
                            if (removeFromRoom) mediaDao.deleteById(item.id)
                            deletedCount++
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                return@withContext DeleteResult.Success(deletedCount)
            }

            val uris = items.map { Uri.parse(it.url) }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pi = MediaStore.createDeleteRequest(context.contentResolver, uris)
                return@withContext DeleteResult.PermissionRequired(pi.intentSender)
            } else {
                var deletedCount = 0
                for (item in items) {
                    try {
                        context.contentResolver.delete(Uri.parse(item.url), null, null)
                        if (removeFromRoom) mediaDao.deleteById(item.id)
                        deletedCount++
                    } catch (e: SecurityException) {
                        val recoverable = e as? RecoverableSecurityException
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
}

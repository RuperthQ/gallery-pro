package com.my_gallery.data.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class VaultMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cryptoManager = CryptoManager()
    
    // Carpeta privada dentro de app data que ninguna otra app puede ver
    private val vaultDir: File = File(context.filesDir, "vault").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Mueve (Cifra) un archivo de MediaStore hacia la Bóveda Privada.
     */
    suspend fun secureMedia(originalUri: Uri, mediaId: String): File? = withContext(Dispatchers.IO) {
        try {
            val secureFile = File(vaultDir, "$mediaId.enc")
            val thumbFile = File(vaultDir, "$mediaId.thumb")
            
            // 1. Cifrar archivo original
            context.contentResolver.openInputStream(originalUri)?.use { inputStream ->
                FileOutputStream(secureFile).use { outputStream ->
                    cryptoManager.encrypt(inputStream, outputStream)
                }
            }

            // 2. Generar y cifrar miniatura para carga rápida
            generateAndSecureThumbnail(originalUri, thumbFile)
            
            return@withContext secureFile
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    private fun generateAndSecureThumbnail(uri: Uri, targetFile: File) {
        try {
            val size = android.util.Size(512, 512)
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                try {
                    context.contentResolver.loadThumbnail(uri, size, null)
                } catch (e: Exception) {
                    null
                }
            } else null

            val finalBitmap = bitmap ?: context.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it)
            }

            finalBitmap?.let { b ->
                val stream = java.io.ByteArrayOutputStream()
                b.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                val bytes = stream.toByteArray()
                FileOutputStream(targetFile).use { fos ->
                    cryptoManager.encrypt(bytes, fos)
                }
                b.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Descifra a memoria los bytes de un archivo seguro.
     */
    suspend fun decryptMediaBytes(mediaId: String): ByteArray? = withContext(Dispatchers.IO) {
        val secureFile = File(vaultDir, "$mediaId.enc")
        if (!secureFile.exists()) return@withContext null

        return@withContext try {
            FileInputStream(secureFile).use { inputStream ->
                cryptoManager.decrypt(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Descifra la miniatura optimizada.
     */
    suspend fun decryptThumbnailBytes(mediaId: String): ByteArray? = withContext(Dispatchers.IO) {
        val thumbFile = File(vaultDir, "$mediaId.thumb")
        if (!thumbFile.exists()) return@withContext null

        return@withContext try {
            FileInputStream(thumbFile).use { inputStream ->
                cryptoManager.decrypt(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Genera bajo demanda y guarda una miniatura para archivos que no la tengan (migración transparente).
     */
    suspend fun generateAndCacheThumbnailForVaultItem(mediaId: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val isVideo = mediaId.startsWith("vid")
            val originalBitmap = if (isVideo) {
                // Para videos, desciframos a temporal y extraemos frame
                val tempFile = decryptMediaToCache(mediaId, "mp4") ?: return@withContext null
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    retriever.setDataSource(tempFile.absolutePath)
                    retriever.getFrameAtTime(0)
                } finally {
                    retriever.release()
                    tempFile.delete()
                }
            } else {
                val originalBytes = decryptMediaBytes(mediaId) ?: return@withContext null
                BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
            } ?: return@withContext null

            // Calcular dimensiones manteniendo aspecto
            val targetSize = 512
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > height) targetSize.toFloat() / width else targetSize.toFloat() / height
            
            val scaledBitmap = Bitmap.createScaledBitmap(
                originalBitmap, 
                (width * scale).toInt(), 
                (height * scale).toInt(), 
                true
            )

            val stream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            val thumbBytes = stream.toByteArray()

            // Guardar para futuras cargas
            val thumbFile = File(vaultDir, "$mediaId.thumb")
            FileOutputStream(thumbFile).use { fos ->
                cryptoManager.encrypt(thumbBytes, fos)
            }

            if (originalBitmap != scaledBitmap) originalBitmap.recycle()
            scaledBitmap.recycle()

            return@withContext thumbBytes
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
    
    /**
     * Saca el archivo de la bóveda (descifra) y lo guarda en cache temporal
     * Util para videos grandes o share intent
     */
     suspend fun decryptMediaToCache(mediaId: String, extension: String = "jpg"): File? = withContext(Dispatchers.IO) {
         val bytes = decryptMediaBytes(mediaId) ?: return@withContext null
         try {
             val tempFile = File(context.cacheDir, "decrypted_$mediaId.$extension")
             FileOutputStream(tempFile).use {
                 it.write(bytes)
             }
             return@withContext tempFile
         } catch(e: Exception) {
             e.printStackTrace()
             return@withContext null
         }
     }
     
     fun isMediaSecured(mediaId: String): Boolean {
         return File(vaultDir, "$mediaId.enc").exists()
     }

    /**
     * Descifra un archivo de la Bóveda y lo restaura en el almacenamiento público (MediaStore).
     */
    suspend fun restoreMedia(
        mediaId: String, 
        fileName: String, 
        mimeType: String, 
        originalAlbumId: String? = null,
        targetAlbumName: String? = null,
        targetRelativePath: String? = null,
        originalDate: Long? = null
    ): Uri? = withContext(Dispatchers.IO) {
        val secureFile = File(vaultDir, "$mediaId.enc")
        if (!secureFile.exists()) return@withContext null

        val isVideo = mimeType.startsWith("video/")
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (isVideo) android.provider.MediaStore.Video.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            if (isVideo) android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        // Determinar ruta relativa
        val finalRelativePath = when {
            !targetRelativePath.isNullOrBlank() -> targetRelativePath
            !targetAlbumName.isNullOrBlank() -> {
                val baseDir = if (isVideo) android.os.Environment.DIRECTORY_DCIM else android.os.Environment.DIRECTORY_PICTURES
                "$baseDir/$targetAlbumName"
            }
            else -> {
                val baseDir = if (isVideo) android.os.Environment.DIRECTORY_DCIM else android.os.Environment.DIRECTORY_PICTURES
                "$baseDir/Gallery_Pro"
            }
        }

        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
            
            originalDate?.let { dateMs ->
                // MediaStore suele usar SEGUNDOS para DATE_ADDED y MILISEGUNDOS para DATE_TAKEN
                val dateSec = dateMs / 1000L
                put(android.provider.MediaStore.MediaColumns.DATE_ADDED, dateSec)
                put(android.provider.MediaStore.MediaColumns.DATE_MODIFIED, dateSec)
                
                if (isVideo) {
                    put(android.provider.MediaStore.Video.VideoColumns.DATE_TAKEN, dateMs)
                } else {
                    put(android.provider.MediaStore.Images.ImageColumns.DATE_TAKEN, dateMs)
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, finalRelativePath)
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        try {
            val restoredUri = context.contentResolver.insert(collection, values) ?: return@withContext null

            context.contentResolver.openOutputStream(restoredUri)?.use { outputStream ->
                FileInputStream(secureFile).use { inputStream ->
                    cryptoManager.decryptRestoring(inputStream, outputStream)
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.clear()
                values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(restoredUri, values, null, null)
            }

            // Una vez restaurado con éxito, lo eliminamos de la bóveda
            secureFile.delete()

            return@withContext restoredUri
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun getEncryptedFiles(): List<File> = withContext(Dispatchers.IO) {
        vaultDir.listFiles { file -> file.name.endsWith(".enc") }?.toList() ?: emptyList()
    }
}

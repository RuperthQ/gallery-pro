package com.my_gallery.data.repository.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.my_gallery.domain.model.TrashedMediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos para archivos eliminados (papelera de MediaStore).
 * Solo disponible en API 30 (Android 11) o superior.
 */
@Singleton
class TrashDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /**
     * Obtiene la lista de archivos en la papelera del sistema.
     */
    fun getTrashedMedia(): Flow<List<TrashedMediaItem>> = flow {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            emit(emptyList())
            return@flow
        }

        val items = mutableListOf<TrashedMediaItem>()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_EXPIRES,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT
        )

        val baseUris = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )

        baseUris.forEach { baseUri ->
            val bundle = Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.MediaColumns.DATE_EXPIRES} ASC")
            }

            context.contentResolver.query(baseUri, projection, bundle, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val dateExpiresCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_EXPIRES)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(baseUri, id)
                    items.add(
                        TrashedMediaItem(
                            id = id,
                            uri = contentUri.toString(),
                            name = cursor.getString(nameCol) ?: "Sin nombre",
                            mimeType = cursor.getString(mimeCol) ?: "image/jpeg",
                            size = cursor.getLong(sizeCol),
                            dateAdded = cursor.getLong(dateAddedCol),
                            dateExpires = cursor.getLong(dateExpiresCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol)
                        )
                    )
                }
            }
        }

        emit(items.sortedBy { it.dateExpires })
    }.flowOn(Dispatchers.IO)

    /**
     * Restaura un archivo de la papelera a su ubicación original.
     */
    suspend fun restoreFromTrash(item: TrashedMediaItem): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return try {
            val contentUri = android.net.Uri.parse(item.uri)
            val values = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.IS_TRASHED, 0)
            }
            context.contentResolver.update(contentUri, values, null, null) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Elimina permanentemente un archivo de la papelera.
     */
    suspend fun deletePermanently(item: TrashedMediaItem): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return try {
            val contentUri = android.net.Uri.parse(item.uri)
            context.contentResolver.delete(contentUri, null, null) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Cuenta los archivos totales en papelera.
     */
    suspend fun getTrashedCount(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return 0
        var count = 0
        val uris = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
        uris.forEach { baseUri ->
            val bundle = Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            }
            context.contentResolver.query(baseUri, arrayOf(MediaStore.MediaColumns._ID), bundle, null)
                ?.use { cursor -> count += cursor.count }
        }
        return count
    }

    /**
     * Obtiene la miniatura del archivo más reciente en papelera.
     */
    suspend fun getTrashedThumbnail(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val uris = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
        for (baseUri in uris) {
            val bundle = Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.MediaColumns.DATE_ADDED} DESC")
            }
            context.contentResolver.query(baseUri, arrayOf(MediaStore.MediaColumns._ID), bundle, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        return ContentUris.withAppendedId(baseUri, id).toString()
                    }
                }
        }
        return null
    }
}

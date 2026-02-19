package com.my_gallery.data.coil

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import androidx.core.graphics.drawable.toDrawable

/**
 * Fetcher optimizado para MediaStore que aprovecha loadThumbnail de Android 10+.
 * Esto evita abrir archivos pesados (>10MB o RAW) para generar la miniatura.
 */
class MediaStoreThumbnailFetcher(
    private val data: Uri,
    private val options: Options,
    private val contentResolver: ContentResolver
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        // Si el tamaño solicitado es grande o ORIGINAL, dejamos que Coil lea el archivo completo
        // para mantener la máxima calidad en el visor.
        val size = options.size
        if (size == coil.size.Size.ORIGINAL) return null
        
        // Si el tamaño solicitado es mayor a 600px, probablemente es el visor, devolvemos null
        // para cargar la imagen original.
        if (size.width is coil.size.Dimension.Pixels && (size.width as coil.size.Dimension.Pixels).px > 600) {
            return null
        }

        // Solo para Android 10 (API 29) o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Pedimos una miniatura de 400x400 (buffer cómodo)
                val bitmap = contentResolver.loadThumbnail(data, Size(400, 400), null)
                return DrawableResult(
                    drawable = bitmap.toDrawable(options.context.resources),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }

    class Factory(private val contentResolver: ContentResolver) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Solo interceptamos URIs locales de MediaStore
            if (data.scheme == ContentResolver.SCHEME_CONTENT && data.authority == "media") {
                return MediaStoreThumbnailFetcher(data, options, contentResolver)
            }
            return null
        }
    }
}

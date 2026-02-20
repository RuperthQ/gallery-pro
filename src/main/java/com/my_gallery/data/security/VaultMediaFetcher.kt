package com.my_gallery.data.security

import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.buffer
import okio.source
import coil.size.pxOrElse
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class VaultMediaFetcher @Inject constructor(
    private val vaultMediaRepository: VaultMediaRepository,
    @ApplicationContext private val context: Context
) : Fetcher.Factory<Uri> {

    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        if (!isVaultUri(data)) return null
        return VaultFetcher(data, options, vaultMediaRepository, context)
    }

    private fun isVaultUri(uri: Uri): Boolean {
        return uri.scheme == "vault"
    }

    class VaultFetcher(
        private val uri: Uri,
        private val options: Options,
        private val repository: VaultMediaRepository,
        private val context: Context
    ) : Fetcher {
        override suspend fun fetch(): FetchResult? {
            val mediaId = uri.authority ?: return null

            val width = options.size.width.pxOrElse { 0 }
            val height = options.size.height.pxOrElse { 0 }
            
            // Verificamos si se solicitó explícitamente la resolución completa (Visor)
            val isFullResRequested = options.parameters.value("is_full_res") as? Boolean ?: false

            // Heurística de miniatura: Si no es FullRes explicito y el tamaño es pequeño (<1000px en ambos ejes)
            val isThumbnailRequest = !isFullResRequested && 
                (width in 1..1000) && (height in 1..1000)

            val decryptedBytes = if (isThumbnailRequest) {
                repository.decryptThumbnailBytes(mediaId) ?: repository.generateAndCacheThumbnailForVaultItem(mediaId)
            } else {
                repository.decryptMediaBytes(mediaId)
            } ?: return null

            val inputStream = ByteArrayInputStream(decryptedBytes)
            return SourceResult(
                source = ImageSource(
                    source = inputStream.source().buffer(),
                    context = context
                ),
                mimeType = null,
                dataSource = DataSource.DISK
            )
        }
    }
}

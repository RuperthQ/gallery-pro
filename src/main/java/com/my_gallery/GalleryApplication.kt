package com.my_gallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import com.my_gallery.data.coil.MediaStoreThumbnailFetcher
import com.my_gallery.data.security.VaultMediaFetcher
import com.my_gallery.data.security.VaultMediaRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class GalleryApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface VaultRepositoryEntryPoint {
        fun vaultRepository(): VaultMediaRepository
    }

    override fun newImageLoader(): ImageLoader {
        val vaultRepository = EntryPointAccessors.fromApplication(
            this, VaultRepositoryEntryPoint::class.java
        ).vaultRepository()

        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // Reducido al 15% para dar aire al decodificador de Video
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("gallery_cache"))
                    .maxSizePercent(0.1) // 10% del espacio del disco o...
                    .maxSizeBytes(512L * 1024 * 1024) // ...máximo 512MB
                    .build()
            }
            .components {
                // Priorizar el fetcher de miniaturas de Android para fotos locales
                add(MediaStoreThumbnailFetcher.Factory(contentResolver))
                // Agregar Fetcher para la bóveda segura
                add(VaultMediaFetcher(vaultRepository, this@GalleryApplication))
            }
            .crossfade(true)
            .allowHardware(true)
            .build()
    }
}

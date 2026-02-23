package com.my_gallery.domain.usecase.gallery

import com.my_gallery.data.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetAvailableMediaFiltersUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    fun getTypes(): Flow<List<String>> = kotlinx.coroutines.flow.flowOf(listOf("Todos", "Imágenes", "Videos"))

    fun getExtensions(): Flow<List<String>> {
        return repository.getDistinctMimeTypes("LOCAL")
            .map { mimes ->
                val exts = mimes.map { it.split("/").last().uppercase() }
                    .filter { it.isNotBlank() && it != "%" }
                    .distinct()
                    .sorted()
                listOf("Todas") + exts
            }
    }

    fun getVideoResolutions(): Flow<List<String>> {
        return repository.getAvailableVideoResolutions("LOCAL")
            .map { resolutions ->
                val labels = mutableSetOf<String>()
                resolutions.forEach { res ->
                    val maxDim = maxOf(res.width, res.height)
                    when {
                        maxDim >= 3840 -> labels.add("4K")
                        maxDim >= 2560 -> labels.add("2K")
                        maxDim >= 1920 -> labels.add("1080P")
                        maxDim >= 1280 -> labels.add("720P")
                        else -> labels.add("SD")
                    }
                }
                listOf("Todas") + labels.toList().sortedByDescending {
                    when(it) { "4K" -> 4; "2K" -> 3; "1080P" -> 2; "720P" -> 1; else -> 0 }
                }
            }
    }
}

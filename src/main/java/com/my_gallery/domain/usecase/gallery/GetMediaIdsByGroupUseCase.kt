package com.my_gallery.domain.usecase.gallery

import com.my_gallery.data.repository.MediaRepository
import javax.inject.Inject

class GetMediaIdsByGroupUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(
        period: String,
        types: Set<String>,
        extensions: Set<String>,
        resolutions: Set<String>,
        albumId: String?
    ): List<String> {
        return repository.getMediaIdsMultiFilter(
            source = "LOCAL",
            periods = listOf(period),
            types = types.toList(),
            extensions = extensions.toList(),
            resolutions = resolutions.toList(),
            albumId = albumId
        )
    }
}

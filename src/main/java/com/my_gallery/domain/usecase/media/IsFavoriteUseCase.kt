package com.my_gallery.domain.usecase.media

import com.my_gallery.data.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsFavoriteUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(id: String): Flow<Boolean> {
        return repository.isFavorite(id)
    }
}

package com.my_gallery.domain.usecase.gallery

import com.my_gallery.data.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMediaChangesUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(): Flow<Unit> = repository.mediaChanges
}

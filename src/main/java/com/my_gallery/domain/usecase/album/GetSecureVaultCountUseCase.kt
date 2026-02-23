package com.my_gallery.domain.usecase.album

import com.my_gallery.data.repository.MediaRepository
import javax.inject.Inject

class GetSecureVaultCountUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(): Int {
        return repository.getSecureVaultCount()
    }
}

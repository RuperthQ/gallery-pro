package com.my_gallery.domain.usecase.media

import javax.inject.Inject

data class MediaUseCases @Inject constructor(
    val deleteMedia: DeleteMediaUseCase,
    val secureMedia: SecureMediaUseCase,
    val unsecureMedia: UnsecureMediaUseCase,
    val renameMedia: RenameMediaUseCase,
    val rotateMedia: RotateMediaUseCase
)

package com.my_gallery.domain.usecase.permissions

import javax.inject.Inject

data class PermissionUseCases @Inject constructor(
    val checkEditPermission: CheckEditPermissionUseCase,
    val requestEditPermission: RequestEditPermissionUseCase
)

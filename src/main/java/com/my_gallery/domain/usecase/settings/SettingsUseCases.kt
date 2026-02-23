package com.my_gallery.domain.usecase.settings

import javax.inject.Inject

data class SettingsUseCases @Inject constructor(
    val updateSettings: UpdateSettingsUseCase
)

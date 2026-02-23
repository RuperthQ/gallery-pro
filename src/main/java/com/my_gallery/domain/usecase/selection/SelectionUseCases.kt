package com.my_gallery.domain.usecase.selection

import javax.inject.Inject

data class SelectionUseCases @Inject constructor(
    val toggleMediaSelection: ToggleMediaSelectionUseCase,
    val toggleGroupSelection: ToggleGroupSelectionUseCase,
    val isGroupSelected: IsGroupSelectedUseCase
)

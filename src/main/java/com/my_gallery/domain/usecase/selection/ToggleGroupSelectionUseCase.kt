package com.my_gallery.domain.usecase.selection

import com.my_gallery.domain.usecase.gallery.GetMediaIdsByGroupUseCase
import javax.inject.Inject

class ToggleGroupSelectionUseCase @Inject constructor(
    private val getMediaIdsByGroup: GetMediaIdsByGroupUseCase
) {
    suspend operator fun invoke(
        currentSelection: Set<String>,
        period: String,
        types: Set<String>,
        extensions: Set<String>,
        resolutions: Set<String>,
        albumId: String?
    ): Pair<Set<String>, List<String>> {
        val groupIds = getMediaIdsByGroup(period, types, extensions, resolutions, albumId)
        val newSelection = currentSelection.toMutableSet()
        
        if (groupIds.isNotEmpty() && groupIds.all { it in currentSelection }) {
            newSelection.removeAll(groupIds)
        } else {
            newSelection.addAll(groupIds)
        }
        
        return newSelection to groupIds
    }
}

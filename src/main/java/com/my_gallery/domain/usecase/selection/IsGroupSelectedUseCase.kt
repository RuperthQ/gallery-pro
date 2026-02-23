package com.my_gallery.domain.usecase.selection

import javax.inject.Inject

class IsGroupSelectedUseCase @Inject constructor() {
    operator fun invoke(
        period: String,
        selectedIds: Set<String>,
        groupIdsCache: Map<String, List<String>>
    ): Boolean {
        val ids = groupIdsCache[period] ?: return false
        if (ids.isEmpty()) return false
        return ids.all { it in selectedIds }
    }
}

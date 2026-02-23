package com.my_gallery.domain.usecase.gallery

import com.my_gallery.ui.gallery.utils.FormatUtils
import javax.inject.Inject

class UpdateFiltersDisplayUseCase @Inject constructor() {
    operator fun invoke(currentFilters: Set<String>, isShort: Boolean): Set<String> {
        if (currentFilters.isEmpty()) return currentFilters
        
        return currentFilters.mapNotNull { display ->
            FormatUtils.parseDateSafely(display, !isShort)?.let { date -> // Parse with old format
                FormatUtils.formatPeriodLabel(date, isShort) // Format with new format
            }
        }.toSet()
    }
}

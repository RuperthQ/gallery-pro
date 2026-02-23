package com.my_gallery.domain.usecase.gallery

import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.ui.gallery.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class GetMediaRankUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(
        targetId: String,
        selectedFilters: Set<String>,
        types: Set<String>,
        extensions: Set<String>,
        resolutions: Set<String>,
        albumId: String?,
        isShortDate: Boolean
    ): Int {
        val dbPeriods = selectedFilters.mapNotNull { display ->
            FormatUtils.parseDateSafely(display, isShortDate)?.let { date ->
                SimpleDateFormat("MM-yyyy", Locale.US).format(date)
            }
        }

        return repository.getMediaRankMultiFilter(
            targetId = targetId,
            source = "LOCAL",
            periods = dbPeriods,
            types = types.toList(),
            extensions = extensions.toList(),
            resolutions = resolutions.toList(),
            albumId = albumId
        )
    }
}

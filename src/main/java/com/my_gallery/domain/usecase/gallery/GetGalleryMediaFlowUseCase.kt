package com.my_gallery.domain.usecase.gallery

import androidx.paging.PagingData
import com.my_gallery.ui.gallery.GalleryUiModel
import com.my_gallery.ui.gallery.utils.FormatUtils
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class GetGalleryMediaFlowUseCase @Inject constructor(
    private val getPagedMedia: GetPagedMediaUseCase
) {
    operator fun invoke(
        dates: Set<String>,
        types: Set<String>,
        extensions: Set<String>,
        resolutions: Set<String>,
        albumId: String?,
        isShortDate: Boolean,
        withSeparators: Boolean = true
    ): Flow<PagingData<GalleryUiModel>> {
        val dbPeriods = dates.mapNotNull { display ->
            FormatUtils.parseDateSafely(display, isShortDate)?.let { date ->
                SimpleDateFormat("MM-yyyy", Locale.US).format(date)
            }
        }

        return getPagedMedia(
            periods = dbPeriods,
            types = types.toList(),
            extensions = extensions.toList(),
            resolutions = resolutions.toList(),
            albumId = albumId,
            withSeparators = withSeparators
        )
    }
}

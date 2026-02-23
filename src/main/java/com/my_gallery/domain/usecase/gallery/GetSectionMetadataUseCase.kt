package com.my_gallery.domain.usecase.gallery

import com.my_gallery.data.local.dao.SectionMetadataRow
import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.ui.gallery.utils.FormatUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

class GetSectionMetadataUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(
        types: List<String>,
        extensions: List<String>,
        resolutions: List<String>,
        albumId: String?,
        shortDates: Boolean
    ): Flow<Map<String, SectionMetadataRow>> {
        return repository.getAllSectionsMetadataMultiFilter(
            source = "LOCAL",
            types = types,
            extensions = extensions,
            resolutions = resolutions,
            albumId = albumId
        ).map { list ->
            list.associateBy { row ->
                try {
                    val parts = row.period.split("-")
                    val month = parts[0].toInt()
                    val year = parts[1].toInt()
                    val cal = Calendar.getInstance().apply { set(year, month - 1, 1) }
                    FormatUtils.formatPeriodLabel(cal.time, shortDates)
                } catch (e: Exception) {
                    row.period
                }
            }
        }
    }
}

package com.my_gallery.domain.usecase.gallery

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import com.my_gallery.data.local.entity.MediaEntity
import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.ui.gallery.GalleryUiModel
import com.my_gallery.ui.gallery.utils.FormatUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class GetPagedMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(
        periods: List<String>,
        types: List<String>,
        extensions: List<String>,
        resolutions: List<String>,
        albumId: String?,
        isShortDate: Boolean = false,
        withSeparators: Boolean = true
    ): Flow<PagingData<GalleryUiModel>> {
        val flow = Pager(
            config = PagingConfig(
                pageSize = 40,
                prefetchDistance = 80,
                initialLoadSize = 120,
                enablePlaceholders = true
            ),
            pagingSourceFactory = {
                repository.getPagedItemsMultiFilter(
                    source = "LOCAL",
                    periods = periods,
                    types = types,
                    extensions = extensions,
                    resolutions = resolutions,
                    albumId = albumId
                )
            }
        ).flow
            .map { pagingData ->
                pagingData.map { item: MediaEntity -> 
                    GalleryUiModel.Media(item.toDomain()) as GalleryUiModel 
                }
            }
            
        if (!withSeparators) return flow

        return flow.map { pagingData ->
                pagingData.insertSeparators { before: GalleryUiModel?, after: GalleryUiModel? ->
                    if (after == null) return@insertSeparators null
                    val a = (after as GalleryUiModel.Media).item
                    val afterLabel = FormatUtils.formatDate(a.dateAdded)
                    val afterPeriod = SimpleDateFormat("MM-yyyy", Locale.US).format(Date(a.dateAdded))
                    
                    if (before == null) return@insertSeparators GalleryUiModel.Separator(
                        dateLabel = afterLabel, 
                        period = afterPeriod,
                        timestamp = a.dateAdded
                    )
                    
                    val b = (before as GalleryUiModel.Media).item
                    val beforeLabel = FormatUtils.formatDate(b.dateAdded)
                    if (beforeLabel != afterLabel) {
                        GalleryUiModel.Separator(
                            dateLabel = afterLabel, 
                            period = afterPeriod,
                            timestamp = a.dateAdded
                        )
                    } else null
                }
            }
    }
}

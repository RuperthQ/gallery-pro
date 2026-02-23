package com.my_gallery.domain.usecase.recovery

import com.my_gallery.data.repository.media.TrashDataSource
import com.my_gallery.domain.model.TrashedMediaItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTrashedMediaUseCase @Inject constructor(
    private val trashDataSource: TrashDataSource
) {
    operator fun invoke(): Flow<List<TrashedMediaItem>> = trashDataSource.getTrashedMedia()
    val isSupported: Boolean get() = trashDataSource.isSupported
}

class RestoreFromTrashUseCase @Inject constructor(
    private val trashDataSource: TrashDataSource
) {
    suspend operator fun invoke(item: TrashedMediaItem): Boolean =
        trashDataSource.restoreFromTrash(item)
}

class DeletePermanentlyUseCase @Inject constructor(
    private val trashDataSource: TrashDataSource
) {
    suspend operator fun invoke(item: TrashedMediaItem): Boolean =
        trashDataSource.deletePermanently(item)

    suspend operator fun invoke(items: List<TrashedMediaItem>): Int =
        items.count { trashDataSource.deletePermanently(it) }
}

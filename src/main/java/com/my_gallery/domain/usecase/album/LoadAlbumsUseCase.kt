package com.my_gallery.domain.usecase.album

import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.data.repository.SecurityRepository
import com.my_gallery.domain.model.AlbumItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class LoadAlbumsUseCase @Inject constructor(
    private val repository: MediaRepository,
    private val securityRepository: SecurityRepository
) {
    operator fun invoke(showEmpty: Boolean): Flow<List<AlbumItem>> {
        return combine(
            repository.getLocalAlbums(showEmpty),
            securityRepository.isDecoyMode,
            securityRepository.lockedAlbums
        ) { list, isDecoy, locked ->
            val filteredList = if (isDecoy) {
                list.filter { it.id !in locked }
            } else list

            val totalCount = filteredList.sumOf { it.count }
            val latestPublicThumb = repository.getLatestPublicThumbnail() ?: filteredList.firstOrNull()?.thumbnail ?: ""
            
            val virtualAll = AlbumItem(
                id = "ALL_VIRTUAL_ALBUM",
                name = "Todo",
                thumbnail = latestPublicThumb,
                count = totalCount
            )
            
            val vaultCount = if (isDecoy) 0 else repository.getSecureVaultCount()
            if (vaultCount > 0) {
                val vaultThumb = repository.getSecureVaultThumbnail() ?: ""
                val vaultVirtual = AlbumItem(
                    id = "SECURE_VAULT",
                    name = "Bóveda Segura",
                    thumbnail = vaultThumb,
                    count = vaultCount
                )
                listOf(virtualAll, vaultVirtual) + filteredList
            } else {
                listOf(virtualAll) + filteredList
            }
        }
    }
}

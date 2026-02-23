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
            securityRepository.lockedAlbums,
            repository.getFavoritesCountFlow(),
            repository.getFavoritesThumbnailFlow()
        ) { list, isDecoy, locked, favCount, favThumb ->
            val filteredList = if (isDecoy) {
                list.filter { it.id !in locked }
            } else list

            val totalCount = filteredList.sumOf { it.count }
            val latestPublicThumb = repository.getLatestPublicThumbnail() ?: filteredList.firstOrNull()?.thumbnail ?: ""
            
            val virtualAlbums = mutableListOf<AlbumItem>()
            
            virtualAlbums.add(AlbumItem(
                id = "ALL_VIRTUAL_ALBUM",
                name = "Todo",
                thumbnail = latestPublicThumb,
                count = totalCount
            ))
            
            if (favCount > 0) {
                virtualAlbums.add(AlbumItem(
                    id = "FAVORITES_VIRTUAL_ALBUM",
                    name = "Favoritos",
                    thumbnail = favThumb ?: "",
                    count = favCount
                ))
            }
            
            val vaultCount = if (isDecoy) 0 else repository.getSecureVaultCount()
            if (vaultCount > 0) {
                virtualAlbums.add(AlbumItem(
                    id = "SECURE_VAULT",
                    name = "Bóveda Segura",
                    thumbnail = repository.getSecureVaultThumbnail() ?: "",
                    count = vaultCount
                ))
            }
            
            virtualAlbums + filteredList
        }
    }
}

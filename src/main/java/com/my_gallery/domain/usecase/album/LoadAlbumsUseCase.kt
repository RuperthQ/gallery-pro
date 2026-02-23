package com.my_gallery.domain.usecase.album

import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.data.repository.SecurityRepository
import com.my_gallery.data.repository.SettingsRepository
import com.my_gallery.domain.model.AlbumItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class LoadAlbumsUseCase @Inject constructor(
    private val repository: MediaRepository,
    private val securityRepository: SecurityRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(showEmpty: Boolean): Flow<List<AlbumItem>> {
        return combine(
            repository.getLocalAlbums(showEmpty),
            securityRepository.isDecoyMode,
            securityRepository.lockedAlbums,
            repository.getFavoritesCountFlow(),
            combine(repository.getFavoritesThumbnailFlow(), settingsRepository.showVaultInCarousel, ::Pair)
        ) { list, isDecoy, locked, favCount, pair ->
            val (favThumb, showVaultInCarousel) = pair
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
            if (vaultCount > 0 && showVaultInCarousel) {
                virtualAlbums.add(AlbumItem(
                    id = "SECURE_VAULT",
                    name = "Bóveda Segura",
                    thumbnail = repository.getSecureVaultThumbnail() ?: "",
                    count = vaultCount
                ))
            }

            if (!isDecoy && repository.isTrashedSupported) {
                val trashCount = repository.getTrashedCount()
                if (trashCount > 0) {
                    virtualAlbums.add(AlbumItem(
                        id = "TRASH_VIRTUAL_ALBUM",
                        name = "Papelera",
                        thumbnail = repository.getTrashedThumbnail() ?: "",
                        count = trashCount
                    ))
                }
            }
            
            virtualAlbums + filteredList
        }
    }
}

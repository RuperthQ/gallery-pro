package com.my_gallery.domain.usecase.settings

import com.my_gallery.data.repository.SettingsRepository
import com.my_gallery.ui.gallery.AlbumBehavior
import com.my_gallery.ui.gallery.MenuStyle
import com.my_gallery.ui.theme.AppThemeColor
import javax.inject.Inject

class UpdateSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    fun setAlbumBehavior(behavior: AlbumBehavior) = repository.setAlbumBehavior(behavior)
    fun setThemeColor(color: AppThemeColor) = repository.setThemeColor(color)
    fun setMenuStyle(style: MenuStyle) = repository.setMenuStyle(style)
    fun setShowEmptyAlbums(enabled: Boolean) = repository.setShowEmptyAlbums(enabled)
    fun setAutoplayEnabled(enabled: Boolean) = repository.setAutoplayEnabled(enabled)
    fun setAutoNavigateAfterMove(enabled: Boolean) = repository.setAutoNavigateAfterMove(enabled)
    fun setShortDateFilters(enabled: Boolean) = repository.setShortDateFilters(enabled)
    fun setColumnCount(count: Int) = repository.setColumnCount(count)
    fun setShowFilterType(enabled: Boolean) = repository.setShowFilterType(enabled)
    fun setShowFilterRes(enabled: Boolean) = repository.setShowFilterRes(enabled)
    fun setShowFilterExt(enabled: Boolean) = repository.setShowFilterExt(enabled)
    fun setStartInLastAlbum(enabled: Boolean) = repository.setStartInLastAlbum(enabled)
}

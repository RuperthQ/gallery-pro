package com.my_gallery.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.my_gallery.ui.gallery.MenuStyle
import com.my_gallery.ui.gallery.AlbumBehavior
import com.my_gallery.ui.theme.AppThemeColor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gallery_settings_prefs", Context.MODE_PRIVATE)

    private val _menuStyle = MutableStateFlow(
        MenuStyle.valueOf(prefs.getString(KEY_MENU_STYLE, MenuStyle.TOP_HEADER.name) ?: MenuStyle.TOP_HEADER.name)
    )
    val menuStyle: StateFlow<MenuStyle> = _menuStyle.asStateFlow()

    private val _columnCount = MutableStateFlow(prefs.getInt(KEY_COLUMN_COUNT, 4))
    val columnCount: StateFlow<Int> = _columnCount.asStateFlow()

    private val _showEmptyAlbums = MutableStateFlow(prefs.getBoolean(KEY_SHOW_EMPTY_ALBUMS, false))
    val showEmptyAlbums: StateFlow<Boolean> = _showEmptyAlbums.asStateFlow()

    private val _albumBehavior = MutableStateFlow(
        AlbumBehavior.valueOf(prefs.getString(KEY_ALBUM_BEHAVIOR, AlbumBehavior.FIXED_IN_GRID.name) ?: AlbumBehavior.FIXED_IN_GRID.name)
    )
    val albumBehavior: StateFlow<AlbumBehavior> = _albumBehavior.asStateFlow()

    private val _themeColor = MutableStateFlow(
        AppThemeColor.valueOf(prefs.getString(KEY_THEME_COLOR, AppThemeColor.SYSTEM.name) ?: AppThemeColor.SYSTEM.name)
    )
    val themeColor: StateFlow<AppThemeColor> = _themeColor.asStateFlow()

    private val _autoplayEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTOPLAY, true))
    val autoplayEnabled: StateFlow<Boolean> = _autoplayEnabled.asStateFlow()

    private val _autoNavigateAfterMove = MutableStateFlow(prefs.getBoolean(KEY_AUTO_NAVIGATE, true))
    val autoNavigateAfterMove: StateFlow<Boolean> = _autoNavigateAfterMove.asStateFlow()

    private val _shortDateFilters = MutableStateFlow(prefs.getBoolean(KEY_SHORT_DATE_FILTERS, false))
    val shortDateFilters: StateFlow<Boolean> = _shortDateFilters.asStateFlow()

    private val _showFilterType = MutableStateFlow(prefs.getBoolean(KEY_SHOW_FILTER_TYPE, true))
    val showFilterType: StateFlow<Boolean> = _showFilterType.asStateFlow()

    private val _showFilterRes = MutableStateFlow(prefs.getBoolean(KEY_SHOW_FILTER_RES, false))
    val showFilterRes: StateFlow<Boolean> = _showFilterRes.asStateFlow()

    private val _showFilterExt = MutableStateFlow(prefs.getBoolean(KEY_SHOW_FILTER_EXT, false))
    val showFilterExt: StateFlow<Boolean> = _showFilterExt.asStateFlow()

    private val _startInLastAlbum = MutableStateFlow(prefs.getBoolean(KEY_START_LAST_ALBUM, false))
    val startInLastAlbum: StateFlow<Boolean> = _startInLastAlbum.asStateFlow()

    private val _lastVisitedAlbum = MutableStateFlow(prefs.getString(KEY_LAST_VISITED_ALBUM, null))
    val lastVisitedAlbum: StateFlow<String?> = _lastVisitedAlbum.asStateFlow()

    fun setStartInLastAlbum(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_START_LAST_ALBUM, enabled).apply()
        _startInLastAlbum.value = enabled
    }

    fun setLastVisitedAlbum(albumId: String?) {
        prefs.edit().putString(KEY_LAST_VISITED_ALBUM, albumId).apply()
        _lastVisitedAlbum.value = albumId
    }

    fun setShowFilterType(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_FILTER_TYPE, enabled).apply()
        _showFilterType.value = enabled
    }

    fun setShowFilterRes(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_FILTER_RES, enabled).apply()
        _showFilterRes.value = enabled
    }

    fun setShowFilterExt(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_FILTER_EXT, enabled).apply()
        _showFilterExt.value = enabled
    }

    fun setShortDateFilters(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHORT_DATE_FILTERS, enabled).apply()
        _shortDateFilters.value = enabled
    }

    fun setAutoNavigateAfterMove(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_NAVIGATE, enabled).apply()
        _autoNavigateAfterMove.value = enabled
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOPLAY, enabled).apply()
        _autoplayEnabled.value = enabled
    }

    fun setMenuStyle(style: MenuStyle) {
        prefs.edit().putString(KEY_MENU_STYLE, style.name).apply()
        _menuStyle.value = style
    }

    fun setAlbumBehavior(behavior: AlbumBehavior) {
        prefs.edit().putString(KEY_ALBUM_BEHAVIOR, behavior.name).apply()
        _albumBehavior.value = behavior
    }

    fun setThemeColor(color: AppThemeColor) {
        prefs.edit().putString(KEY_THEME_COLOR, color.name).apply()
        _themeColor.value = color
    }

    fun setColumnCount(count: Int) {
        prefs.edit().putInt(KEY_COLUMN_COUNT, count).apply()
        _columnCount.value = count
    }

    fun setShowEmptyAlbums(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_EMPTY_ALBUMS, show).apply()
        _showEmptyAlbums.value = show
    }

    companion object {
        private const val KEY_MENU_STYLE = "KEY_MENU_STYLE"
        private const val KEY_COLUMN_COUNT = "KEY_COLUMN_COUNT"
        private const val KEY_SHOW_EMPTY_ALBUMS = "KEY_SHOW_EMPTY_ALBUMS"
        private const val KEY_ALBUM_BEHAVIOR = "KEY_ALBUM_BEHAVIOR"
        private const val KEY_THEME_COLOR = "KEY_THEME_COLOR"
        private const val KEY_AUTOPLAY = "KEY_AUTOPLAY"
        private const val KEY_AUTO_NAVIGATE = "KEY_AUTO_NAVIGATE"
        private const val KEY_SHORT_DATE_FILTERS = "KEY_SHORT_DATE_FILTERS"
        private const val KEY_SHOW_FILTER_TYPE = "KEY_SHOW_FILTER_TYPE"
        private const val KEY_SHOW_FILTER_RES = "KEY_SHOW_FILTER_RES"
        private const val KEY_SHOW_FILTER_EXT = "KEY_SHOW_FILTER_EXT"
        private const val KEY_START_LAST_ALBUM = "KEY_START_LAST_ALBUM"
        private const val KEY_LAST_VISITED_ALBUM = "KEY_LAST_VISITED_ALBUM"
    }
}

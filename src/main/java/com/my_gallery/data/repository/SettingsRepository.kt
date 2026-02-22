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
    }
}

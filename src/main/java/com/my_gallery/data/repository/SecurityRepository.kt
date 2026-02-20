package com.my_gallery.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gallery_security_prefs", Context.MODE_PRIVATE)

    private val _isAppLocked = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCKED, false))
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _lockedAlbums = MutableStateFlow(getLockedAlbumsPref())
    val lockedAlbums: StateFlow<Set<String>> = _lockedAlbums
        .combine(MutableStateFlow(setOf("SECURE_VAULT"))) { locked, fixed -> 
            locked + fixed 
        }
        .stateIn(
            scope = kotlinx.coroutines.GlobalScope, 
            started = SharingStarted.Eagerly, 
            initialValue = getLockedAlbumsPref() + "SECURE_VAULT"
        )

    fun setAppLocked(locked: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCKED, locked).apply()
        _isAppLocked.value = locked
    }

    fun toggleAlbumLock(albumId: String) {
        val current = _lockedAlbums.value.toMutableSet()
        if (current.contains(albumId)) current.remove(albumId) else current.add(albumId)
        
        prefs.edit().putStringSet(KEY_LOCKED_ALBUMS, current).apply()
        _lockedAlbums.value = current
    }

    fun isAlbumLocked(albumId: String): Boolean {
        if (albumId == "SECURE_VAULT") return true
        return _lockedAlbums.value.contains(albumId)
    }

    private fun getLockedAlbumsPref(): Set<String> {
        return prefs.getStringSet(KEY_LOCKED_ALBUMS, emptySet())?.toSet() ?: emptySet()
    }

    companion object {
        private const val KEY_APP_LOCKED = "KEY_APP_LOCKED"
        private const val KEY_LOCKED_ALBUMS = "KEY_LOCKED_ALBUMS"
    }
}

package com.my_gallery.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.my_gallery.data.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityRepository: SecurityRepository
) : ViewModel() {

    val isAppLocked = securityRepository.isAppLocked
    val lockedAlbums: StateFlow<Set<String>> = securityRepository.lockedAlbums
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    val isDecoyMode = securityRepository.isDecoyMode

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        // Si la app no tiene bloqueo por huella, estamos autenticados por defecto.
        viewModelScope.launch {
            isAppLocked.collect { locked ->
                if (!locked) {
                    _isAuthenticated.value = true
                }
            }
        }
    }

    fun setAuthenticated(value: Boolean) {
        _isAuthenticated.value = value
    }

    fun setError(error: String?) {
        _authError.value = error
    }

    fun toggleAppLock(enabled: Boolean) {
        securityRepository.setAppLocked(enabled)
        if (!enabled) _isAuthenticated.value = true
    }

    fun toggleAlbumLock(albumId: String) {
        securityRepository.toggleAlbumLock(albumId)
    }

    fun isAlbumLocked(albumId: String): Boolean {
        return securityRepository.isAlbumLocked(albumId)
    }

    fun logout() {
        if (securityRepository.isAppLocked.value) {
            _isAuthenticated.value = false
            securityRepository.setDecoyMode(false)
        }
    }

    fun enterDecoyMode() {
        securityRepository.setDecoyMode(true)
        _isAuthenticated.value = true
    }
}

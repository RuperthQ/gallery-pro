package com.my_gallery.ui.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.my_gallery.domain.model.TrashedMediaItem
import com.my_gallery.domain.usecase.recovery.DeletePermanentlyUseCase
import com.my_gallery.domain.usecase.recovery.GetTrashedMediaUseCase
import com.my_gallery.domain.usecase.recovery.RestoreFromTrashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val getTrashedMedia: GetTrashedMediaUseCase,
    private val restoreFromTrash: RestoreFromTrashUseCase,
    private val deletePermanently: DeletePermanentlyUseCase
) : ViewModel() {

    val isSupported: Boolean = getTrashedMedia.isSupported

    val trashedMedia: StateFlow<List<TrashedMediaItem>> = getTrashedMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _lastActionMessage = MutableStateFlow<String?>(null)
    val lastActionMessage: StateFlow<String?> = _lastActionMessage.asStateFlow()

    fun toggleSelection(id: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (contains(id)) remove(id) else add(id)
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun selectAll() {
        _selectedIds.value = trashedMedia.value.map { it.id }.toSet()
    }

    fun restoreSelected() {
        val ids = _selectedIds.value
        val items = trashedMedia.value.filter { it.id in ids }
        viewModelScope.launch {
            _isLoading.value = true
            var restored = 0
            items.forEach { if (restoreFromTrash(it)) restored++ }
            _selectedIds.value = emptySet()
            _lastActionMessage.value = if (restored > 0) "Se restauraron $restored archivos" else "No se pudo restaurar"
            _isLoading.value = false
        }
    }

    fun restoreItem(item: TrashedMediaItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val ok = restoreFromTrash(item)
            _lastActionMessage.value = if (ok) "Archivo restaurado" else "No se pudo restaurar"
            _isLoading.value = false
        }
    }

    fun deleteSelected() {
        val ids = _selectedIds.value
        val items = trashedMedia.value.filter { it.id in ids }
        viewModelScope.launch {
            _isLoading.value = true
            var deleted = 0
            items.forEach { if (deletePermanently(it)) deleted++ }
            _selectedIds.value = emptySet()
            _lastActionMessage.value = if (deleted > 0) "Se eliminaron $deleted archivos" else "No se pudo eliminar"
            _isLoading.value = false
        }
    }

    fun deleteItem(item: TrashedMediaItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val ok = deletePermanently(item)
            _lastActionMessage.value = if (ok) "Eliminado permanentemente" else "No se pudo eliminar"
            _isLoading.value = false
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            _isLoading.value = true
            val all = trashedMedia.value
            var deleted = 0
            all.forEach { if (deletePermanently(it)) deleted++ }
            _selectedIds.value = emptySet()
            _lastActionMessage.value = "Papelera vaciada ($deleted archivos)"
            _isLoading.value = false
        }
    }

    fun clearMessage() {
        _lastActionMessage.value = null
    }
}

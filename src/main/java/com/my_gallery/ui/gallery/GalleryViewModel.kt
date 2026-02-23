package com.my_gallery.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.my_gallery.domain.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import java.util.Locale
import javax.inject.Inject
import android.os.Build
import androidx.annotation.RequiresApi
import com.my_gallery.data.repository.SecurityRepository
import com.my_gallery.data.repository.SettingsRepository
import com.my_gallery.data.repository.media.RenameResult
import com.my_gallery.data.repository.media.DeleteResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import com.my_gallery.domain.model.AlbumItem
import com.my_gallery.data.local.dao.SectionMetadataRow
import com.my_gallery.ui.theme.AppThemeColor

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val settingsRepository: SettingsRepository,
    private val mediaUseCases: com.my_gallery.domain.usecase.media.MediaUseCases,
    private val albumUseCases: com.my_gallery.domain.usecase.album.AlbumUseCases,
    private val galleryUseCases: com.my_gallery.domain.usecase.gallery.GalleryUseCases,
    private val selectionUseCases: com.my_gallery.domain.usecase.selection.SelectionUseCases,
    private val permissionUseCases: com.my_gallery.domain.usecase.permissions.PermissionUseCases,
    private val settingsUseCases: com.my_gallery.domain.usecase.settings.SettingsUseCases
) : ViewModel() {

    private val _albums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val albums: StateFlow<List<AlbumItem>> = _albums.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<String?>(
        if (settingsRepository.startInLastAlbum.value) settingsRepository.lastVisitedAlbum.value else null
    )
    val selectedAlbum: StateFlow<String?> = _selectedAlbum.asStateFlow()

    fun toggleAlbum(albumId: String?) {
        groupIdsCache.clear()
        if (albumId == "ALL_VIRTUAL_ALBUM") {
            _selectedAlbum.value = null
        } else {
            _selectedAlbum.value = if (_selectedAlbum.value == albumId) null else albumId
        }
        settingsRepository.setLastVisitedAlbum(_selectedAlbum.value)
    }

    val columnCount: StateFlow<Int> = settingsRepository.columnCount

    val albumBehavior: StateFlow<AlbumBehavior> = settingsRepository.albumBehavior

    fun setAlbumBehavior(behavior: AlbumBehavior) {
        settingsUseCases.updateSettings.setAlbumBehavior(behavior)
    }

    val themeColor: StateFlow<AppThemeColor> = settingsRepository.themeColor

    fun setThemeColor(color: AppThemeColor) {
        settingsUseCases.updateSettings.setThemeColor(color)
    }

    private val _selectedFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedFilters: StateFlow<Set<String>> = _selectedFilters.asStateFlow()

    private val _selectedTypes = MutableStateFlow<Set<String>>(emptySet())
    val selectedTypes: StateFlow<Set<String>> = _selectedTypes.asStateFlow()

    private val _selectedExtensions = MutableStateFlow<Set<String>>(emptySet())
    val selectedExtensions: StateFlow<Set<String>> = _selectedExtensions.asStateFlow()

    private val _selectedResolutions = MutableStateFlow<Set<String>>(emptySet())
    val selectedResolutions: StateFlow<Set<String>> = _selectedResolutions.asStateFlow()

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val _pendingActions = MutableStateFlow(MediaPendingActions())
    val pendingActions: StateFlow<MediaPendingActions> = _pendingActions.asStateFlow()

    fun updateState(transform: (GalleryUiState) -> GalleryUiState) {
        _uiState.value = transform(_uiState.value)
    }

    fun toggleAppLock(locked: Boolean) {
        securityRepository.setAppLocked(locked)
    }

    val isAppLocked = securityRepository.isAppLocked

    fun showSettings() = updateState { it.copy(showSettings = true) }
    fun hideSettings() = updateState { it.copy(showSettings = false) }

    fun showTrash() = updateState { it.copy(showTrash = true) }
    fun hideTrash() = updateState { it.copy(showTrash = false) }

    fun deleteAlbum(album: com.my_gallery.domain.model.AlbumItem) {
        viewModelScope.launch {
            albumUseCases.deleteAlbum(album)
            syncGallery()
        }
    }

    val menuStyle: StateFlow<MenuStyle> = settingsRepository.menuStyle

    fun setMenuStyle(style: MenuStyle) {
        settingsUseCases.updateSettings.setMenuStyle(style)
    }

    val showEmptyAlbums: StateFlow<Boolean> = settingsRepository.showEmptyAlbums

    fun toggleShowEmptyAlbums() {
        settingsUseCases.updateSettings.setShowEmptyAlbums(!showEmptyAlbums.value)
        syncGallery()
    }

    private val _selectedItem = MutableStateFlow<MediaItem?>(null)
    val selectedItem: StateFlow<MediaItem?> = _selectedItem.asStateFlow()

    private val _viewerItem = MutableStateFlow<MediaItem?>(null)
    val viewerItem: StateFlow<MediaItem?> = _viewerItem.asStateFlow()

    private val _viewerIndex = MutableStateFlow(0)
    val viewerIndex: StateFlow<Int> = _viewerIndex.asStateFlow()

    val autoplayEnabled: StateFlow<Boolean> = settingsRepository.autoplayEnabled

    fun toggleAutoplay() {
        settingsUseCases.updateSettings.setAutoplayEnabled(!autoplayEnabled.value)
    }

    val autoNavigateAfterMove: StateFlow<Boolean> = settingsRepository.autoNavigateAfterMove

    fun toggleAutoNavigate() {
        settingsUseCases.updateSettings.setAutoNavigateAfterMove(!autoNavigateAfterMove.value)
    }

    val shortDateFilters: StateFlow<Boolean> = settingsRepository.shortDateFilters

    fun toggleShortDateFilters() {
        settingsUseCases.updateSettings.setShortDateFilters(!shortDateFilters.value)
    }

    val startInLastAlbum: StateFlow<Boolean> = settingsRepository.startInLastAlbum

    fun toggleStartInLastAlbum() {
        settingsUseCases.updateSettings.setStartInLastAlbum(!startInLastAlbum.value)
    }

    val showFilterType: StateFlow<Boolean> = settingsRepository.showFilterType
    val showFilterRes: StateFlow<Boolean> = settingsRepository.showFilterRes
    val showFilterExt: StateFlow<Boolean> = settingsRepository.showFilterExt

    fun toggleFilterType() = settingsUseCases.updateSettings.setShowFilterType(!showFilterType.value)
    fun toggleFilterRes() = settingsUseCases.updateSettings.setShowFilterRes(!showFilterRes.value)
    fun toggleFilterExt() = settingsUseCases.updateSettings.setShowFilterExt(!showFilterExt.value)


    private val _selectedMediaIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMediaIds: StateFlow<Set<String>> = _selectedMediaIds.asStateFlow()

    private val groupIdsCache = mutableMapOf<String, List<String>>()

    fun toggleSelectionMode() {
        val next = !_uiState.value.isSelectionMode
        updateState { it.copy(isSelectionMode = next) }
        if (!next) exitSelection()
    }

    fun toggleMediaSelection(mediaId: String) {
        val newSelection = selectionUseCases.toggleMediaSelection(_selectedMediaIds.value, mediaId)
        _selectedMediaIds.value = newSelection
        
        if (newSelection.isEmpty() && _uiState.value.isSelectionMode) {
            exitSelection()
        } else if (newSelection.isNotEmpty() && !_uiState.value.isSelectionMode) {
            updateState { it.copy(isSelectionMode = true) }
        }
    }

    fun showCreateAlbumDialog() = updateState { it.copy(showCreateAlbumDialog = true) }
    fun hideCreateAlbumDialog() = updateState { it.copy(showCreateAlbumDialog = false) }

    fun showMoveToAlbumDialog() = updateState { it.copy(showMoveToAlbumDialog = true) }
    fun hideMoveToAlbumDialog() = updateState { it.copy(showMoveToAlbumDialog = false) }

    fun showDeleteConfirmation() = updateState { it.copy(showDeleteConfirmation = true) }
    fun hideDeleteConfirmation() = updateState { it.copy(showDeleteConfirmation = false) }

    fun startAlbumCreation(name: String) {
        val currentSelection = _selectedMediaIds.value
        updateState { it.copy(showCreateAlbumDialog = false, targetAlbumName = name) }

        if (currentSelection.isNotEmpty()) {
            saveSelectedToNewAlbum()
        } else {
            updateState { it.copy(isSelectionMode = true, isAlbumCreationPending = true) }
            _selectedMediaIds.value = emptySet()

            val tempAlbum = albumUseCases.prepareTempAlbum(name)
            _albums.value = listOf(tempAlbum) + _albums.value
        }
    }

    fun saveSelectedToNewAlbum() {
        val albumName = _uiState.value.targetAlbumName ?: return
        viewModelScope.launch {
            updateState { it.copy(isMovingMedia = true) }
            try {
                val result = albumUseCases.saveMediaToAlbum(_selectedMediaIds.value.toList(), albumName, _uiState.value.targetAlbumId)
                handleSaveResult(result)
            } finally {
                updateState { it.copy(isMovingMedia = false) }
            }
        }
    }

    private fun handleSaveResult(result: com.my_gallery.domain.usecase.album.SaveResult) {
        when (result) {
            is com.my_gallery.domain.usecase.album.SaveResult.AlbumCreatedOnly -> {
                exitSelection()
                syncGallery()
            }
            is com.my_gallery.domain.usecase.album.SaveResult.SuccessMove -> {
                val shouldNavigate = autoNavigateAfterMove.value
                exitSelection()
                if (shouldNavigate) _selectedAlbum.value = result.finalAlbumId
                viewModelScope.launch { delay(1000); syncGallery() }
            }
            else -> {}
        }
    }

    fun moveSelectedToExistingAlbum(album: AlbumItem) {
        updateState { it.copy(showMoveToAlbumDialog = false, targetAlbumName = album.name, targetAlbumId = album.id) }
        saveSelectedToNewAlbum()
    }

    fun exitSelection() {
        updateState { it.copy(isSelectionMode = false, isAlbumCreationPending = false, targetAlbumName = null, targetAlbumId = null) }
        _selectedMediaIds.value = emptySet()
    }

    // --------------------------------

    fun checkEditPermission() {
        val granted = permissionUseCases.checkEditPermission()
        updateState { it.copy(isEditPermissionGranted = granted) }
    }

    fun requestEditPermission(context: android.content.Context) {
        permissionUseCases.requestEditPermission(context)
    }



    init {
        checkEditPermission()
        
        viewModelScope.launch {
            delay(500)
            galleryUseCases.syncGallery(force = false)
            loadAlbums()
        }

        viewModelScope.launch {
            galleryUseCases.getMediaChanges()
                .debounce(1000)
                .collect { syncGallery() }
        }

        viewModelScope.launch {
            shortDateFilters.drop(1).collect { isShort ->
                _selectedFilters.value = galleryUseCases.updateFiltersDisplay(_selectedFilters.value, isShort)
            }
        }
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            albumUseCases.loadAlbums(showEmptyAlbums.value).collect { list ->
                _albums.value = list
            }
        }
    }

    val availableTypes: StateFlow<List<String>> = galleryUseCases.getAvailableFilters.getTypes()
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf("Todos"))

    val availableExtensions: StateFlow<List<String>> = galleryUseCases.getAvailableFilters.getExtensions()
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf("Todas"))

    val availableResolutions: StateFlow<List<String>> = galleryUseCases.getAvailableFilters.getVideoResolutions()
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf("Todas"))

    @OptIn(ExperimentalCoroutinesApi::class)
    val sectionMetadata: StateFlow<Map<String, SectionMetadataRow>> = combine(
        _selectedTypes,
        _selectedExtensions,
        _selectedResolutions,
        _selectedAlbum,
        shortDateFilters
    ) { types, exts, vidRes, albumId, shortDates ->
        galleryUseCases.getSectionMetadata(types.toList(), exts.toList(), vidRes.toList(), albumId, shortDates)
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val availableFilters: StateFlow<List<String>> = sectionMetadata.map { metadata ->
        listOf("Todos") + metadata.keys.map { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString() } }
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf("Todos"))

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedItems: Flow<PagingData<GalleryUiModel>> = combine(
        _selectedFilters,
        _selectedTypes,
        _selectedExtensions,
        _selectedResolutions,
        combine(_selectedAlbum, shortDateFilters) { a, b -> Pair(a, b) }
    ) { dates, types, exts, vidRes, (albumId, shortDates) ->
        galleryUseCases.getGalleryMediaFlow(dates, types, exts, vidRes, albumId, shortDates)
    }.flatMapLatest { it }.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewerPagingData: Flow<PagingData<GalleryUiModel>> = combine(
        _selectedFilters,
        _selectedTypes,
        _selectedExtensions,
        _selectedResolutions,
        combine(_selectedAlbum, shortDateFilters) { a, b -> Pair(a, b) }
    ) { dates, types, exts, vidRes, (albumId, shortDates) ->
        galleryUseCases.getGalleryMediaFlow(dates, types, exts, vidRes, albumId, shortDates, withSeparators = false)
    }.flatMapLatest { it }.cachedIn(viewModelScope)





    fun changeColumns() {
        val next = when (columnCount.value) {
            3 -> 4
            4 -> 5
            5 -> 6
            6 -> 3
            else -> 4
        }
        settingsUseCases.updateSettings.setColumnCount(next)
    }

    fun onFilterSelected(filter: String) {
        groupIdsCache.clear()
        if (filter == "Todos") {
            _selectedFilters.value = emptySet()
            return
        }
        val current = _selectedFilters.value
        _selectedFilters.value = if (current.contains(filter)) current - filter else current + filter
    }

    fun onTypeFilterSelected(type: String) {
        groupIdsCache.clear()
        if (type == "Todos") {
            _selectedTypes.value = emptySet()
            return
        }
        val current = _selectedTypes.value
        _selectedTypes.value = if (current.contains(type)) current - type else current + type
    }

    fun onExtensionFilterSelected(ext: String) {
        groupIdsCache.clear()
        if (ext == "Todas") {
            _selectedExtensions.value = emptySet()
            return
        }
        val current = _selectedExtensions.value
        _selectedExtensions.value = if (current.contains(ext)) current - ext else current + ext
    }

    fun onResolutionFilterSelected(res: String) {
        groupIdsCache.clear()
        if (res == "Todas") {
            _selectedResolutions.value = emptySet()
            return
        }
        val current = _selectedResolutions.value
        _selectedResolutions.value = if (current.contains(res)) current - res else current + res
    }

    fun toggleFilters() = updateState { it.copy(showFilters = !it.showFilters) }

    fun selectItem(item: MediaItem) { _selectedItem.value = item }
    fun deselectItem() { _selectedItem.value = null }

    fun openViewer(item: MediaItem, index: Int) {
        viewModelScope.launch {
            val rank = galleryUseCases.getMediaRank(
                targetId = item.id,
                selectedFilters = _selectedFilters.value,
                types = _selectedTypes.value,
                extensions = _selectedExtensions.value,
                resolutions = _selectedResolutions.value,
                albumId = _selectedAlbum.value,
                isShortDate = shortDateFilters.value
            )
            _viewerIndex.value = rank
            _viewerItem.value = item
        }
    }

    fun closeViewer() {
        _viewerItem.value = null
        _viewerIndex.value = 0
    }



    fun syncGallery() {
        groupIdsCache.clear()
        checkEditPermission() // Refrescar estado de permisos al sincronizar
        viewModelScope.launch {
            galleryUseCases.syncGallery(force = false)
            delay(500) // Small delay to let MediaStore update
            loadAlbums()
        }
    }

    fun forceSyncGallery() {
        groupIdsCache.clear()
        viewModelScope.launch {
            updateState { it.copy(isForceSyncing = true) }
            try {
                galleryUseCases.syncGallery(force = true)
                _selectedAlbum.value = _selectedAlbum.value // Trigger Refresh
                loadAlbums()
            } finally {
                updateState { it.copy(isForceSyncing = false) }
            }
        }
    }



    @RequiresApi(Build.VERSION_CODES.Q)
    fun renameMedia(item: MediaItem, newName: String) {
        viewModelScope.launch {
            when (val result = mediaUseCases.renameMedia(item, newName)) {
                is RenameResult.Success -> {
                    _selectedItem.value = item.copy(title = newName)
                    _pendingActions.value = MediaPendingActions()
                }
                is RenameResult.PermissionRequired -> {
                    _pendingActions.value = MediaPendingActions(rename = item to newName, intentSender = result.intentSender)
                }
                else -> _pendingActions.value = MediaPendingActions()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteSelectedMedia() {
        viewModelScope.launch {
            val selectedIds = _selectedMediaIds.value.toList()
            val result = mediaUseCases.deleteMedia(selectedIds) ?: return@launch
            handleOperationResult(result, deleteIds = selectedIds)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun secureSelectedMedia() {
        viewModelScope.launch {
            updateState { it.copy(isSecuringMedia = true) }
            try {
                val selectedIds = _selectedMediaIds.value.toList()
                val result = mediaUseCases.secureMedia(selectedIds) ?: return@launch
                handleOperationResult(result, secureIds = selectedIds)
            } finally {
                updateState { it.copy(isSecuringMedia = false) }
            }
        }
    }

    private fun handleOperationResult(result: com.my_gallery.data.repository.media.DeleteResult, deleteIds: List<String>? = null, secureIds: List<String>? = null) {
        when (result) {
            is com.my_gallery.data.repository.media.DeleteResult.Success -> {
                exitSelection()
                syncGallery()
            }
            is com.my_gallery.data.repository.media.DeleteResult.PermissionRequired -> {
                _pendingActions.value = MediaPendingActions(deleteIds = deleteIds, secureIds = secureIds, intentSender = result.intentSender)
            }
            else -> {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun unsecureSelectedMedia() {
        viewModelScope.launch {
            updateState { it.copy(isUnsecuringMedia = true) }
            try {
                val result = mediaUseCases.unsecureMedia(_selectedMediaIds.value.toList())
                if (result is com.my_gallery.data.repository.media.DeleteResult.Success) {
                    exitSelection()
                    syncGallery()
                }
            } finally {
                updateState { it.copy(isUnsecuringMedia = false) }
            }
        }
    }

    fun rotateMedia(item: MediaItem) {
        viewModelScope.launch {
            mediaUseCases.rotateMedia(item)
            syncGallery()
        }
    }

    fun setWallpaper(item: MediaItem, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            if (mediaUseCases.setWallpaper(item)) onSuccess() else onError()
        }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            mediaUseCases.toggleFavorite(item.id)
            syncGallery()
        }
    }

    fun toggleFavoriteSelection() {
        viewModelScope.launch {
            val selectedIds = _selectedMediaIds.value.toList()
            mediaUseCases.toggleFavorite(selectedIds)
            exitSelection()
            syncGallery()
        }
    }

    fun isFavorite(id: String): Flow<Boolean> = mediaUseCases.isFavorite(id)

    @RequiresApi(Build.VERSION_CODES.Q)
    fun onPermissionResult(success: Boolean) {
        val pending = _pendingActions.value
        _pendingActions.value = MediaPendingActions()
        
        if (success) {
            pending.rename?.let { (item, name) -> renameMedia(item, name) }
            pending.deleteIds?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) exitSelection().also { syncGallery() } else deleteSelectedMedia() }
            pending.secureIds?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) exitSelection().also { syncGallery() } else secureSelectedMedia() }
        }
    }

    fun clearPendingIntent() { _pendingActions.value = MediaPendingActions() }


    fun toggleGroupSelection(label: String, period: String) {
        viewModelScope.launch {
            val (newSelection, groupIds) = selectionUseCases.toggleGroupSelection(
                currentSelection = _selectedMediaIds.value,
                period = period,
                types = _selectedTypes.value,
                extensions = _selectedExtensions.value,
                resolutions = _selectedResolutions.value,
                albumId = _selectedAlbum.value
            )
            
            groupIdsCache[period] = groupIds
            _selectedMediaIds.value = newSelection
            
            if (newSelection.isNotEmpty() && !_uiState.value.isSelectionMode) {
                updateState { it.copy(isSelectionMode = true) }
            }
        }
    }

    fun isGroupSelected(label: String, period: String, selectedIds: Set<String>): Boolean {
        return selectionUseCases.isGroupSelected(period, selectedIds, groupIdsCache)
    }

    fun areAllSelectedSecured(): Boolean {
        if (_selectedMediaIds.value.isEmpty()) return false
        return _selectedAlbum.value == "SECURE_VAULT"
    }

    suspend fun decryptMediaToCache(item: MediaItem): String? {
        return galleryUseCases.decryptMedia(item)
    }

    fun clearDecryptedCache() {
        viewModelScope.launch {
            galleryUseCases.clearDecryptedCache()
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearDecryptedCache()
    }
}



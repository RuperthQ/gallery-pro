package com.my_gallery.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.my_gallery.domain.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.data.repository.RenameResult
import com.my_gallery.data.repository.SecurityRepository
import com.my_gallery.data.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import com.my_gallery.domain.model.AlbumItem
import com.my_gallery.data.local.dao.SectionMetadataRow
import com.my_gallery.ui.theme.AppThemeColor

enum class MenuStyle { TOP_HEADER, BOTTOM_FLOATING }
enum class AlbumBehavior { FIXED_IN_GRID, FLOATING_TOP, STATIC_TOP }

enum class GallerySource { LOCAL }

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val application: Application,
    private val repository: MediaRepository,
    private val securityRepository: SecurityRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _albums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val albums: StateFlow<List<AlbumItem>> = _albums.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<String?>(null)
    val selectedAlbum: StateFlow<String?> = _selectedAlbum.asStateFlow()

    fun toggleAlbum(albumId: String?) {
        groupIdsCache.clear()
        if (albumId == "ALL_VIRTUAL_ALBUM") {
            _selectedAlbum.value = null
        } else {
            _selectedAlbum.value = if (_selectedAlbum.value == albumId) null else albumId
        }
    }

    val columnCount: StateFlow<Int> = settingsRepository.columnCount

    val albumBehavior: StateFlow<AlbumBehavior> = settingsRepository.albumBehavior

    fun setAlbumBehavior(behavior: AlbumBehavior) {
        settingsRepository.setAlbumBehavior(behavior)
    }

    val themeColor: StateFlow<AppThemeColor> = settingsRepository.themeColor

    fun setThemeColor(color: AppThemeColor) {
        settingsRepository.setThemeColor(color)
    }

    private val _selectedFilter = MutableStateFlow<String?>(null)
    val selectedFilter: StateFlow<String?> = _selectedFilter.asStateFlow()

    private val _selectedImageFilter = MutableStateFlow<String?>(null)
    val selectedImageFilter: StateFlow<String?> = _selectedImageFilter.asStateFlow()

    private val _selectedVideoFilter = MutableStateFlow<String?>(null)
    val selectedVideoFilter: StateFlow<String?> = _selectedVideoFilter.asStateFlow()

    private val _showFilters = MutableStateFlow(false)
    val showFilters: StateFlow<Boolean> = _showFilters.asStateFlow()

    fun toggleAppLock(locked: Boolean) {
        securityRepository.setAppLocked(locked)
    }

    val isAppLocked = securityRepository.isAppLocked

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    fun showSettings() { _showSettings.value = true }
    fun hideSettings() { _showSettings.value = false }

    val menuStyle: StateFlow<MenuStyle> = settingsRepository.menuStyle

    fun setMenuStyle(style: MenuStyle) {
        settingsRepository.setMenuStyle(style)
    }

    val showEmptyAlbums: StateFlow<Boolean> = settingsRepository.showEmptyAlbums

    fun toggleShowEmptyAlbums() {
        settingsRepository.setShowEmptyAlbums(!showEmptyAlbums.value)
        syncGallery()
    }

    private val _selectedItem = MutableStateFlow<MediaItem?>(null)
    val selectedItem: StateFlow<MediaItem?> = _selectedItem.asStateFlow()

    private val _viewerItem = MutableStateFlow<MediaItem?>(null)
    val viewerItem: StateFlow<MediaItem?> = _viewerItem.asStateFlow()

    private val _viewerIndex = MutableStateFlow(0)
    val viewerIndex: StateFlow<Int> = _viewerIndex.asStateFlow()

    private val _autoplayEnabled = MutableStateFlow(true)
    val autoplayEnabled: StateFlow<Boolean> = _autoplayEnabled.asStateFlow()

    fun toggleAutoplay() {
        _autoplayEnabled.value = !_autoplayEnabled.value
    }

    // --- Modo Selección y Álbumes ---
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedMediaIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMediaIds: StateFlow<Set<String>> = _selectedMediaIds.asStateFlow()

    // Cache para IDs de grupos/secciones (para optimizar isGroupSelected)
    private val groupIdsCache = mutableMapOf<String, List<String>>()

    private val _showCreateAlbumDialog = MutableStateFlow(false)
    val showCreateAlbumDialog: StateFlow<Boolean> = _showCreateAlbumDialog.asStateFlow()

    private val _showMoveToAlbumDialog = MutableStateFlow(false)
    val showMoveToAlbumDialog: StateFlow<Boolean> = _showMoveToAlbumDialog.asStateFlow()

    fun showMoveToAlbumDialog() {
        _showMoveToAlbumDialog.value = true
    }

    fun hideMoveToAlbumDialog() {
        _showMoveToAlbumDialog.value = false
    }

    private val _isMovingMedia = MutableStateFlow(false)
    val isMovingMedia: StateFlow<Boolean> = _isMovingMedia.asStateFlow()

    private val _isSecuringMedia = MutableStateFlow(false)
    val isSecuringMedia: StateFlow<Boolean> = _isSecuringMedia.asStateFlow()

    private val _isUnsecuringMedia = MutableStateFlow(false)
    val isUnsecuringMedia: StateFlow<Boolean> = _isUnsecuringMedia.asStateFlow()

    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation.asStateFlow()

    fun showDeleteConfirmation() {
        _showDeleteConfirmation.value = true
    }

    fun hideDeleteConfirmation() {
        _showDeleteConfirmation.value = false
    }

    private var targetAlbumName: String? = null
    private var targetAlbumId: String? = null

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedMediaIds.value = emptySet()
            _isAlbumCreationPending.value = false
            targetAlbumName = null
            targetAlbumId = null
        }
    }

    fun toggleMediaSelection(mediaId: String) {
        val current = _selectedMediaIds.value.toMutableSet()
        if (current.contains(mediaId)) current.remove(mediaId) else current.add(mediaId)
        
        if (current.isEmpty() && _isSelectionMode.value) {
            exitSelection()
        } else {
            _selectedMediaIds.value = current
        }
    }

    fun showCreateAlbumDialog() {
        _showCreateAlbumDialog.value = true
    }

    fun hideCreateAlbumDialog() {
        _showCreateAlbumDialog.value = false
    }

    private val _allPagingItems = mutableListOf<MediaItem>()
    // Nota: Para obtener los items reales seleccionados, los buscaremos por ID. 
    // En una implementación real con Paging, esto puede ser complejo si no están cargados.
    // Usaremos un truco: registrar los items que pasan por el DataSource o mantener una lista paralela.

    private val _isAlbumCreationPending = MutableStateFlow(false)
    val isAlbumCreationPending: StateFlow<Boolean> = _isAlbumCreationPending.asStateFlow()

    fun startAlbumCreation(name: String) {
        val currentSelection = _selectedMediaIds.value
        targetAlbumName = name
        _showCreateAlbumDialog.value = false

        if (currentSelection.isNotEmpty()) {
            // Proceder directamente a mover los elementos seleccionados
            saveSelectedToNewAlbum()
        } else {
            // Flujo normal: iniciar selección para el nuevo álbum
            _isSelectionMode.value = true
            _isAlbumCreationPending.value = true
            _selectedMediaIds.value = emptySet()

            // Insertar álbum temporalmente en el carrusel después del primer elemento (Todo)
            val currentAlbums = _albums.value.toMutableList()
            val tempAlbum = AlbumItem(
                id = "TEMP_ALBUM_${System.currentTimeMillis()}",
                name = name,
                thumbnail = "", 
                count = 0
            )
            if (currentAlbums.size > 1) {
                currentAlbums.add(1, tempAlbum)
            } else {
                currentAlbums.add(tempAlbum)
            }
            _albums.value = currentAlbums
        }
    }

    fun saveSelectedToNewAlbum() {
        val albumName = targetAlbumName ?: return
        val selectedIds = _selectedMediaIds.value
        if (selectedIds.isEmpty()) {
            // Si está vacío, solo creamos el folder
            viewModelScope.launch {
                repository.createAlbum(albumName)
                exitSelection()
                syncGallery() // Refrescar álbumes
            }
            return
        }

        viewModelScope.launch {
            _isMovingMedia.value = true
            try {
                // Necesitamos los objetos MediaItem reales. 
                val toMove = repository.getMediaByIds(selectedIds.toList())
                
                val success = repository.moveMediaToAlbum(toMove, albumName, targetAlbumId)
                if (success) {
                    // Refrescar inmediatamente
                    _selectedMediaIds.value = emptySet()
                    targetAlbumName = null
                    targetAlbumId = null
                    _isSelectionMode.value = false
                    
                    // Pequeña espera para asegurar que MediaStore se actualice
                    delay(1000) 
                    syncGallery()
                }
            } finally {
                _isMovingMedia.value = false
            }
        }
    }

    fun moveSelectedToExistingAlbum(album: AlbumItem) {
        _showMoveToAlbumDialog.value = false
        targetAlbumName = album.name
        targetAlbumId = album.id
        saveSelectedToNewAlbum()
    }

    fun exitSelection() {
        _isSelectionMode.value = false
        _isAlbumCreationPending.value = false
        _selectedMediaIds.value = emptySet()
        targetAlbumName = null
        targetAlbumId = null
    }

    // --------------------------------

    private val _isEditPermissionGranted = MutableStateFlow(
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.provider.MediaStore.canManageMedia(application)
        } else true
    )
    val isEditPermissionGranted: StateFlow<Boolean> = _isEditPermissionGranted.asStateFlow()

    fun checkEditPermission() {
        val granted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            true
        }
        android.util.Log.d("GalleryViewModel", "Check All Files permission: $granted")
        _isEditPermissionGranted.value = granted
    }

    fun requestEditPermission(context: android.content.Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
            }
        }
    }

    private val dateFormatter = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))

    init {
        checkEditPermission()
        viewModelScope.launch {
            delay(500)
            repository.syncLocalGallery()
            loadAlbums()
        }
        
        // Reactividad a cambios externos
        viewModelScope.launch {
            repository.mediaChanges
                .debounce(1000) // Evitar múltiples ráfagas de actualizaciones
                .collect {
                    syncGallery()
                }
        }
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            combine(
                repository.getLocalAlbums(showEmptyAlbums.value),
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
                    _albums.value = listOf(virtualAll, vaultVirtual) + filteredList
                } else {
                    _albums.value = listOf(virtualAll) + filteredList
                }
            }.collect()
        }
    }

    val availableImageExtensions: StateFlow<List<String>> = repository.getDistinctMimeTypes("LOCAL")
        .map { mimes ->
            listOf("Todas") + mimes
                .filter { it.startsWith("image/") }
                .map { it.split("/").last().uppercase() }
                .distinct()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf("Todas"))

    val availableVideoResolutions: StateFlow<List<String>> = repository.getAvailableVideoResolutions("LOCAL")
        .map { resolutions ->
            val labels = mutableSetOf<String>()
            resolutions.forEach { res ->
                val maxDim = maxOf(res.width, res.height)
                when {
                    maxDim >= 3840 -> labels.add("4K")
                    maxDim >= 2560 -> labels.add("2K")
                    maxDim >= 1920 -> labels.add("1080P")
                    maxDim >= 1280 -> labels.add("720P")
                    else -> labels.add("SD")
                }
            }
            listOf("Todas") + labels.toList().sortedByDescending {
                when(it) { "4K" -> 4; "2K" -> 3; "1080P" -> 2; "720P" -> 1; else -> 0 }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf("Todas"))

    @OptIn(ExperimentalCoroutinesApi::class)
    val sectionMetadata: StateFlow<Map<String, SectionMetadataRow>> = combine(
        _selectedImageFilter,
        _selectedVideoFilter,
        _selectedAlbum
    ) { imgExt, vidRes, albumId ->
        Triple(imgExt, vidRes, albumId)
    }.flatMapLatest { (imgExt: String?, vidRes: String?, albumId: String?) ->
        val mimeFilter = when {
            imgExt != null -> "image/${imgExt.lowercase()}"
            vidRes != null -> "video/%"
            else -> "%"
        }
        val (minW, minH) = when(vidRes) {
            "4K" -> 3840 to 2160
            "2K" -> 2560 to 1440
            "1080P" -> 1920 to 1080
            "720P" -> 1280 to 720
            else -> 0 to 0
        }
        repository.getAllSectionsMetadata("LOCAL", mimeFilter, albumId, minW, minH)
            .map { list: List<SectionMetadataRow> ->
                list.associateBy { row: SectionMetadataRow ->
                    try {
                        val parts = row.period.split("-")
                        val month = parts[0].toInt()
                        val year = parts[1]
                        val monthName = java.text.DateFormatSymbols(Locale("es", "ES")).months[month - 1]
                        "$monthName $year"
                    } catch (e: Exception) {
                        row.period
                    }
                }
            }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val availableFilters: StateFlow<List<String>> = sectionMetadata.map { metadata ->
        listOf("Todos") + metadata.keys.map { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString() } }
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf("Todos"))

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedItems: Flow<PagingData<GalleryUiModel>> = combine(
        _selectedFilter,
        _selectedImageFilter,
        _selectedVideoFilter,
        _selectedAlbum
    ) { date, imgExt, vidRes, albumId ->
        FilterState(date, imgExt, vidRes, albumId)
    }.distinctUntilChanged()
     .flatMapLatest { state ->
        val (date, imgExt, vidRes, albumId) = state
        var startRange = 0L
        var endRange = Long.MAX_VALUE
        if (date != null && date != "Todos") {
            try {
                dateFormatter.parse(date)?.let { d ->
                    val cal = Calendar.getInstance().apply { time = d }
                    startRange = cal.timeInMillis
                    cal.add(Calendar.MONTH, 1)
                    endRange = cal.timeInMillis
                }
            } catch (e: Exception) {}
        }

        val mimeFilter = when {
            imgExt != null -> "image/${imgExt.lowercase()}"
            vidRes != null -> "video/%"
            else -> "%"
        }

        val (minW, minH) = when(vidRes) {
            "4K" -> 3840 to 2160
            "2K" -> 2560 to 1440
            "1080P" -> 1920 to 1080
            "720P" -> 1280 to 720
            else -> 0 to 0
        }

        Pager(
            config = PagingConfig(
                pageSize = 40,
                prefetchDistance = 80,
                initialLoadSize = 120,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { repository.getPagedItems("LOCAL", startRange, endRange, mimeFilter, albumId, minW, minH) }
        ).flow
            .map { it.map { item -> 
                GalleryUiModel.Media(item.toDomain()) as GalleryUiModel 
            } }
            .map { pagingData ->
                pagingData.insertSeparators { before, after ->
                    if (after == null) return@insertSeparators null
                    val a = (after as GalleryUiModel.Media).item
                    val afterDate = formatDate(a.dateAdded)
                    val afterPeriod = SimpleDateFormat("MM-yyyy", Locale.US).format(Date(a.dateAdded))
                    
                    if (before == null) return@insertSeparators GalleryUiModel.Separator(dateLabel = afterDate, period = afterPeriod)
                    val b = (before as GalleryUiModel.Media).item
                    if (formatDate(b.dateAdded) != afterDate) {
                        GalleryUiModel.Separator(dateLabel = afterDate, period = afterPeriod)
                    } else null
                }
            }
    }.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewerPagingData: Flow<PagingData<GalleryUiModel>> = combine(
        _selectedFilter,
        _selectedImageFilter,
        _selectedVideoFilter,
        _selectedAlbum
    ) { date, imgExt, vidRes, albumId ->
        FilterState(date, imgExt, vidRes, albumId)
    }.distinctUntilChanged()
     .flatMapLatest { state ->
        val (date, imgExt, vidRes, albumId) = state
        var startRange = 0L
        var endRange = Long.MAX_VALUE
        if (date != null && date != "Todos") {
            try {
                dateFormatter.parse(date)?.let { d ->
                    val cal = Calendar.getInstance().apply { time = d }
                    startRange = cal.timeInMillis
                    cal.add(Calendar.MONTH, 1)
                    endRange = cal.timeInMillis
                }
            } catch (e: Exception) {}
        }

        val mimeFilter = when {
            imgExt != null -> "image/${imgExt.lowercase()}"
            vidRes != null -> "video/%"
            else -> "%"
        }

        val (minW, minH) = when(vidRes) {
            "4K" -> 3840 to 2160
            "2K" -> 2560 to 1440
            "1080P" -> 1920 to 1080
            "720P" -> 1280 to 720
            else -> 0 to 0
        }

        Pager(
            config = PagingConfig(
                pageSize = 40,
                prefetchDistance = 80,
                initialLoadSize = 120,
                enablePlaceholders = true,
                maxSize = 300 // Evitar que el cache de Paging crezca infinitamente
            ),
            pagingSourceFactory = { repository.getPagedItems("LOCAL", startRange, endRange, mimeFilter, albumId, minW, minH) }
        ).flow.map { pagingData ->
            pagingData.map { GalleryUiModel.Media(it.toDomain()) as GalleryUiModel }
        }
    }.cachedIn(viewModelScope)

    private fun formatDate(timestamp: Long): String {
        return dateFormatter.format(Date(timestamp)).replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString() }
    }

    fun changeColumns() {
        val next = when (columnCount.value) {
            3 -> 4
            4 -> 5
            5 -> 6
            6 -> 3
            else -> 4
        }
        settingsRepository.setColumnCount(next)
    }

    fun onFilterSelected(filter: String) {
        groupIdsCache.clear()
        _selectedFilter.value = if (filter == "Todos") null else filter
    }

    fun onImageFilterSelected(ext: String) {
        _selectedVideoFilter.value = null
        _selectedImageFilter.value = if (ext == "Todas") null else ext
    }

    fun onVideoFilterSelected(res: String) {
        groupIdsCache.clear()
        _selectedImageFilter.value = null
        _selectedVideoFilter.value = if (res == "Todas") null else res
    }

    fun toggleFilters() {
        _showFilters.value = !_showFilters.value
    }

    fun selectItem(item: MediaItem) {
        _selectedItem.value = item
    }

    fun deselectItem() {
        _selectedItem.value = null
    }

    fun openViewer(item: MediaItem, index: Int) {
        viewModelScope.launch {
            val date = _selectedFilter.value
            val imgExt = _selectedImageFilter.value
            val vidRes = _selectedVideoFilter.value
            val albumId = _selectedAlbum.value

            var startRange = 0L
            var endRange = Long.MAX_VALUE
            if (date != null && date != "Todos") {
                try {
                    dateFormatter.parse(date)?.let { d ->
                        val cal = Calendar.getInstance().apply { time = d }
                        startRange = cal.timeInMillis
                        cal.add(Calendar.MONTH, 1)
                        endRange = cal.timeInMillis
                    }
                } catch (e: Exception) {}
            }

            val mimeFilter = when {
                imgExt != null -> "image/${imgExt.lowercase()}"
                vidRes != null -> "video/%"
                else -> "%"
            }

            val (minW, minH) = when(vidRes) {
                "4K" -> 3840 to 2160
                "2K" -> 2560 to 1440
                "1080P" -> 1920 to 1080
                "720P" -> 1280 to 720
                else -> 0 to 0
            }

            val rank = repository.getMediaRank(item.id, "LOCAL", startRange, endRange, mimeFilter, albumId, minW, minH)
            
            _viewerItem.value = item
            _viewerIndex.value = rank
        }
    }

    fun rotateMedia(item: MediaItem) {
        val newRotation = (item.rotation + 90f) % 360f
        viewModelScope.launch {
            repository.updateMediaRotation(item, newRotation)
            // No necesitamos actualizar _viewerItem manualmente si el PagingSource emite de nuevo,
            // pero para una respuesta instantánea en el visor actualizamos el estado local
            _viewerItem.value = item.copy(rotation = newRotation)
        }
    }

    fun syncGallery() {
        groupIdsCache.clear()
        viewModelScope.launch {
            repository.syncLocalGallery()
            delay(500) // Small delay to let MediaStore update
            loadAlbums()
        }
    }

    private val _pendingIntent = MutableStateFlow<android.content.IntentSender?>(null)
    val pendingIntent: StateFlow<android.content.IntentSender?> = _pendingIntent.asStateFlow()

    private var pendingRenameData: Pair<MediaItem, String>? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    fun renameMedia(item: MediaItem, newName: String) {
        viewModelScope.launch {
            checkEditPermission()
            
            val lastDotIndex = item.title.lastIndexOf('.')
            val extension = if (lastDotIndex != -1) item.title.substring(lastDotIndex) else ""
            val cleanedNewName = newName.removeSuffix(extension).removeSuffix(".")
            val fullNewName = "$cleanedNewName$extension"
            
            when (val result = repository.renameMedia(item, fullNewName)) {
                is RenameResult.Success -> {
                    val updated = item.copy(title = fullNewName)
                    _selectedItem.value = updated
                    if (_viewerItem.value?.id == item.id) _viewerItem.value = updated
                    pendingRenameData = null
                }
                is RenameResult.PermissionRequired -> {
                    pendingRenameData = item to newName
                    _pendingIntent.value = result.intentSender
                }
                is RenameResult.Error -> {
                    pendingRenameData = null
                }
            }
        }
    }

    private var pendingDeleteItems: List<MediaItem>? = null
    private var pendingSecureItems: List<MediaItem>? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteSelectedMedia() {
        viewModelScope.launch {
            val selectedIds = _selectedMediaIds.value
            val toDelete = repository.getMediaByIds(selectedIds.toList())
            
            if (toDelete.isEmpty()) return@launch

            val result = repository.deleteMedia(toDelete)
            when (result) {
                is com.my_gallery.data.repository.DeleteResult.Success -> {
                    _selectedMediaIds.value = emptySet()
                    _isSelectionMode.value = false
                    syncGallery()
                }
                is com.my_gallery.data.repository.DeleteResult.PermissionRequired -> {
                     // For API 30+, this intent performs the delete. We just need to sync after.
                     // For API < 30, we might need to retry? 
                     // Actually RecoverableSecurityException usually requires retry.
                     // Let's assume we store them to retry if needed.
                     pendingDeleteItems = toDelete 
                     _pendingIntent.value = result.intentSender
                }
                is com.my_gallery.data.repository.DeleteResult.Error -> {
                    // Handle error (show toast?)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun secureSelectedMedia() {
        viewModelScope.launch {
            val selectedIds = _selectedMediaIds.value
            val toSecure = repository.getMediaByIds(selectedIds.toList())
            
            if (toSecure.isEmpty()) return@launch

            _isSecuringMedia.value = true
            try {
                val result = repository.secureMediaItems(toSecure)
                when (result) {
                    is com.my_gallery.data.repository.DeleteResult.Success -> {
                        _selectedMediaIds.value = emptySet()
                        _isSelectionMode.value = false
                        syncGallery()
                    }
                    is com.my_gallery.data.repository.DeleteResult.PermissionRequired -> {
                         pendingSecureItems = toSecure 
                         _pendingIntent.value = result.intentSender
                    }
                    is com.my_gallery.data.repository.DeleteResult.Error -> {
                        // Handle error (show toast?)
                    }
                }
            } finally {
                _isSecuringMedia.value = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun unsecureSelectedMedia() {
        viewModelScope.launch {
            val selectedIds = _selectedMediaIds.value
            val toUnsecure = repository.getMediaByIds(selectedIds.toList())
                .filter { it.albumId == "SECURE_VAULT" }
            
            if (toUnsecure.isEmpty()) return@launch

            _isUnsecuringMedia.value = true
            try {
                val result = repository.unsecureMediaItems(toUnsecure)
                when (result) {
                    is com.my_gallery.data.repository.DeleteResult.Success -> {
                        _selectedMediaIds.value = emptySet()
                        _isSelectionMode.value = false
                        syncGallery()
                    }
                    else -> {}
                }
            } finally {
                _isUnsecuringMedia.value = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun onPermissionResult(success: Boolean) {
        _pendingIntent.value = null
        if (success) {
            pendingRenameData?.let { (item, name) ->
                renameMedia(item, name)
            }
            pendingDeleteItems?.let { items ->
                // If API 30+, delete happened. If < 30, retry.
                // Simplest is to just call deleteSelectedMedia again (since items are still selected initially?)
                // But wait, if we call deleteMedia again on items that are already deleted (API 30+), it might fail or do nothing.
                // Let's just try to sync first. If items are gone, great.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                     _selectedMediaIds.value = emptySet()
                    _isSelectionMode.value = false
                    syncGallery()
                } else {
                    // Start delete again logic? 
                    // To keep it simple, we just retry delete.
                     deleteSelectedMedia()
                }
            }
            pendingSecureItems?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                     _selectedMediaIds.value = emptySet()
                    _isSelectionMode.value = false
                    syncGallery()
                } else {
                     secureSelectedMedia()
                }
            }
        }
        pendingRenameData = null
        pendingDeleteItems = null
        pendingSecureItems = null
    }

    fun clearPendingIntent() {
        _pendingIntent.value = null
    }

    fun closeViewer() {
        _viewerItem.value = null
    }

    fun toggleGroupSelection(headerLabel: String) {
        viewModelScope.launch {
            var start = 0L
            var end = Long.MAX_VALUE
            try {
                dateFormatter.parse(headerLabel)?.let { d ->
                    val cal = Calendar.getInstance().apply { time = d }
                    start = cal.timeInMillis
                    cal.add(Calendar.MONTH, 1)
                    end = cal.timeInMillis
                }
            } catch (e: Exception) { 
                return@launch 
            }
            
            val imgExt = _selectedImageFilter.value
            val vidRes = _selectedVideoFilter.value
            val albumId = _selectedAlbum.value // toggleAlbum sets null for ALL_VIRTUAL_ALBUM, so just use value
            
            val source = "LOCAL"
            
            val mimeFilter = when {
                imgExt != null -> "image/${imgExt.lowercase()}"
                vidRes != null -> "video/%"
                else -> "%"
            }

            val (minW, minH) = when(vidRes) {
                "4K" -> 3840 to 2160
                "2K" -> 2560 to 1440
                "1080P" -> 1920 to 1080
                "720P" -> 1280 to 720
                else -> 0 to 0
            }

            val groupIds = repository.getMediaIds(source, start, end, mimeFilter, albumId, minW, minH)
            
            val current = _selectedMediaIds.value
            val allInGroupSelected = groupIds.isNotEmpty() && groupIds.all { it in current }
            
            val newSet = current.toMutableSet()
            if (allInGroupSelected) {
                 newSet.removeAll(groupIds)
            } else {
                 newSet.addAll(groupIds)
            }
            
            if (newSet.isEmpty() && _isSelectionMode.value) {
                exitSelection()
            } else {
                _selectedMediaIds.value = newSet
                if (newSet.isNotEmpty() && !_isSelectionMode.value) {
                    _isSelectionMode.value = true
                }
            }
        }
    }
    fun toggleGroupSelection(label: String, period: String) {
        viewModelScope.launch {
            val mimeFilter = when {
                _selectedImageFilter.value != null -> "image/${_selectedImageFilter.value!!.lowercase()}"
                _selectedVideoFilter.value != null -> "video/%"
                else -> "%"
            }
            val ids = repository.getMediaIdsByPeriod("LOCAL", period, mimeFilter, _selectedAlbum.value)
            groupIdsCache[period] = ids // Guardamos en cache para isGroupSelected
            
            val current = _selectedMediaIds.value.toMutableSet()
            
            // Si ya están todos seleccionados, quitamos. Si no, añadimos todos.
            if (ids.all { it in current }) {
                current.removeAll(ids)
            } else {
                current.addAll(ids)
            }
            
            _selectedMediaIds.value = current
            if (current.isNotEmpty() && !_isSelectionMode.value) {
                _isSelectionMode.value = true
            }
        }
    }

    fun isGroupSelected(label: String, period: String, selectedIds: Set<String>): Boolean {
        // Obtenemos los IDs del cache. Si no están, no podemos asegurar que esté seleccionado.
        val ids = groupIdsCache[period] ?: return false
        if (ids.isEmpty()) return false
        
        // Verificamos si todos los IDs de este grupo están en el set de seleccionados
        return ids.all { it in selectedIds }
    }

    fun areAllSelectedSecured(): Boolean {
        if (_selectedMediaIds.value.isEmpty()) return false
        return _selectedAlbum.value == "SECURE_VAULT"
    }

    suspend fun decryptMediaToCache(item: MediaItem): String? {
        val file = repository.decryptMediaToCache(item.id, item.mimeType)
        return file?.absolutePath
    }

    fun clearDecryptedCache() {
        viewModelScope.launch {
            repository.clearDecryptedCache()
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearDecryptedCache()
    }
}

data class FilterState(
    val date: String?,
    val imgExt: String?,
    val vidRes: String?,
    val albumId: String?
)

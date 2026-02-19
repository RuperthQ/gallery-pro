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
import com.my_gallery.data.repository.MediaRepository
import com.my_gallery.data.repository.RenameResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import com.my_gallery.domain.model.AlbumItem

enum class GallerySource { LOCAL }

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val application: Application,
    private val repository: MediaRepository
) : ViewModel() {

    private val _albums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val albums: StateFlow<List<AlbumItem>> = _albums.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<String?>(null)
    val selectedAlbum: StateFlow<String?> = _selectedAlbum.asStateFlow()

    fun toggleAlbum(albumId: String?) {
        if (albumId == "ALL_VIRTUAL_ALBUM") {
            _selectedAlbum.value = null
        } else {
            _selectedAlbum.value = if (_selectedAlbum.value == albumId) null else albumId
        }
    }

    private val _columnCount = MutableStateFlow(4)
    val columnCount: StateFlow<Int> = _columnCount.asStateFlow()

    private val _selectedFilter = MutableStateFlow<String?>(null)
    val selectedFilter: StateFlow<String?> = _selectedFilter.asStateFlow()

    private val _selectedImageFilter = MutableStateFlow<String?>(null)
    val selectedImageFilter: StateFlow<String?> = _selectedImageFilter.asStateFlow()

    private val _selectedVideoFilter = MutableStateFlow<String?>(null)
    val selectedVideoFilter: StateFlow<String?> = _selectedVideoFilter.asStateFlow()

    private val _showFilters = MutableStateFlow(false)
    val showFilters: StateFlow<Boolean> = _showFilters.asStateFlow()

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
            
            repository.getLocalAlbums().collect { list ->
                val virtualAll = AlbumItem(
                    id = "ALL_VIRTUAL_ALBUM",
                    name = "Todo",
                    thumbnail = list.firstOrNull()?.thumbnail ?: "",
                    count = 0
                )
                _albums.value = listOf(virtualAll) + list
            }
        }
    }

    val availableFilters = flow {
        val filters = listOf("Todos", "Enero 2026", "Diciembre 2025", "Noviembre 2025", "Octubre 2025")
        emit(filters)
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf("Todos"))

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
                maxSize = PagingConfig.MAX_SIZE_UNBOUNDED,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { repository.getPagedItems("LOCAL", startRange, endRange, mimeFilter, albumId, minW, minH) }
        ).flow
            .map { it.map { item -> GalleryUiModel.Media(item.toDomain()) as GalleryUiModel } }
            .map { pagingData ->
                pagingData.insertSeparators { before, after ->
                    if (after == null) return@insertSeparators null
                    val a = (after as GalleryUiModel.Media).item
                    val afterDate = formatDate(a.dateAdded)
                    if (before == null) return@insertSeparators GalleryUiModel.Separator(afterDate)
                    val b = (before as GalleryUiModel.Media).item
                    if (formatDate(b.dateAdded) != afterDate) GalleryUiModel.Separator(afterDate) else null
                }
            }
    }.cachedIn(viewModelScope)

    private fun formatDate(timestamp: Long): String {
        return dateFormatter.format(Date(timestamp))
    }

    fun changeColumns() {
        _columnCount.value = when (_columnCount.value) {
            4 -> 5
            5 -> 6
            else -> 4
        }
    }

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = if (filter == "Todos") null else filter
    }

    fun onImageFilterSelected(ext: String) {
        _selectedVideoFilter.value = null
        _selectedImageFilter.value = if (ext == "Todas") null else ext
    }

    fun onVideoFilterSelected(res: String) {
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
        _viewerItem.value = item
        _viewerIndex.value = index
    }

    fun syncGallery() {
        viewModelScope.launch {
            repository.syncLocalGallery()
        }
    }

    private val _pendingIntent = MutableStateFlow<android.content.IntentSender?>(null)
    val pendingIntent: StateFlow<android.content.IntentSender?> = _pendingIntent.asStateFlow()

    private var pendingRenameData: Pair<MediaItem, String>? = null

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

    fun onPermissionResult(success: Boolean) {
        _pendingIntent.value = null
        if (success) {
            pendingRenameData?.let { (item, name) ->
                renameMedia(item, name)
            }
        }
        pendingRenameData = null
    }

    fun clearPendingIntent() {
        _pendingIntent.value = null
    }

    fun closeViewer() {
        _viewerItem.value = null
    }
}

data class FilterState(
    val date: String?,
    val imgExt: String?,
    val vidRes: String?,
    val albumId: String?
)

package com.my_gallery.ui.gallery

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.my_gallery.ui.gallery.components.*
import com.my_gallery.ui.gallery.utils.BiometricHandler
import com.my_gallery.ui.security.SecurityViewModel
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.glassBackground
import com.my_gallery.ui.theme.GalleryDesign.bottomPremiumBorder

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel(),
    securityViewModel: SecurityViewModel = hiltViewModel()
) {
    // --- ESTADOS COLECTADOS ---
    val columnCount by viewModel.columnCount.collectAsStateWithLifecycle()
    val showFilters by viewModel.showFilters.collectAsStateWithLifecycle()
    val selectedItem by viewModel.selectedItem.collectAsStateWithLifecycle()
    val viewerItem by viewModel.viewerItem.collectAsStateWithLifecycle()
    val pendingIntent by viewModel.pendingIntent.collectAsStateWithLifecycle()
    val isEditPermissionGranted by viewModel.isEditPermissionGranted.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val lockedAlbums by securityViewModel.lockedAlbums.collectAsStateWithLifecycle(initialValue = emptySet())
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedMediaIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val menuStyle by viewModel.menuStyle.collectAsStateWithLifecycle()
    val albumBehavior by viewModel.albumBehavior.collectAsStateWithLifecycle()
    
    // --- PAGING ITEMS ---
    val items = viewModel.pagedItems.collectAsLazyPagingItems()
    val viewerItems = viewModel.viewerPagingData.collectAsLazyPagingItems()

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    // --- MANEJO DE PERMISOS Y CICLO DE VIDA ---
    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> 
        viewModel.onPermissionResult(result.resultCode == android.app.Activity.RESULT_OK)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) viewModel.syncGallery()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.checkEditPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_MEDIA_VIDEO)
        } else arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        permissionLauncher.launch(permissions)
    }

    LaunchedEffect(pendingIntent) {
        pendingIntent?.let { intentSender ->
            intentSenderLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }

    // --- ACCIONES REUTILIZABLES ---
    val onAlbumClick: (com.my_gallery.domain.model.AlbumItem) -> Unit = { album ->
        if (album.id != "ALL_VIRTUAL_ALBUM" && securityViewModel.isAlbumLocked(album.id)) {
            BiometricHandler.authenticateAlbumAction(context, album, true, 
                onSuccess = { viewModel.toggleAlbum(album.id) })
        } else viewModel.toggleAlbum(album.id)
    }

    val onAlbumLongClick: (com.my_gallery.domain.model.AlbumItem) -> Unit = { album ->
        if (album.id != "ALL_VIRTUAL_ALBUM") {
            BiometricHandler.authenticateAlbumAction(context, album, securityViewModel.isAlbumLocked(album.id),
                onSuccess = { securityViewModel.toggleAlbumLock(album.id) })
        }
    }

    // --- UI ORQUESTADA ---
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        
        // 1. GRID PRINCIPAL (Contenido)
        GalleryMainGrid(
            items = items,
            viewModel = viewModel,
            securityViewModel = securityViewModel,
            columnCount = columnCount,
            menuStyle = menuStyle,
            albumBehavior = albumBehavior,
            albums = albums,
            selectedAlbum = selectedAlbum,
            lockedAlbums = lockedAlbums,
            isEditPermissionGranted = isEditPermissionGranted,
            isSelectionMode = isSelectionMode,
            selectedMediaIds = selectedMediaIds,
            onAlbumClick = onAlbumClick,
            onAlbumLongClick = onAlbumLongClick,
            onMediaClick = { media, index -> viewModel.openViewer(media, index) }
        )

        // 2. DETALLE DE METADATOS (BottomSheet)
        if (selectedItem != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.deselectItem() },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = GalleryDesign.AlphaGlassHigh),
                shape = GalleryDesign.HeaderShape,
                dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary) }
            ) {
                MetadataSheetContent(selectedItem!!, viewModel)
            }
        }

        // 3. CABECERAS Y CARRUSELES SUPERIORES
        GalleryHeaderOrchestrator(
            viewModel = viewModel,
            securityViewModel = securityViewModel,
            menuStyle = menuStyle,
            albumBehavior = albumBehavior,
            showFilters = showFilters,
            albums = albums,
            selectedAlbum = selectedAlbum,
            lockedAlbums = lockedAlbums,
            isViewerOpen = viewerItem != null,
            onAlbumClick = onAlbumClick,
            onAlbumLongClick = onAlbumLongClick
        )

        // 4. GRADIENTE DE NAVEGACIÓN INFERIOR
        AnimatedVisibility(
            visible = viewerItem == null && menuStyle == MenuStyle.BOTTOM_FLOATING,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(90.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.85f), MaterialTheme.colorScheme.background)))
            )
        }

        // 5. MENÚ FLOTANTE
        AnimatedVisibility(
            visible = viewerItem == null && menuStyle == MenuStyle.BOTTOM_FLOATING,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.navigationBarsPadding().align(Alignment.BottomCenter)
        ) {
            FloatingGalleryMenu(viewModel = viewModel)
        }

        // 6. DIÁLOGOS, VISOR Y AJUSTES
        GalleryDialogOrchestrator(
            viewModel = viewModel,
            securityViewModel = securityViewModel,
            albums = albums,
            viewerItems = viewerItems,
            onShowMetadata = { viewModel.selectItem(it) }
        )

        // --- BACK HANDLERS ---
        BackHandler(enabled = isSelectionMode && viewerItem == null) { viewModel.exitSelection() }
        val showSettings by viewModel.showSettings.collectAsStateWithLifecycle()
        BackHandler(enabled = showSettings) { viewModel.hideSettings() }
    }
}
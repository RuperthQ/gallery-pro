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
import com.my_gallery.ui.gallery.handlers.*
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingActions by viewModel.pendingActions.collectAsStateWithLifecycle()
    val selectedItem by viewModel.selectedItem.collectAsStateWithLifecycle()
    val viewerItem by viewModel.viewerItem.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val lockedAlbums by securityViewModel.lockedAlbums.collectAsStateWithLifecycle(initialValue = emptySet())
    val selectedMediaIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val menuStyle by viewModel.menuStyle.collectAsStateWithLifecycle()
    val albumBehavior by viewModel.albumBehavior.collectAsStateWithLifecycle()
    
    // --- PAGING ITEMS ---
    val items = viewModel.pagedItems.collectAsLazyPagingItems()
    val viewerItems = viewModel.viewerPagingData.collectAsLazyPagingItems()

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    // --- MANEJO DE PERMISOS Y CICLO DE VIDA ---
    setupPermissionsAndIntents(viewModel, pendingActions.intentSender)
    observeGalleryLifecycle(viewModel)

    // --- ACCIONES REUTILIZABLES ---
    val onAlbumClick = rememberAlbumClick(context, viewModel, securityViewModel)
    val onAlbumLongClick = rememberAlbumLongClick(context, securityViewModel)

    // --- UI ORQUESTADA ---
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        
        // 1. GRID PRINCIPAL (Contenido)
        GalleryMainGrid(
            items = items,
            viewModel = viewModel,
            securityViewModel = securityViewModel,
            columnCount = viewModel.columnCount.collectAsStateWithLifecycle().value,
            menuStyle = menuStyle,
            albumBehavior = albumBehavior,
            albums = albums,
            selectedAlbum = selectedAlbum,
            lockedAlbums = lockedAlbums,
            isEditPermissionGranted = uiState.isEditPermissionGranted,
            isSelectionMode = uiState.isSelectionMode,
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
            showFilters = uiState.showFilters,
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
        setupGalleryBackHandlers(
            viewModel = viewModel,
            isSelectionMode = uiState.isSelectionMode,
            showSettings = uiState.showSettings,
            showTrash = uiState.showTrash,
            selectedAlbumId = selectedAlbum,
            viewerItem = viewerItem
        )
    }
}
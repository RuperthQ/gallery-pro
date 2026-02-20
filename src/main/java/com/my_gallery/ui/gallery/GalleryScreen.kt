package com.my_gallery.ui.gallery

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.my_gallery.domain.model.MediaItem
import com.my_gallery.ui.gallery.components.CreateAlbumDialog
import com.my_gallery.ui.gallery.components.DeleteConfirmationDialog
import com.my_gallery.ui.gallery.components.GalleryItem
import com.my_gallery.ui.gallery.components.GalleryPlaceholder
import com.my_gallery.ui.gallery.components.HeaderLayout
import com.my_gallery.ui.gallery.components.LoadingOverlay
import com.my_gallery.ui.gallery.components.MetadataSheetContent
import com.my_gallery.ui.gallery.components.MoveToAlbumDialog
import com.my_gallery.ui.security.BiometricPromptManager
import androidx.fragment.app.FragmentActivity
import com.my_gallery.ui.security.SecurityViewModel
import com.my_gallery.ui.gallery.components.PermissionBanner
import com.my_gallery.ui.gallery.components.PremiumAlbumCarousel
import com.my_gallery.ui.gallery.components.SectionHeader
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.glassBackground
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel(),
    securityViewModel: SecurityViewModel = hiltViewModel()
) {
    val columnCount by viewModel.columnCount.collectAsStateWithLifecycle()
    val showFilters by viewModel.showFilters.collectAsStateWithLifecycle()
    val selectedItem by viewModel.selectedItem.collectAsStateWithLifecycle()
    val viewerItem by viewModel.viewerItem.collectAsStateWithLifecycle()
    val viewerIndex by viewModel.viewerIndex.collectAsStateWithLifecycle()
    val pendingIntent by viewModel.pendingIntent.collectAsStateWithLifecycle()
    val isEditPermissionGranted by viewModel.isEditPermissionGranted.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val lockedAlbums by securityViewModel.lockedAlbums.collectAsStateWithLifecycle(initialValue = emptySet<String>())
    
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedMediaIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val showCreateAlbumDialog by viewModel.showCreateAlbumDialog.collectAsStateWithLifecycle()
    val isMovingMedia by viewModel.isMovingMedia.collectAsStateWithLifecycle()
    
    val items: LazyPagingItems<GalleryUiModel> =
        viewModel.pagedItems.collectAsLazyPagingItems()
    val viewerItems: LazyPagingItems<GalleryUiModel> =
        viewModel.viewerPagingData.collectAsLazyPagingItems()
    val animatedColumns by animateIntAsState(targetValue = columnCount, label = "columnAnim")

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onPermissionResult(true)
        } else {
            viewModel.onPermissionResult(false)
        }
    }

    BackHandler(enabled = isSelectionMode && viewerItem == null) {
        viewModel.exitSelection()
    }

    val sheetState = rememberModalBottomSheetState()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.syncGallery()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.`Event`.ON_RESUME) {
                viewModel.checkEditPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    LaunchedEffect(pendingIntent) {
        pendingIntent?.let { intentSender ->
            val request = IntentSenderRequest.Builder(intentSender).build()
            intentSenderLauncher.launch(request)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(animatedColumns),
            contentPadding = PaddingValues(
                bottom = GalleryDesign.PaddingLarge,
                start = GalleryDesign.PaddingSmall,
                end = GalleryDesign.PaddingSmall
            ),
            horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall),
            verticalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HeaderLayout(
                    showFilters = showFilters,
                    viewModel = viewModel,
                    securityViewModel = securityViewModel,
                    modifier = Modifier.alpha(0f)
                )
            }

            if (albums.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PremiumAlbumCarousel(
                        albums = albums,
                        selectedAlbumId = selectedAlbum,
                        onAlbumClick = { albumInfo ->
                            if (albumInfo.id != "ALL_VIRTUAL_ALBUM" && securityViewModel.isAlbumLocked(albumInfo.id)) {
                                val biom = (context as? FragmentActivity)?.let { BiometricPromptManager(it) }
                                if (biom?.canAuthenticate() == true) {
                                    biom.authenticate(
                                        title = "Álbum Privado",
                                        subtitle = "Desbloquea este álbum",
                                        onSuccess = { viewModel.toggleAlbum(albumInfo.id) },
                                        onError = { /* Opcional mostrar error */ }
                                    )
                                } else {
                                     // Permitir si no tiene biometría configurada o mostrar error.
                                    // Para propósitos de este demo:
                                    viewModel.toggleAlbum(albumInfo.id) 
                                }
                            } else {
                                viewModel.toggleAlbum(albumInfo.id)
                            }
                        },
                        lockedAlbums = lockedAlbums,
                        onAlbumLongClick = { albumInfo ->
                            if (albumInfo.id != "ALL_VIRTUAL_ALBUM") {
                                val biom = (context as? FragmentActivity)?.let { BiometricPromptManager(it) }
                                if (biom?.canAuthenticate() == true) {
                                    biom.authenticate(
                                        title = if (securityViewModel.isAlbumLocked(albumInfo.id)) "Desbloquear Álbum" else "Bloquear Álbum",
                                        subtitle = "Autorización requerida",
                                        onSuccess = { securityViewModel.toggleAlbumLock(albumInfo.id) },
                                        onError = { /* Opcional error */ }
                                    )
                                } else {
                                    securityViewModel.toggleAlbumLock(albumInfo.id)
                                }
                            }
                        }
                    )
                }
            }

            if (!isEditPermissionGranted) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PermissionBanner(onGrantClick = { viewModel.requestEditPermission(context) })
                }
            }

            items(
                count = items.itemCount,
                key = items.itemKey { 
                    when(it) {
                        is GalleryUiModel.Separator -> "sep_${it.dateLabel}"
                        is GalleryUiModel.Media -> it.item.id
                    }
                },
                span = { index ->
                    val span = when (items[index]) {
                        is GalleryUiModel.Separator -> maxLineSpan
                        else -> 1
                    }
                    GridItemSpan(span)
                }
            ) { index ->
                items[index]?.let { model ->
                    when (model) {
                        is GalleryUiModel.Separator -> {
                            val metadata by viewModel.sectionMetadata.collectAsStateWithLifecycle()
                            val isChecked = remember(selectedMediaIds, model.dateLabel) {
                                viewModel.isGroupSelected(model.dateLabel, model.period, selectedMediaIds)
                            }
                            // Pass metadata to header
                            SectionHeader(
                                label = model.dateLabel,
                                metadata = metadata[model.dateLabel],
                                isChecked = isChecked,
                                onToggleCheck = { viewModel.toggleGroupSelection(model.dateLabel, model.period) }
                            )
                        }
                        is GalleryUiModel.Media -> {
                            GalleryItem(
                                item = model.item,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedMediaIds.contains(model.item.id),
                                onClick = { viewModel.openViewer(model.item, index) },
                                onLongClick = { viewModel.selectItem(model.item) },
                                onToggleSelection = { viewModel.toggleMediaSelection(model.item.id) }
                            )
                        }
                    }
                }
            }

            if (items.itemCount == 0 && items.loadState.refresh is LoadState.NotLoading) {
                 item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(GalleryDesign.PaddingLarge), contentAlignment = Alignment.Center) {
                        Text("No hay elementos para mostrar", color = Color.Gray)
                    }
                 }
            }

            if (items.loadState.refresh is LoadState.Loading) {
                items(20) { GalleryPlaceholder() }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(GalleryDesign.PaddingLarge),
                    contentAlignment = Alignment.Center
                ) {
                    if (items.loadState.append is LoadState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(GalleryDesign.IconSizeNormal))
                    }
                }
            }
        }

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

        AnimatedVisibility(
            visible = viewerItem == null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GalleryDesign.HeaderFullShape)
                    .premiumBorder(shape = GalleryDesign.HeaderFullShape, alpha = GalleryDesign.AlphaBorderLight)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .glassBackground()
                        .blur(GalleryDesign.BlurRadius)
                )

                HeaderLayout(
                    showFilters = showFilters,
                    viewModel = viewModel,
                    securityViewModel = securityViewModel
                )
            }
        }

        var itemForTransition by remember { mutableStateOf<MediaItem?>(null) }
        LaunchedEffect(viewerItem) {
            if (viewerItem != null) itemForTransition = viewerItem
        }

        AnimatedVisibility(
            visible = viewerItem != null,
            enter = fadeIn(animationSpec = tween(GalleryDesign.ViewerAnimSlow)) + scaleIn(initialScale = GalleryDesign.ViewerScaleTransition, animationSpec = tween(GalleryDesign.ViewerAnimSlow)),
            exit = fadeOut(animationSpec = tween(GalleryDesign.ViewerAnimSlow)) + scaleOut(targetScale = GalleryDesign.ViewerScaleTransition, animationSpec = tween(GalleryDesign.ViewerAnimSlow))
        ) {
            val currentItem = viewerItem ?: itemForTransition
            if (currentItem != null) {
                MediaViewerScreen(
                    item = currentItem,
                    items = viewerItems,
                    initialIndex = viewerIndex,
                    viewModel = viewModel,
                    onClose = { viewModel.closeViewer() },
                    onShowMetadata = { viewModel.selectItem(currentItem) }
                )
            }
        }

        // --- Diálogos de Álbum y Overlays ---
        if (showCreateAlbumDialog) {
            CreateAlbumDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.hideCreateAlbumDialog() }
            )
        }

        val showMoveToAlbumDialog by viewModel.showMoveToAlbumDialog.collectAsStateWithLifecycle()
        if (showMoveToAlbumDialog) {
            MoveToAlbumDialog(
                albums = albums,
                viewModel = viewModel,
                onDismiss = { viewModel.hideMoveToAlbumDialog() }
            )
        }

        val showDeleteConfirmation by viewModel.showDeleteConfirmation.collectAsStateWithLifecycle()
        if (showDeleteConfirmation) {
            DeleteConfirmationDialog(
                count = selectedMediaIds.size,
                onConfirm = { 
                    viewModel.hideDeleteConfirmation()
                    viewModel.deleteSelectedMedia()
                },
                onDismiss = { viewModel.hideDeleteConfirmation() }
            )
        }

        if (isMovingMedia) {
            LoadingOverlay(message = "Moviendo archivos...")
        }
    }
}
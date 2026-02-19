package com.my_gallery.ui.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.my_gallery.ui.gallery.components.*
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.glassBackground
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel()
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
    
    val items: LazyPagingItems<GalleryUiModel> =
        viewModel.pagedItems.collectAsLazyPagingItems()
    val animatedColumns by animateIntAsState(targetValue = columnCount, label = "columnAnim")

    val sheetState = rememberModalBottomSheetState()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.syncGallery()
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
            val activity = context as? androidx.activity.ComponentActivity
            activity?.startIntentSenderForResult(
                intentSender, 
                1001, 
                null, 0, 0, 0
            )
            viewModel.clearPendingIntent()
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
                    modifier = Modifier.alpha(0f)
                )
            }

            if (albums.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PremiumAlbumCarousel(
                        albums = albums,
                        selectedAlbumId = selectedAlbum,
                        onAlbumClick = { viewModel.toggleAlbum(it.id) }
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
                            SectionHeader(
                                label = model.dateLabel,
                                metadata = metadata[model.dateLabel]
                            )
                        }
                        is GalleryUiModel.Media -> {
                            GalleryItem(
                                item = model.item,
                                onClick = { viewModel.openViewer(model.item, index) },
                                onLongClick = { viewModel.selectItem(model.item) }
                            )
                        }
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
                    viewModel = viewModel
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
                    items = items,
                    initialIndex = viewerIndex,
                    viewModel = viewModel,
                    onClose = { viewModel.closeViewer() },
                    onShowMetadata = { viewModel.selectItem(currentItem) }
                )
            }
        }
    }
}
package com.my_gallery.ui.gallery

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.my_gallery.domain.model.AlbumItem
import com.my_gallery.domain.model.MediaItem
import com.my_gallery.ui.components.shimmerEffect
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.glassBackground
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val columnCount by viewModel.columnCount.collectAsStateWithLifecycle()
    val currentSource by viewModel.currentSource.collectAsStateWithLifecycle()
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
            viewModel.syncGallery() // Sincronizamos local al recibir permiso
        }
    }

    // Auto-solicitud de permisos al entrar
    LaunchedEffect(Unit) {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted) {
            permissionLauncher.launch(permissions)
        }
    }

    // --- RE-CHECK PERMISSIONS ON RESUME ---
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkEditPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onPermissionResult(result.resultCode == Activity.RESULT_OK)
    }

    val manageMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        viewModel.checkEditPermission()
    }

    LaunchedEffect(pendingIntent) {
        pendingIntent?.let { sender ->
            val request = androidx.activity.result.IntentSenderRequest.Builder(sender).build()
            intentSenderLauncher.launch(request)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Capa de Contenido (Grid) - Ocupa toda la pantalla
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
                    currentSource = currentSource,
                    showFilters = showFilters,
                    viewModel = viewModel,
                    modifier = Modifier.alpha(0f)
                )
            }

            // --- CAROUSEL DE ÁLBUMES (PRO) ---
            if (albums.isNotEmpty() && currentSource == GallerySource.LOCAL) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PremiumAlbumCarousel(
                        albums = albums,
                        selectedAlbumId = selectedAlbum,
                        onAlbumClick = { viewModel.toggleAlbum(it.id) }
                    )
                }
            }
            
            // --- BANNER DE PERMISO PRO (Cubre toda la galería) ---
            if (!isEditPermissionGranted) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PermissionBanner(
                        onGrantClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                manageMediaLauncher.launch(intent)
                            } else {
                                viewModel.checkEditPermission()
                            }
                        }
                    )
                }
            }

                items(
                count = items.itemCount,
                key = { index: Int ->
                    when (val item: GalleryUiModel? = items[index]) {
                        is GalleryUiModel.Media -> item.item.id
                        is GalleryUiModel.Separator -> item.dateLabel
                        null -> "placeholder_$index"
                    }
                },
                span = { index: Int ->
                    val item: GalleryUiModel? = items[index]
                    if (item is GalleryUiModel.Separator) {
                        GridItemSpan(animatedColumns)
                    } else {
                        GridItemSpan(1)
                    }
                }
            ) { index: Int ->
                when (val item: GalleryUiModel? = items[index]) {
                    is GalleryUiModel.Media -> GalleryItem(
                        item = item.item,
                        onClick = { viewModel.openViewer(item.item, index) },
                        onLongClick = { viewModel.selectItem(item.item) }
                    )
                    is GalleryUiModel.Separator -> SectionHeader(item.dateLabel)
                    null -> GalleryPlaceholder()
                }
            }

            // --- CARGANDO / ESTADO VACÍO / ERROR ---
            when (val refreshState = items.loadState.refresh) {
                is LoadState.Loading -> {
                    if (items.itemCount == 0) {
                        item(span = { GridItemSpan(animatedColumns) }) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(GalleryDesign.ThumbImageSize.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                is LoadState.Error -> {
                    item(span = { GridItemSpan(animatedColumns) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(GalleryDesign.ThumbImageSize.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Error al cargar la galería", color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(GalleryDesign.PaddingMedium))
                                Button(onClick = { items.retry() }) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                }
                is LoadState.NotLoading -> {
                    if (items.itemCount == 0) {
                        item(span = { GridItemSpan(animatedColumns) }) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(400.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = Color.Gray.copy(alpha = GalleryDesign.AlphaOverlay),
                                        modifier = Modifier.size(GalleryDesign.IconSizeAction * 1.5f)
                                    )
                                    Spacer(modifier = Modifier.height(GalleryDesign.PaddingMedium))
                                    Text("No se encontraron medios", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(GalleryDesign.PaddingSmall))
                                    
                                    val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        listOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_MEDIA_VIDEO)
                                    } else {
                                        listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                    
                                    val hasPermissions = permissions.all {
                                        androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    }

                                    if (!hasPermissions && currentSource == GallerySource.LOCAL) {
                                        Button(onClick = { permissionLauncher.launch(permissions.toTypedArray()) }) {
                                            Text("Conceder Permisos")
                                        }
                                    } else {
                                        Text("Verifica los filtros o intenta cambiar de fuente", 
                                            style = MaterialTheme.typography.bodySmall, 
                                            color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- MODAL DE METADATA "PRO" ---
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

        // 2. Capa de Header Real (Fijo arriba con Glassmorphism)
        androidx.compose.animation.AnimatedVisibility(
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
                // Fondo difuminado real
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .glassBackground()
                        .blur(GalleryDesign.BlurRadius)
                )

                // Contenido Visible
                HeaderLayout(
                    currentSource = currentSource,
                    showFilters = showFilters,
                    viewModel = viewModel
                )
            }
        }

        // --- VISOR PRO FULL SCREEN (Al final para que esté encima de todo) ---
        // Truco Premium: Mantenemos el item para que la animación de salida no sea "tosca"
        var itemForTransition by remember { mutableStateOf<MediaItem?>(null) }
        LaunchedEffect(viewerItem) {
            if (viewerItem != null) itemForTransition = viewerItem
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = viewerItem != null,
            enter = fadeIn(animationSpec = tween(GalleryDesign.ViewerAnimSlow)) + scaleIn(initialScale = GalleryDesign.ViewerScaleTransition, animationSpec = tween(GalleryDesign.ViewerAnimSlow)),
            exit = fadeOut(animationSpec = tween(GalleryDesign.ViewerAnimSlow)) + scaleOut(targetScale = GalleryDesign.ViewerScaleTransition, animationSpec = tween(GalleryDesign.ViewerAnimSlow))
        ) {
            // Usamos el item persistido para que no desaparezca a mitad de animación
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

/**
 * Componente unificado para mantener consistencia entre el Header real y el Ghost Header.
 */
@Composable
fun HeaderLayout(
    currentSource: GallerySource,
    showFilters: Boolean,
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .padding(bottom = GalleryDesign.PaddingTiny)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GalleryDesign.PaddingSmall, vertical = GalleryDesign.HeaderVerticalSpacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.toggleSource() }) {
                Icon(
                    imageVector = if (currentSource == GallerySource.CLOUD)
                        Icons.Default.Cloud else Icons.Default.PhoneAndroid,
                    contentDescription = "Cambiar fuente",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(GalleryDesign.IconSizeNormal)
                )
            }

            Text(
                "Galería Pro",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.toggleFilters() }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Mostrar filtros",
                        tint = if (showFilters) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(GalleryDesign.IconSizeNormal)
                    )
                }
                IconButton(onClick = { viewModel.changeColumns() }) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Cambiar columnas",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(GalleryDesign.IconSizeNormal)
                    )
                }
            }
        }
        
        AnimatedVisibility(
            visible = showFilters,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(vertical = GalleryDesign.PaddingSmall)) {
                FilterSection(label = "Fecha", viewModel = viewModel) {
                    FilterRow(viewModel)
                }
                Spacer(modifier = Modifier.height(GalleryDesign.PaddingSmall))
                FilterSection(label = "Imágenes", viewModel = viewModel) {
                    ImageFilterRow(viewModel)
                }
                Spacer(modifier = Modifier.height(GalleryDesign.PaddingSmall))
                FilterSection(label = "Videos", viewModel = viewModel) {
                    VideoFilterRow(viewModel)
                }
                Spacer(modifier = Modifier.height(GalleryDesign.PaddingMedium))
            }
        }
    }
}

@Composable
fun FilterSection(
    label: String,
    viewModel: GalleryViewModel,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = GalleryDesign.PaddingLarge, vertical = GalleryDesign.PaddingTiny),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = GalleryDesign.AlphaOverlay),
                modifier = Modifier.size(GalleryDesign.IconSizeExtraSmall)
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            content()
        }
    }
}

@Composable
fun FilterRow(viewModel: GalleryViewModel) {
    val filters by viewModel.availableFilters.collectAsStateWithLifecycle()
    val selected by viewModel.selectedFilter.collectAsStateWithLifecycle()

    LazyRow(
        contentPadding = PaddingValues(horizontal = GalleryDesign.PaddingLarge),
        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingMedium),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(filters.size) { index ->
            val filter = filters[index]
            val isSelected = (filter == "Todos" && selected == null) || filter == selected

            GalleryFilterChip(
                label = filter,
                isSelected = isSelected,
                onClick = { viewModel.onFilterSelected(filter) }
            )
        }
    }
}


@Composable
fun ImageFilterRow(viewModel: GalleryViewModel) {
    val extensions by viewModel.availableImageExtensions.collectAsStateWithLifecycle()
    val selected by viewModel.selectedImageFilter.collectAsStateWithLifecycle()

    LazyRow(
        contentPadding = PaddingValues(horizontal = GalleryDesign.PaddingLarge),
        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingMedium),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(extensions.size) { index ->
            val ext = extensions[index]
            val isSelected = (ext == "Todas" && selected == null) || ext == selected

            GalleryFilterChip(
                label = ext,
                isSelected = isSelected,
                onClick = { viewModel.onImageFilterSelected(ext) }
            )
        }
    }
}

@Composable
fun VideoFilterRow(viewModel: GalleryViewModel) {
    val resolutions by viewModel.availableVideoResolutions.collectAsStateWithLifecycle()
    val selected by viewModel.selectedVideoFilter.collectAsStateWithLifecycle()

    LazyRow(
        contentPadding = PaddingValues(horizontal = GalleryDesign.PaddingLarge),
        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingMedium),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(resolutions.size) { index ->
            val res = resolutions[index]
            val isSelected = (res == "Todas" && selected == null) || res == selected

            GalleryFilterChip(
                label = res,
                isSelected = isSelected,
                onClick = { viewModel.onVideoFilterSelected(res) }
            )
        }
    }
}

@Composable
fun GalleryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(GalleryDesign.ButtonHeight)
            .clip(GalleryDesign.FilterShape)
            .then(
                if (isSelected) {
                    Modifier.background(GalleryDesign.primaryGradient())
                } else {
                    Modifier.premiumBorder(
                        width = GalleryDesign.BorderWidthBold,
                        shape = GalleryDesign.FilterShape,
                        alpha = GalleryDesign.AlphaBorderDefault
                    )
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = GalleryDesign.PaddingMedium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SectionHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = GalleryDesign.PaddingSmall,
                bottom = GalleryDesign.PaddingSmall,
                start = GalleryDesign.PaddingSmall
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryItem(
    item: MediaItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var isLoaded by remember { mutableStateOf(false) }

    val imageRequest = remember(item.thumbnail) {
        ImageRequest.Builder(context)
            .data(item.thumbnail)
            .size(GalleryDesign.ThumbImageSize, GalleryDesign.ThumbImageSize)
            .precision(coil.size.Precision.INEXACT)
            .crossfade(true)
            .allowHardware(true)
            .build()
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(GalleryDesign.CardShape)
            .premiumBorder(shape = GalleryDesign.CardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = GalleryDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = GalleryDesign.ElevationSmall)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isLoaded) {
                Box(Modifier.fillMaxSize().shimmerEffect())
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    if (state is coil.compose.AsyncImagePainter.State.Success) {
                        isLoaded = true
                    }
                }
            )

            if (item.mimeType.startsWith("video")) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(GalleryDesign.PaddingSmall)
                        .size(GalleryDesign.IconSizeLarge)
                        .background(
                            Color.Black.copy(alpha = GalleryDesign.AlphaOverlay),
                            GalleryDesign.OverlayShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Video",
                        modifier = Modifier.size(GalleryDesign.IconSizeSmall),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryPlaceholder() {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(GalleryDesign.CardShape)
            .shimmerEffect()
    )
}

@Composable
fun MetadataSheetContent(item: MediaItem, viewModel: GalleryViewModel) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(GalleryDesign.PaddingLarge)
            .navigationBarsPadding()
    ) {
        // Fila de Metadata Unificada (Ahora actúa como Header Pro)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = GalleryDesign.PaddingMedium),
            horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall)
        ) {
            MetadataColumnItem(
                icon = Icons.Default.SdStorage,
                label = "Tamaño",
                value = formatFileSize(item.size),
                modifier = Modifier.weight(1f)
            )
            MetadataColumnItem(
                icon = Icons.Default.Straighten,
                label = "Resolución",
                value = if (item.width > 0) "${item.width}x${item.height}" else "---",
                modifier = Modifier.weight(1f)
            )
            MetadataColumnItem(
                icon = Icons.Default.Info,
                label = "Tipo",
                value = item.mimeType.split("/").last().uppercase(),
                modifier = Modifier.weight(1f)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall)) {
            MetadataEditItem(
                icon = Icons.Default.Title,
                label = "Nombre del archivo",
                initialValue = item.title,
                onSave = { newName -> viewModel.renameMedia(item, newName) }
            )
            MetadataItem(
                icon = Icons.Default.CalendarMonth,
                label = "Fecha de captura",
                value = dateFormatter.format(Date(item.dateAdded))
            )
        }
        
        Spacer(modifier = Modifier.height(GalleryDesign.PaddingLarge))
    }
}

@Composable
fun MetadataEditItem(
    icon: ImageVector,
    label: String,
    initialValue: String,
    onSave: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    
    // Separamos nombre de extensión para bloquear esta última
    val lastDotIndex = initialValue.lastIndexOf('.')
    val namePart = if (lastDotIndex != -1) initialValue.substring(0, lastDotIndex) else initialValue
    val extPart = if (lastDotIndex != -1) initialValue.substring(lastDotIndex) else ""
    
    var textValue by remember(initialValue) { mutableStateOf(namePart) }
    val hasChanged = textValue != namePart

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GalleryDesign.CardShape)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            .padding(GalleryDesign.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(GalleryDesign.IconSizeNormal)
        )
        Spacer(modifier = Modifier.width(GalleryDesign.PaddingMedium))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEditing) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f, fill = false),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = extPart,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    Text(
                        text = initialValue,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        IconButton(
            onClick = {
                if (isEditing) {
                    if (hasChanged && textValue.isNotBlank()) {
                        onSave(textValue)
                    }
                    isEditing = false
                } else {
                    isEditing = true
                }
            }
        ) {
            Icon(
                imageVector = if (isEditing) {
                    if (hasChanged) Icons.Default.Check else Icons.Default.Close
                } else {
                    Icons.Default.Edit
                },
                contentDescription = if (isEditing) "Guardar" else "Editar",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(GalleryDesign.IconSizeSmall)
            )
        }
    }
}

@Composable
fun PermissionBanner(onGrantClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(GalleryDesign.PaddingMedium)
            .premiumBorder(shape = GalleryDesign.CardShape),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        shape = GalleryDesign.CardShape
    ) {
        Row(
            modifier = Modifier.padding(GalleryDesign.PaddingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(GalleryDesign.IconSizeAction)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SdStorage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(GalleryDesign.PaddingLarge))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Permiso de Edición Pro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Para renombrar y organizar tu galería sin interrupciones, activa el acceso avanzado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(GalleryDesign.PaddingSmall))
            Button(
                onClick = onGrantClick,
                shape = GalleryDesign.FilterShape,
                contentPadding = PaddingValues(horizontal = GalleryDesign.PaddingMedium),
                modifier = Modifier.height(GalleryDesign.ButtonHeight)
            ) {
                Text("Activar", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun PremiumAlbumCarousel(
    albums: List<AlbumItem>,
    selectedAlbumId: String?,
    onAlbumClick: (AlbumItem) -> Unit
) {
    val density = LocalDensity.current
    val gridPadding = with(density) { GalleryDesign.PaddingSmall.roundToPx() }
    
    // El Carousel se expande lo justo para que el zoom sutil (1.05x) no se corte.
    // Usamos un offset menor para que no se sienta que se sale de la pantalla.
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall),
        contentPadding = PaddingValues(horizontal = 12.dp), // Suficiente para zoom 1.05x
        modifier = Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                // Alineación Pro: Queremos el borde del círculo a 8dp del borde de la pantalla.
                // 1. Padding 12dp + margen interno item 3dp (74w - 68circle / 2) = 15dp.
                // 2. Queremos 8dp. Desplazamiento = 8dp - 15dp = -7dp.
                val offsetPx = with(density) { 7.dp.roundToPx() }
                val extendedWidth = constraints.maxWidth + (offsetPx * 2)
                val placeable = measurable.measure(constraints.copy(maxWidth = extendedWidth))
                
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(-offsetPx, 0)
                }
            }
            .graphicsLayer(clip = false)
            .padding(vertical = GalleryDesign.PaddingSmall)
    ) {
        items(albums) { album ->
            AlbumCircleItem(
                album = album,
                isSelected = (album.id == "ALL_VIRTUAL_ALBUM" && selectedAlbumId == null) || (album.id == selectedAlbumId),
                onClick = { onAlbumClick(album) }
            )
        }
    }
}

@Composable
fun AlbumCircleItem(
    album: AlbumItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f, label = "scale")
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(74.dp) // Más compacto en X
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .premiumBorder(
                    shape = CircleShape,
                    width = if (isSelected) 2.dp else 1.dp,
                    alpha = if (isSelected) 1f else 0.4f
                )
                .padding(3.dp)
                .clip(CircleShape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                )
            }
        }
        
        Spacer(modifier = Modifier.height(GalleryDesign.PaddingTiny))
        Text(
            text = album.name,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.4.sp
            ),
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}

@Composable
fun MetadataItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GalleryDesign.CardShape)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            .padding(GalleryDesign.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(GalleryDesign.IconSizeNormal)
        )
        Spacer(modifier = Modifier.width(GalleryDesign.PaddingMedium))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MetadataColumnItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(GalleryDesign.CardShape)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
            .padding(GalleryDesign.PaddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(GalleryDesign.IconSizeSmall)
        )
        Spacer(modifier = Modifier.height(GalleryDesign.PaddingTiny))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
}
package com.my_gallery.ui.gallery

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.my_gallery.ui.components.PremiumMenu
import com.my_gallery.ui.components.PremiumMenuItem
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.my_gallery.domain.model.MediaItem
import com.my_gallery.ui.components.VideoPlayer
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.abs

@Composable
fun MediaViewerScreen(
    item: MediaItem, 
    items: LazyPagingItems<GalleryUiModel>,
    initialIndex: Int,
    viewModel: GalleryViewModel,
    onClose: () -> Unit,
    onShowMetadata: () -> Unit
) {
    var uiVisible by remember { mutableStateOf(true) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var showUnlockOverlay by remember { mutableStateOf(false) }
    var isLockingPersist by remember { mutableStateOf(false) } 
    var globalScale by remember { mutableFloatStateOf(1f) }
    var showMenu by remember { mutableStateOf(false) }
    var currentVideoIsHorizontal by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val autoplayEnabled by viewModel.autoplayEnabled.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = initialIndex) { items.itemCount }
    val thumbnailListState = rememberLazyListState()

    // BackHandler inteligente: Lock > Menu > Pantalla Completa > Cerrar
    androidx.activity.compose.BackHandler(enabled = true) {
        when {
            isLocked -> {
                showUnlockOverlay = true
            }
            showMenu -> showMenu = false
            isFullScreen -> isFullScreen = false
            else -> onClose()
        }
    }

    // Auto-ocultar el botón de desbloqueo
    LaunchedEffect(showUnlockOverlay) {
        if (showUnlockOverlay) {
            kotlinx.coroutines.delay(GalleryDesign.ViewerAnimLong.toLong())
            showUnlockOverlay = false
        }
    }

    // Resetear el overlay si se desbloquea externamente
    LaunchedEffect(isLocked) {
        if (!isLocked) showUnlockOverlay = false
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidth = viewportWidth()
    val view = LocalView.current
    
    // Evitar que la pantalla se apague mientras se ve contenido
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    // Control de Barras de Sistema en FullScreen
    LaunchedEffect(isFullScreen) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (isFullScreen) {
            controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            // Al salir de FS, restauramos la orientación por si acaso
            (view.context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Sincronización: Cuando el pager cambia, el carrusel de abajo centra la miniatura activa
    LaunchedEffect(pagerState.currentPage) {
        val itemSizePx = with(density) { GalleryDesign.ViewerThumbSize.toPx() }
        val screenWidthPx = with(density) { screenWidth.toPx() }
        val centerOffset = (screenWidthPx / 2) - (itemSizePx / 2)
        
        thumbnailListState.animateScrollToItem(
            index = pagerState.currentPage,
            scrollOffset = -centerOffset.toInt()
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isLocked && globalScale <= 1f, // BLOQUEO: Si está bloqueado o hay zoom, no hay swipe
            pageSpacing = GalleryDesign.PaddingLarge
        ) { pageIndex ->
            val uiModel = items[pageIndex]
            if (uiModel is GalleryUiModel.Media) {
                // Cálculo de transición de "Repintado" (Fade + Zoom + Static Translation)
                val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                
                // Repaint Interpolation: La imagen no se mueve, solo fluye
                val alphaFactor = (1f - absOffset).coerceIn(0f, 1f)
                val scaleFactor = GalleryDesign.ViewerScalePagerDown + ((1f - GalleryDesign.ViewerScalePagerDown) * (1f - absOffset))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Efecto Premium: Parallax suave + Escala
                            // Ya NO contrarrestamos translationX para permitir el deslizamiento
                            val scale = GalleryDesign.ViewerScalePagerDown + 
                                (1f - GalleryDesign.ViewerScalePagerDown) * (1f - absOffset)
                            scaleX = scale
                            scaleY = scale
                            alpha = (1f - absOffset).coerceIn(0.5f, 1f)
                            
                            // Parallax: La imagen se mueve un poco más lento que el contenedor
                            val widthPx = size.width
                            translationX = pageOffset * widthPx * 0.2f
                        }
                ) {
                    if (uiModel.item.mimeType.startsWith("video/")) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { 
                                            if (isLocked) showUnlockOverlay = true 
                                            else uiVisible = !uiVisible 
                                        },
                                        onLongPress = { if (!isLocked) showMenu = true }
                                    )
                                }
                        ) {
                            VideoPlayer(
                                videoUrl = uiModel.item.url,
                                videoWidth = uiModel.item.width,
                                videoHeight = uiModel.item.height,
                                autoplayEnabled = autoplayEnabled,
                                isActive = pagerState.currentPage == pageIndex,
                                isFullScreen = isFullScreen,
                                showControls = uiVisible,
                                onFullScreenChange = { isFullScreen = it },
                                onControlsVisibilityChange = { if (!isLocked) uiVisible = it },
                                onVideoOrientationDetected = { currentVideoIsHorizontal = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        ZoomableImage(
                             item = uiModel.item,
                             onScaleChange = { globalScale = it },
                             onTap = { 
                                 if (isLocked) showUnlockOverlay = true 
                                 else uiVisible = !uiVisible 
                             },
                             onLongPress = { if (!isLocked) showMenu = true }
                         )
                    }
                }
            } else {
                // Si es un separador, mostramos un placeholder o saltamos (en este caso vacío)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cargando...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = GalleryDesign.AlphaOverlay))
                }
            }
        }

        // --- UI OVERLAY (Tools + Carousel) ---
        AnimatedVisibility(
            visible = uiVisible && !isFullScreen,
            enter = fadeIn(tween(GalleryDesign.ViewerAnimFast)) + scaleIn(initialScale = GalleryDesign.ViewerScaleOverlay, animationSpec = tween(GalleryDesign.ViewerAnimFast)),
            exit = fadeOut(tween(GalleryDesign.ViewerAnimFast)) + scaleOut(targetScale = GalleryDesign.ViewerScaleOverlay, animationSpec = tween(GalleryDesign.ViewerAnimFast))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Barra Superior (Close + Title + Menu)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(GalleryDesign.PaddingMedium)
                ) {
                    SmallFloatingActionButton(
                        onClick = onClose,
                        containerColor = Color.Black.copy(alpha = GalleryDesign.AlphaOverlay),
                        contentColor = Color.White,
                        modifier = Modifier.align(Alignment.CenterStart),
                        shape = GalleryDesign.CardShape
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cerrar") }

                    val currentItem = (items[pagerState.currentPage] as? GalleryUiModel.Media)?.item
                    if (currentItem != null) {
                        Surface(
                            color = Color.Black.copy(alpha = GalleryDesign.AlphaBorderLight),
                            shape = GalleryDesign.FilterShape,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = GalleryDesign.ViewerHeaderSafetyPadding)
                        ) {
                            Text(
                                text = currentItem.title,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = GalleryDesign.ViewerTitlePaddingH, vertical = GalleryDesign.ViewerTitlePaddingV)
                            )
                        }

                        // Menú Flotante Premium (Trigger)
                        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                            SmallFloatingActionButton(
                                onClick = { showMenu = true },
                                containerColor = Color.Black.copy(alpha = GalleryDesign.AlphaOverlay),
                                contentColor = Color.White,
                                shape = GalleryDesign.CardShape
                            ) {
                                Icon(Icons.Default.MoreVert, "Opciones")
                            }
                        }
                    }
                }

                // --- CAROUSEL ELEGANTE (Bottom) ---
                val currentUiModel = items[pagerState.currentPage]
                val isCurrentVideo = (currentUiModel as? GalleryUiModel.Media)?.item?.mimeType?.startsWith("video/") == true

                if (!isCurrentVideo) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = GalleryDesign.PaddingLarge)
                    ) {
                        // El Carrusel de Miniaturas
                        LazyRow(
                            state = thumbnailListState,
                            contentPadding = PaddingValues(horizontal = GalleryDesign.PaddingMedium),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(GalleryDesign.ViewerCarouselHeight),
                            horizontalArrangement = Arrangement.spacedBy(GalleryDesign.ViewerThumbSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(items.itemCount) { idx ->
                                val thumbModel = items[idx]
                                if (thumbModel is GalleryUiModel.Media) {
                                    val isSelected = idx == pagerState.currentPage
                                    
                                    // Animación de escala reactiva
                                    val thumbScale by animateFloatAsState(
                                        targetValue = if (isSelected) GalleryDesign.ViewerThumbScaleSelected else GalleryDesign.ViewerThumbScaleNormal,
                                        animationSpec = tween(GalleryDesign.ViewerAnimNormal, easing = FastOutSlowInEasing),
                                        label = "thumbScale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(GalleryDesign.ViewerThumbSize)
                                            .graphicsLayer {
                                                scaleX = thumbScale
                                                scaleY = thumbScale
                                            }
                                            .clip(GalleryDesign.CardShape)
                                            .then(
                                                if (isSelected) Modifier.rotatingPremiumBorder(GalleryDesign.CardShape)
                                                else Modifier.premiumBorder(shape = GalleryDesign.CardShape)
                                            )
                                            .clickable {
                                                scope.launch { pagerState.animateScrollToPage(idx) }
                                            }
                                    ) {
                                        AsyncImage(
                                            model = thumbModel.item.thumbnail,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().alpha(if (isSelected) 1f else 0.5f)
                                        )

                                        if (thumbModel.item.mimeType.startsWith("video/")) {
                                            Icon(
                                                imageVector = Icons.Default.PlayCircle,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = GalleryDesign.AlphaGlassLow),
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(GalleryDesign.IconSizeNormal)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SISTEMA DE MENÚ PREMIUM (Capa Superior) ---
        val currentItem = (items[pagerState.currentPage] as? GalleryUiModel.Media)?.item
        PremiumMenu(
            visible = showMenu,
            onDismiss = { showMenu = false },
            items = listOf(
                PremiumMenuItem(
                    label = "Compartir",
                    icon = Icons.Default.Share,
                    onClick = { 
                        showMenu = false
                        /* TODO: Implement Share */
                    }
                ),
                PremiumMenuItem(
                    label = "Información",
                    icon = Icons.Default.Info,
                    onClick = { 
                        showMenu = false
                        if (currentItem != null) onShowMetadata()
                    }
                ),
                PremiumMenuItem(
                    label = "Auto-reproducción",
                    icon = Icons.Default.Settings,
                    isSelected = autoplayEnabled,
                    showToggle = true,
                    onClick = { 
                        viewModel.toggleAutoplay()
                    }
                )
            )
        )

        // --- SISTEMA DE DESBLOQUEO (Overlay Invisible que captura toques si está bloqueado) ---
        if (isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showUnlockOverlay = true })
                    }
            )
        }

        // --- BOTÓN DE BLOQUEO (Especial para FullScreen + Solo Horizontales) ---
        val isHorizontal = if (currentItem?.mimeType?.startsWith("video/") == true) {
            currentVideoIsHorizontal
        } else {
            currentItem?.let { it.width >= it.height } ?: true
        }
        AnimatedVisibility(
            visible = isFullScreen && uiVisible && !isLocked && isHorizontal,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = GalleryDesign.PaddingViewerLockV)
                .padding(horizontal = GalleryDesign.PaddingViewerLockH)
        ) {
            Box(
                modifier = Modifier
                    .size(GalleryDesign.IconSizeAction)
                    .background(Color.Black.copy(alpha = GalleryDesign.AlphaOverlay), GalleryDesign.CardShape)
                    .premiumBorder(shape = GalleryDesign.CardShape)
                    .clickable { 
                        // Bloqueo instantáneo
                        isLocked = true 
                        uiVisible = false 
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bloquear",
                    tint = Color.White,
                    modifier = Modifier.size(GalleryDesign.IconSizeSmall)
                )
            }
        }

        // --- BOTÓN DE DESBLOQUEO FLOTANTE ---
        AnimatedVisibility(
            visible = showUnlockOverlay,
            enter = fadeIn(tween(GalleryDesign.ViewerAnimNormal)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(tween(GalleryDesign.ViewerAnimNormal)) + scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = GalleryDesign.PaddingViewerLockV)
                .padding(horizontal = GalleryDesign.PaddingViewerLockH)
        ) {
            Box(
                modifier = Modifier
                    .size(GalleryDesign.IconSizeAction)
                    .background(brush = GalleryDesign.primaryGradient(), shape = GalleryDesign.CardShape)
                    .premiumBorder(shape = GalleryDesign.CardShape)
                    .clickable { 
                        isLocked = false 
                        showUnlockOverlay = false
                        uiVisible = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = "Desbloquear",
                    tint = Color.White,
                    modifier = Modifier.size(GalleryDesign.IconSizeSmall)
                )
            }
        }
    }
}

@Composable
fun ZoomableImage(
    item: MediaItem,
    onScaleChange: (Float) -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var imageIntrinsicSize by remember { mutableStateOf(Size.Zero) }
    // Sincronizamos la escala interna con el visor global
    LaunchedEffect(scale) {
        onScaleChange(scale)
    }

    fun calculateBoundOffset(newOffset: Offset, currentScale: Float): Offset {
        if (currentScale <= 1f || viewportSize == IntSize.Zero || imageIntrinsicSize == Size.Zero) return Offset.Zero
        val ratio = min(viewportSize.width.toFloat() / imageIntrinsicSize.width, viewportSize.height.toFloat() / imageIntrinsicSize.height)
        val fittedWidth = imageIntrinsicSize.width * ratio
        val fittedHeight = imageIntrinsicSize.height * ratio
        val maxX = maxOf(0f, (fittedWidth * currentScale - viewportSize.width) / 2f)
        val maxY = maxOf(0f, (fittedHeight * currentScale - viewportSize.height) / 2f)
        return Offset(newOffset.x.coerceIn(-maxX, maxX), newOffset.y.coerceIn(-maxY, maxY))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = GalleryDesign.ViewerScaleMax
                            offset = Offset.Zero 
                        }
                        onScaleChange(scale)
                    }
                )
            }
            .pointerInput(scale) {
                // Modificamos el detector para que sea cooperativo con el Pager
                awaitEachGesture {
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop

                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (!pastTouchSlop) {
                                zoom *= zoomChange
                                pan += panChange
                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                val zoomMotion = kotlin.math.abs(1 - zoom) * centroidSize
                                val panMotion = pan.getDistance()

                                if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                    pastTouchSlop = true
                                }
                            }

                            if (pastTouchSlop) {
                                // Solo consumimos si estamos haciendo zoom o si ya estamos ampliados
                                if (scale > 1.01f || zoomChange > 1.01f) {
                                    val newScale = (scale * zoomChange).coerceIn(1f, GalleryDesign.ViewerScaleLimit)
                                    val candidateOffset = offset + panChange * scale
                                    
                                    scale = newScale
                                    offset = calculateBoundOffset(candidateOffset, scale)
                                    onScaleChange(scale)

                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed })
                    
                    // Al soltar, si estamos cerca de 1, reseteamos para habilitar el pager
                    if (scale <= 1.01f) {
                        scale = 1f
                        offset = Offset.Zero
                        onScaleChange(1f)
                    }
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.url)
                .crossfade(GalleryDesign.ViewerAnimNormal)
                .build(),
            contentDescription = null,
            onState = { if (it is coil.compose.AsyncImagePainter.State.Success) imageIntrinsicSize = it.painter?.intrinsicSize ?: Size.Zero },
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun viewportWidth() = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp

@Composable
fun ViewerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(GalleryDesign.PaddingSmall)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(GalleryDesign.IconSizeNormal))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Modificador para un borde premium rotativo (Efecto "Snake Trace" matemático)
 */
fun Modifier.rotatingPremiumBorder(
    shape: Shape,
    borderWidth: Float = GalleryDesign.ViewerBorderWidth
) = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "borderRotation")
    
    // Definimos el patrón a partir de los tokens
    val dashLength = GalleryDesign.ViewerBorderDash
    val gapLength = GalleryDesign.ViewerBorderGap
    val patternLength = dashLength + gapLength

    // Animamos la fase exactamente por la longitud del patrón para un loop infinito REAL
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = patternLength, 
        animationSpec = infiniteRepeatable(
            animation = tween(GalleryDesign.ViewerAnimBorder, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorSecondary = MaterialTheme.colorScheme.secondary

    this.drawWithContent {
        drawContent() // Dibujamos la mini-imagen
        
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path().apply { addOutline(outline) }
        
        // 1. Borde base tenue para que el marco se vea "completo"
        drawOutline(
            outline = outline,
            color = colorPrimary.copy(alpha = GalleryDesign.ViewerBorderAlphaBase),
            style = Stroke(width = GalleryDesign.BorderWidthThin.toPx())
        )
        
        // 2. Las 3 estelas de luz que recorren el perímetro
        drawPath(
            path = path,
            brush = Brush.sweepGradient(
                colors = listOf(colorPrimary, colorSecondary, colorPrimary),
                center = center
            ),
            style = Stroke(
                width = borderWidth,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(dashLength, gapLength),
                    phase = -phase 
                ),
                cap = StrokeCap.Round
            )
        )
    }
}

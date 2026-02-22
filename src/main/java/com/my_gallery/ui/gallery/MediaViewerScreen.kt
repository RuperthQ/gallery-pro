package com.my_gallery.ui.gallery

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import com.my_gallery.domain.model.MediaItem
import com.my_gallery.ui.components.PremiumMenu
import com.my_gallery.ui.components.PremiumMenuItem
import com.my_gallery.ui.gallery.viewer.components.*
import com.my_gallery.ui.theme.GalleryDesign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla Orquestadora del Visor de Medios.
 * Gestiona el estado global, ciclos de vida de seguridad y delega la UI a componentes modulares.
 */
@Composable
fun MediaViewerScreen(
    item: MediaItem, 
    items: LazyPagingItems<GalleryUiModel>,
    initialIndex: Int,
    viewModel: GalleryViewModel,
    onClose: () -> Unit,
    onShowMetadata: () -> Unit
) {
    // --- ESTADO ---
    var uiVisible by remember { mutableStateOf(true) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var showUnlockOverlay by remember { mutableStateOf(false) }
    var globalScale by remember { mutableFloatStateOf(1f) }
    var showMenu by remember { mutableStateOf(false) }
    var currentVideoIsHorizontal by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val autoplayEnabled by viewModel.autoplayEnabled.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = initialIndex) { items.itemCount }
    val thumbnailListState = rememberLazyListState()
    
    val context = LocalContext.current
    val activity = context as? Activity
    val isVaultItem = item.albumId == "SECURE_VAULT"
    val view = LocalView.current
    val density = LocalDensity.current
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp

    // --- SEGURIDAD Y CICLO DE VIDA ---
    DisposableEffect(isVaultItem) {
        if (isVaultItem) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (isVaultItem) activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { 
            view.keepScreenOn = false 
            viewModel.clearDecryptedCache()
        }
    }

    // Botón de Pánico (Shake to Lock)
    DisposableEffect(isVaultItem, context) {
        if (!isVaultItem) return@DisposableEffect onDispose {}
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShakeTime = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                val acceleration = kotlin.math.sqrt(x*x + y*y + z*z) - SensorManager.GRAVITY_EARTH
                if (acceleration > 12f) {
                    val now = System.currentTimeMillis()
                    if (now - lastShakeTime > 500) {
                        lastShakeTime = now
                        onClose()
                        viewModel.clearDecryptedCache()
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // --- LÓGICA DE INTERFAZ ---
    BackHandler {
        when {
            isLocked -> showUnlockOverlay = true
            showMenu -> showMenu = false
            isFullScreen -> isFullScreen = false
            else -> onClose()
        }
    }

    LaunchedEffect(showUnlockOverlay) {
        if (showUnlockOverlay) {
            delay(GalleryDesign.ViewerAnimLong.toLong())
            showUnlockOverlay = false
        }
    }

    LaunchedEffect(isFullScreen) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (isFullScreen) {
            controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            (view.context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val itemSizePx = with(density) { GalleryDesign.ViewerThumbSize.toPx() }
        val screenWidthPx = with(density) { screenWidth.toPx() }
        val centerOffset = (screenWidthPx / 2) - (itemSizePx / 2)
        thumbnailListState.animateScrollToItem(pagerState.currentPage, -centerOffset.toInt())
    }

    // --- ESTRUCTURA ---
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        
        // 1. PAGER PRINCIPAL (Contenido: Imagen/Video)
        ViewerPager(
            items = items,
            pagerState = pagerState,
            isLocked = isLocked,
            globalScale = globalScale,
            autoplayEnabled = autoplayEnabled,
            isFullScreen = isFullScreen,
            uiVisible = uiVisible,
            viewModel = viewModel,
            onToggleUi = { 
                if (isLocked) showUnlockOverlay = true 
                else uiVisible = !uiVisible 
            },
            onSetUiVisible = { if (!isLocked) uiVisible = it },
            onShowMenu = { if (!isLocked) showMenu = true },
            onScaleChange = { globalScale = it },
            onFullScreenChange = { isFullScreen = it },
            onVideoOrientationDetected = { currentVideoIsHorizontal = it }
        )

        // 2. OVERLAYS DE CONTROL (Barra superior y carrusel inferior)
        AnimatedVisibility(
            visible = uiVisible && !isFullScreen,
            enter = fadeIn(tween(GalleryDesign.ViewerAnimFast)) + scaleIn(initialScale = GalleryDesign.ViewerScaleOverlay, animationSpec = tween(GalleryDesign.ViewerAnimFast)),
            exit = fadeOut(tween(GalleryDesign.ViewerAnimFast)) + scaleOut(targetScale = GalleryDesign.ViewerScaleOverlay, animationSpec = tween(GalleryDesign.ViewerAnimFast))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val currentMedia = (items[pagerState.currentPage] as? GalleryUiModel.Media)?.item
                
                ViewerTopBar(
                    title = currentMedia?.title ?: "",
                    onClose = onClose,
                    onMoreOptions = { showMenu = true }
                )

                if (currentMedia?.mimeType?.startsWith("video/") == false) {
                    ViewerCarousel(
                        items = items,
                        currentPage = pagerState.currentPage,
                        listState = thumbnailListState,
                        onItemSelected = { idx -> scope.launch { pagerState.animateScrollToPage(idx) } },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = GalleryDesign.PaddingLarge)
                    )
                }
            }
        }

        // 3. MENÚ PREMIUM DE OPCIONES
        val currentMedia = (items[pagerState.currentPage] as? GalleryUiModel.Media)?.item
        PremiumMenu(
            visible = showMenu,
            onDismiss = { showMenu = false },
            items = listOf(
                PremiumMenuItem("Compartir", Icons.Default.Share, onClick = { /* TODO */ showMenu = false }),
                PremiumMenuItem("Información", Icons.Default.Info, onClick = { showMenu = false; onShowMetadata() }),
                PremiumMenuItem("Auto-reproducción", Icons.Default.Settings, isSelected = autoplayEnabled, showToggle = true, onClick = { viewModel.toggleAutoplay() }),
                PremiumMenuItem("Rotar", Icons.Default.RotateRight, onClick = { currentMedia?.let { viewModel.rotateMedia(it) }; showMenu = false })
            )
        )

        // 4. CONTROLES DE BLOQUEO (Lock/Unlock)
        val isHorizontal = if (currentMedia?.mimeType?.startsWith("video/") == true) currentVideoIsHorizontal 
                          else currentMedia?.let { it.width >= it.height } ?: true

        ViewerLockControls(
            isLocked = isLocked,
            showUnlockOverlay = showUnlockOverlay,
            uiVisible = uiVisible,
            isFullScreen = isFullScreen,
            isHorizontal = isHorizontal,
            onLock = { isLocked = true; uiVisible = false },
            onUnlock = { isLocked = false; showUnlockOverlay = false; uiVisible = true }
        )

        // 5. INTERCEPTOR DE TOQUES PARA BLOQUEO
        if (isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showUnlockOverlay = true })
                    }
            )
        }
    }
}

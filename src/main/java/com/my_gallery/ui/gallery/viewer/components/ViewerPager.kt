package com.my_gallery.ui.gallery.viewer.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.paging.compose.LazyPagingItems
import com.my_gallery.domain.model.MediaItem
import com.my_gallery.ui.components.VideoPlayer
import com.my_gallery.ui.gallery.GalleryUiModel
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.theme.GalleryDesign
import kotlin.math.abs

@Composable
fun ViewerPager(
    items: LazyPagingItems<GalleryUiModel>,
    pagerState: PagerState,
    initialItem: MediaItem,
    initialIndex: Int,
    isLocked: Boolean,
    globalScale: Float,
    autoplayEnabled: Boolean,
    isFullScreen: Boolean,
    uiVisible: Boolean,
    viewModel: GalleryViewModel,
    onToggleUi: () -> Unit,
    onSetUiVisible: (Boolean) -> Unit,
    onShowMenu: () -> Unit,
    onScaleChange: (Float) -> Unit,
    onFullScreenChange: (Boolean) -> Unit,
    onVideoOrientationDetected: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = !isLocked && (globalScale <= 1.05f) && 
            !(pagerState.currentPage == 0 && pagerState.currentPageOffsetFraction < 0), 
        pageSpacing = GalleryDesign.PaddingLarge
    ) { pageIndex ->
        val uiModel = items[pageIndex]
        
        // --- LOGICA DE FALLBACK PARA EVITAR "CARGANDO" EN EL ITEM INICIAL ---
        val mediaItem = when {
            uiModel is GalleryUiModel.Media -> uiModel.item
            pageIndex == initialIndex -> initialItem
            else -> null
        }

        if (mediaItem != null) {
            val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
            val absOffset = abs(pageOffset).coerceIn(0f, 1f)
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = GalleryDesign.ViewerScalePagerDown + 
                            (1f - GalleryDesign.ViewerScalePagerDown) * (1f - absOffset)
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - absOffset).coerceIn(0.5f, 1f)
                        translationX = pageOffset * size.width * 0.2f
                    }
            ) {
                if (mediaItem.mimeType.startsWith("video/")) {
                    VideoPage(
                        mediaItem = mediaItem,
                        isActive = pagerState.currentPage == pageIndex,
                        isLocked = isLocked,
                        autoplayEnabled = autoplayEnabled,
                        isFullScreen = isFullScreen,
                        uiVisible = uiVisible,
                        viewModel = viewModel,
                        onToggleUi = onToggleUi,
                        onSetUiVisible = onSetUiVisible,
                        onShowMenu = onShowMenu,
                        onFullScreenChange = onFullScreenChange,
                        onVideoOrientationDetected = onVideoOrientationDetected
                    )
                } else {
                    ZoomableMedia(
                        item = mediaItem,
                        rotation = mediaItem.rotation,
                        onScaleChange = onScaleChange,
                        onRotate = { viewModel.rotateMedia(mediaItem) },
                        onTap = onToggleUi,
                        onLongPress = onShowMenu
                    )
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Cargando...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = GalleryDesign.AlphaOverlay))
            }
        }
    }
}

@Composable
private fun VideoPage(
    mediaItem: MediaItem,
    isActive: Boolean,
    isLocked: Boolean,
    autoplayEnabled: Boolean,
    isFullScreen: Boolean,
    uiVisible: Boolean,
    viewModel: GalleryViewModel,
    onToggleUi: () -> Unit,
    onSetUiVisible: (Boolean) -> Unit,
    onShowMenu: () -> Unit,
    onFullScreenChange: (Boolean) -> Unit,
    onVideoOrientationDetected: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleUi() },
                    onLongPress = { if (!isLocked) onShowMenu() }
                )
            }
    ) {
        val videoUrlToPlay by produceState<String?>(initialValue = null, key1 = mediaItem.url, key2 = isActive) {
            if (isActive) {
                if (mediaItem.url.startsWith("vault://")) {
                    value = viewModel.decryptMediaToCache(mediaItem)
                } else {
                    value = mediaItem.url
                }
            } else {
                value = null
            }
        }

        if (videoUrlToPlay == null && mediaItem.url.startsWith("vault://") && isActive) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (videoUrlToPlay != null) {
            VideoPlayer(
                videoUrl = videoUrlToPlay!!,
                videoWidth = mediaItem.width,
                videoHeight = mediaItem.height,
                autoplayEnabled = autoplayEnabled,
                isActive = isActive,
                isFullScreen = isFullScreen,
                showControls = uiVisible,
                onFullScreenChange = onFullScreenChange,
                onControlsVisibilityChange = { onSetUiVisible(it) },
                onVideoOrientationDetected = onVideoOrientationDetected,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

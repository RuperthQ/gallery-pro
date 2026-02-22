package com.my_gallery.ui.gallery.viewer.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

@Composable
fun ViewerLockControls(
    isLocked: Boolean,
    showUnlockOverlay: Boolean,
    uiVisible: Boolean,
    isFullScreen: Boolean,
    isHorizontal: Boolean,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // --- BOTÓN DE BLOQUEO ---
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
                    .clickable { onLock() },
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
                    .clickable { onUnlock() },
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

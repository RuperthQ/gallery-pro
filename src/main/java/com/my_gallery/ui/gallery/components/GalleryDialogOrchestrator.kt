package com.my_gallery.ui.gallery.components

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import com.my_gallery.domain.model.AlbumItem
import com.my_gallery.ui.gallery.GalleryUiModel
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.gallery.MediaViewerScreen
import com.my_gallery.ui.security.SecurityViewModel
import com.my_gallery.ui.settings.SettingsScreen
import com.my_gallery.ui.theme.GalleryDesign

@Composable
fun GalleryDialogOrchestrator(
    viewModel: GalleryViewModel,
    securityViewModel: SecurityViewModel,
    albums: List<AlbumItem>,
    viewerItems: LazyPagingItems<GalleryUiModel>,
    onShowMetadata: (com.my_gallery.domain.model.MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val showCreateAlbumDialog by viewModel.showCreateAlbumDialog.collectAsStateWithLifecycle()
    val showMoveToAlbumDialog by viewModel.showMoveToAlbumDialog.collectAsStateWithLifecycle()
    val showDeleteConfirmation by viewModel.showDeleteConfirmation.collectAsStateWithLifecycle()
    val isMovingMedia by viewModel.isMovingMedia.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettings.collectAsStateWithLifecycle()
    val viewerItem by viewModel.viewerItem.collectAsStateWithLifecycle()
    val viewerIndex by viewModel.viewerIndex.collectAsStateWithLifecycle()
    val selectedMediaIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()

    // --- DIÁLOGOS ---
    if (showCreateAlbumDialog) {
        CreateAlbumDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideCreateAlbumDialog() }
        )
    }

    if (showMoveToAlbumDialog) {
        MoveToAlbumDialog(
            albums = albums,
            viewModel = viewModel,
            onDismiss = { viewModel.hideMoveToAlbumDialog() }
        )
    }

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

    // --- VISOR DE MEDIOS ---
    AnimatedVisibility(
        visible = viewerItem != null,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(GalleryDesign.ViewerAnimSlow)) + 
                scaleIn(initialScale = GalleryDesign.ViewerScaleTransition, animationSpec = androidx.compose.animation.core.tween(GalleryDesign.ViewerAnimSlow)),
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(GalleryDesign.ViewerAnimSlow)) + 
               scaleOut(targetScale = GalleryDesign.ViewerScaleTransition, animationSpec = androidx.compose.animation.core.tween(GalleryDesign.ViewerAnimSlow))
    ) {
        viewerItem?.let { item ->
            MediaViewerScreen(
                item = item,
                items = viewerItems,
                initialIndex = viewerIndex,
                viewModel = viewModel,
                onClose = { viewModel.closeViewer() },
                onShowMetadata = { onShowMetadata(item) }
            )
        }
    }

    // --- PANTALLA DE AJUSTES ---
    AnimatedVisibility(
        visible = showSettings,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        SettingsScreen(
            galleryViewModel = viewModel,
            securityViewModel = securityViewModel,
            onBack = { viewModel.hideSettings() }
        )
    }
}

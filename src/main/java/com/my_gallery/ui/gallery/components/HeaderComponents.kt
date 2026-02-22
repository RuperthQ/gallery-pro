package com.my_gallery.ui.gallery.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.gallery.header_actions.HeaderAction
import com.my_gallery.ui.gallery.header_actions.HeaderActionsOrchestrator
import com.my_gallery.ui.gallery.filters.FilterOrchestrator
import com.my_gallery.ui.gallery.filters.GalleryFilter
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder
import com.my_gallery.ui.theme.GalleryDesign.glassBackground

import com.my_gallery.ui.security.SecurityViewModel
import com.my_gallery.ui.security.BiometricPromptManager
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen

@Composable
fun HeaderLayout(
    showFilters: Boolean,
    viewModel: GalleryViewModel,
    securityViewModel: SecurityViewModel,
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
                .padding(horizontal = GalleryDesign.PaddingLarge, vertical = GalleryDesign.PaddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val context = LocalContext.current
            val isAppLocked by securityViewModel.isAppLocked.collectAsStateWithLifecycle(initialValue = false)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Título eliminado a petición del usuario
                
                IconButton(onClick = {
                    if (isAppLocked) {
                        val biom = (context as? FragmentActivity)?.let { BiometricPromptManager(it) }
                        if (biom?.canAuthenticate() == true) {
                            biom.authenticate(
                                title = "Desactivar bloqueo",
                                subtitle = "Autorización requerida",
                                onSuccess = { securityViewModel.toggleAppLock(false) },
                                onError = { /* opcional alert */ }
                            )
                        } else {
                            securityViewModel.toggleAppLock(false)
                        }
                    } else {
                        securityViewModel.toggleAppLock(true)
                    }
                }) {
                    Icon(
                        imageVector = if (isAppLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Bloquear App",
                        tint = if (isAppLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            HeaderActionsRow(viewModel, showFilters)
        }

        AnimatedVisibility(
            visible = showFilters,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FilterRow(viewModel)
        }
    }
}

@Composable
fun HeaderActionsRow(viewModel: GalleryViewModel, showFilters: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
        val selectedItems by viewModel.selectedMediaIds.collectAsStateWithLifecycle()

        if (isSelectionMode) {
            SelectionModeActions(viewModel, selectedItems.size)
        } else {
            NormalModeActions(viewModel, showFilters)
        }
    }
}

@Composable
fun SelectionModeActions(viewModel: GalleryViewModel, selectedCount: Int) {
    val isAlbumCreationPending by viewModel.isAlbumCreationPending.collectAsStateWithLifecycle()
    val orchestrator = remember(viewModel) { HeaderActionsOrchestrator(viewModel) }
    
    // Counter
    Text(
        text = "$selectedCount sel.",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(end = GalleryDesign.PaddingSmall)
    )

    // Main Actions
    Row(horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall)) {
        val areAllSecured = viewModel.areAllSelectedSecured()
        val actions = orchestrator.getSelectionActions(isAlbumCreationPending, selectedCount, areAllSecured)
        actions.forEach { action ->
            val tint = if (action.description == "Eliminar") MaterialTheme.colorScheme.error 
                      else if (action.description == "Mover") MaterialTheme.colorScheme.tertiary
                      else if (action.description == "Asegurar" || action.description == "Desbloquear") MaterialTheme.colorScheme.secondary
                      else MaterialTheme.colorScheme.primary
            
            val bg = tint.copy(alpha = 0.2f)
            
            HeaderActionButton(
                action = action,
                tint = tint,
                backgroundColor = bg
            )
        }
    }

    Spacer(modifier = Modifier.width(GalleryDesign.PaddingSmall))

    // Cancel Action
    HeaderActionButton(
        action = orchestrator.getCancelAction(),
        tint = MaterialTheme.colorScheme.error,
        backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
    )
}

@Composable
fun NormalModeActions(viewModel: GalleryViewModel, showFilters: Boolean) {
    val orchestrator = remember(viewModel) { HeaderActionsOrchestrator(viewModel) }
    val showEmptyAlbums by viewModel.showEmptyAlbums.collectAsStateWithLifecycle()
    val actions = orchestrator.getNormalActions(showFilters, showEmptyAlbums)

    actions.forEachIndexed { index, action ->
        if (index > 0) Spacer(modifier = Modifier.width(GalleryDesign.PaddingSmall))
        HeaderActionButton(
            action = action,
            tint = if (action.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            backgroundColor = if (action.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun HeaderActionButton(
    action: HeaderAction,
    tint: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
) {
    Box(
        modifier = Modifier
            .size(GalleryDesign.IconSizeAction)
            .clip(GalleryDesign.CardShape)
            .background(backgroundColor)
            .clickable(onClick = action.onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.description,
            tint = tint,
            modifier = Modifier.size(GalleryDesign.IconSizeNormal)
        )
    }
}
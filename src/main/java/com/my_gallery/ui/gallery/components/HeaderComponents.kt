package com.my_gallery.ui.gallery.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall)
            ) {
                // Botón de Bloqueo con el mismo estilo que los demás
                HeaderActionButton(
                    action = HeaderAction(
                        icon = if (isAppLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        description = "Bloquear App",
                        onClick = {
                            if (isAppLocked) {
                                val biom = (context as? FragmentActivity)?.let { BiometricPromptManager(it) }
                                if (biom?.canAuthenticate() == true) {
                                    biom.authenticate(
                                        title = "Desactivar bloqueo",
                                        subtitle = "Autorización requerida",
                                        onSuccess = { securityViewModel.toggleAppLock(false) },
                                        onError = { }
                                    )
                                } else {
                                    securityViewModel.toggleAppLock(false)
                                }
                            } else {
                                securityViewModel.toggleAppLock(true)
                            }
                        }
                    ),
                    tint = if (isAppLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    backgroundColor = if (isAppLocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = GalleryDesign.PaddingTiny),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            HeaderActionsRow(viewModel, showFilters)
        }
        ActiveFilterPills(viewModel = viewModel)

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
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val selectedItems by viewModel.selectedMediaIds.collectAsStateWithLifecycle()

        if (uiState.isSelectionMode) {
            SelectionModeActions(viewModel, selectedItems.size)
        } else {
            NormalModeActions(viewModel, showFilters)
        }
    }
}

@Composable
fun SelectionModeActions(viewModel: GalleryViewModel, selectedCount: Int) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
        val actions = orchestrator.getSelectionActions(uiState.isAlbumCreationPending, selectedCount, areAllSecured)
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val tmCreateVisible by viewModel.topMenuCreateVisible.collectAsStateWithLifecycle()
    val tmGridVisible by viewModel.topMenuGridVisible.collectAsStateWithLifecycle()
    val tmEmptyVisible by viewModel.topMenuEmptyVisible.collectAsStateWithLifecycle()
    val tmSelectVisible by viewModel.topMenuSelectVisible.collectAsStateWithLifecycle()

    val allActions = orchestrator.getNormalActions(showFilters, showEmptyAlbums)

    val settingsAction = allActions.last()
    val otherActions = allActions.dropLast(1)

    val visibleActions = otherActions.filter { action ->
        when (action.description) {
            "Nuevo Álbum" -> tmCreateVisible
            "Cambiar Columnas" -> tmGridVisible
            "Álbumes Vacíos" -> tmEmptyVisible
            "Seleccionar" -> tmSelectVisible
            else -> true
        }
    }
    
    val hiddenActions = otherActions.filter { it !in visibleActions }
    var showSubMenu by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall)
    ) {
        visibleActions.forEach { action ->
            val isSelected = action.isSelected
            HeaderActionButton(
                action = action,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        }

        // Divisor antes del grupo de "Herramientas" (Más + Ajustes)
        VerticalDivider(
            modifier = Modifier
                .height(20.dp)
                .padding(horizontal = GalleryDesign.PaddingTiny),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )

        // Botón para expandir submenú si hay acciones ocultas, ahora a la izquierda de Ajustes
        if (hiddenActions.isNotEmpty()) {
            Box {
                HeaderActionButton(
                    action = HeaderAction(
                        icon = if (showSubMenu) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        description = "Más opciones",
                        onClick = { showSubMenu = !showSubMenu },
                        isSelected = showSubMenu
                    ),
                    tint = if (showSubMenu) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    backgroundColor = if (showSubMenu) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                
                DropdownMenu(
                    expanded = showSubMenu,
                    onDismissRequest = { showSubMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    hiddenActions.forEach { action ->
                        val isSelected = action.isSelected
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = action.description,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface 
                                )
                            },
                            onClick = { 
                                action.onClick()
                                showSubMenu = false
                            },
                            leadingIcon = { 
                                Icon(
                                    imageVector = action.icon, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(GalleryDesign.IconSizeSmall),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )
                    }
                }
            }
        }

        // Botón de Ajustes (Siempre visible)
        HeaderActionButton(
            action = settingsAction,
            tint = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
package com.my_gallery.ui.gallery.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.gallery.header_actions.ChangeGridAction
import com.my_gallery.ui.gallery.header_actions.CreateAlbumAction
import com.my_gallery.ui.gallery.header_actions.HeaderAction
import com.my_gallery.ui.gallery.header_actions.HeaderActionsOrchestrator
import com.my_gallery.ui.gallery.header_actions.RefreshGalleryAction
import com.my_gallery.ui.gallery.header_actions.SettingsAction
import com.my_gallery.ui.gallery.header_actions.ToggleEmptyAlbumsAction
import com.my_gallery.ui.gallery.header_actions.ToggleFilterAction
import com.my_gallery.ui.gallery.header_actions.ToggleSelectionAction
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.glassBackground
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

@Composable
fun FloatingGalleryMenu(
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedMediaIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val showFilters by viewModel.showFilters.collectAsStateWithLifecycle()
    val showEmptyAlbums by viewModel.showEmptyAlbums.collectAsStateWithLifecycle()
    val isAlbumCreationPending by viewModel.isAlbumCreationPending.collectAsStateWithLifecycle()
    val isForceSyncing by viewModel.isForceSyncing.collectAsStateWithLifecycle()
    
    val orchestrator = remember(viewModel) { HeaderActionsOrchestrator(viewModel) }
    var extraExpanded by remember { mutableStateOf(false) }
    val showSettings by viewModel.showSettings.collectAsStateWithLifecycle()

    // Autocerrar el submenú cuando se entra a los ajustes, a selección, o se abren los filtros
    LaunchedEffect(showSettings, isSelectionMode, showFilters) {
        if (showSettings || isSelectionMode || showFilters) {
            extraExpanded = false
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GalleryDesign.PaddingLarge, vertical = GalleryDesign.PaddingMedium),
        contentAlignment = Alignment.BottomCenter
    ) {
        // --- SUBMENÚ FLOTANTE (MODAL STYLE) ---
        // Se coloca fuera del Surface para que no expanda el contenedor principal
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(bottom = 70.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            AnimatedVisibility(
                visible = extraExpanded && !isSelectionMode,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom) + scaleIn(transformOrigin = TransformOrigin(1f, 1f)),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom) + scaleOut(transformOrigin = TransformOrigin(1f, 1f))
            ) {
                Surface(
                    modifier = Modifier
                        .width(220.dp)
                        .clip(GalleryDesign.CardShape)
                        .glassBackground()
                        .premiumBorder(shape = GalleryDesign.CardShape),
                    shape = GalleryDesign.CardShape,
                    color = Color.Transparent,
                    tonalElevation = GalleryDesign.ElevationSmall
                ) {
                    Column(
                        modifier = Modifier.padding(GalleryDesign.PaddingSmall),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FloatingMenuLabeledRow(
                            action = RefreshGalleryAction(viewModel)(),
                            isSyncing = isForceSyncing
                        )
                        FloatingMenuLabeledRow(ToggleEmptyAlbumsAction(viewModel, showEmptyAlbums)())
                        FloatingMenuLabeledRow(ChangeGridAction(viewModel)())
                        FloatingMenuLabeledRow(SettingsAction(viewModel)())
                    }
                }
            }
        }

        // --- FILTROS FLOTANTES (MODAL STYLE) ---
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(bottom = 70.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = showFilters && !isSelectionMode,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom) + scaleIn(transformOrigin = TransformOrigin(0.5f, 1f)),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom) + scaleOut(transformOrigin = TransformOrigin(0.5f, 1f))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GalleryDesign.FilterShape)
                        .glassBackground()
                        .premiumBorder(shape = GalleryDesign.FilterShape),
                    shape = GalleryDesign.FilterShape,
                    color = Color.Transparent,
                    tonalElevation = GalleryDesign.ElevationSmall
                ) {
                    Column {
                        FilterRow(viewModel = viewModel)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(GalleryDesign.FilterShape)
                .glassBackground()
                .premiumBorder(shape = GalleryDesign.FilterShape),
            shape = GalleryDesign.FilterShape,
            color = Color.Transparent,
            tonalElevation = GalleryDesign.ElevationSmall
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GalleryDesign.PaddingMedium, vertical = GalleryDesign.PaddingSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // --- SLOT IZQUIERDO (Bloqueo) ---
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (!isSelectionMode) {
                        val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle(initialValue = false)
                        val context = androidx.compose.ui.platform.LocalContext.current
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (isAppLocked) {
                                        val biom = (context as? androidx.fragment.app.FragmentActivity)?.let { com.my_gallery.ui.security.BiometricPromptManager(it) }
                                        if (biom?.canAuthenticate() == true) {
                                            biom.authenticate(
                                                title = "Desactivar bloqueo",
                                                subtitle = "Autorización requerida",
                                                onSuccess = { viewModel.toggleAppLock(false) },
                                                onError = { }
                                            )
                                        } else {
                                            viewModel.toggleAppLock(false)
                                        }
                                    } else {
                                        viewModel.toggleAppLock(true)
                                    }
                                },
                                modifier = Modifier
                                    .size(GalleryDesign.IconSizeAction)
                                    .clip(GalleryDesign.CardShape)
                                    .background(if (isAppLocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = if (isAppLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Bloquear App",
                                    tint = if (isAppLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(GalleryDesign.IconSizeNormal)
                                )
                            }
                            
                            VerticalDivider(
                                modifier = Modifier
                                    .height(20.dp)
                                    .padding(horizontal = GalleryDesign.PaddingTiny),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // --- SLOT CENTRAL (Acciones Principales) ---
                Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelectionMode) {
                            val areAllSecured = viewModel.areAllSelectedSecured()
                            val actions = orchestrator.getSelectionActions(isAlbumCreationPending, selectedMediaIds.size, areAllSecured)
                            
                            Text(
                                text = "${selectedMediaIds.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = GalleryDesign.PaddingSmall)
                            )
                            
                            actions.forEach { action ->
                                FloatingMenuButton(action)
                            }
                            
                            FloatingMenuButton(
                                action = orchestrator.getCancelAction(),
                                tint = MaterialTheme.colorScheme.error
                            )
                        } else {
                            FloatingMenuButton(CreateAlbumAction(viewModel)())
                            FloatingMenuButton(ToggleFilterAction(viewModel, showFilters)())
                            FloatingMenuButton(ToggleSelectionAction(viewModel)())
                        }
                    }
                }

                // --- SLOT DERECHO (Ajustes Rápidos) ---
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    if (!isSelectionMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VerticalDivider(
                                modifier = Modifier
                                    .height(20.dp)
                                    .padding(horizontal = GalleryDesign.PaddingTiny),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            
                            IconButton(
                                onClick = { 
                                    extraExpanded = !extraExpanded
                                    if (extraExpanded && showFilters) {
                                        // Ocultar filtros si se abre el submenú de ajustes rápido
                                        viewModel.toggleFilters()
                                    }
                                },
                                modifier = Modifier
                                    .size(GalleryDesign.IconSizeAction)
                                    .clip(GalleryDesign.CardShape)
                                    .background(if (extraExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = if (extraExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = "Ajustes rápidos",
                                    tint = if (extraExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(GalleryDesign.IconSizeNormal)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingMenuLabeledRow(
    action: HeaderAction,
    isSyncing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refreshAnim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Surface(
        onClick = { if (!isSyncing) action.onClick() },
        color = Color.Transparent,
        shape = GalleryDesign.CardShape,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isSyncing
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = GalleryDesign.PaddingMedium, vertical = GalleryDesign.PaddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(GalleryDesign.CardShape)
                    .background(if (action.isSelected || isSyncing) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = if (action.isSelected || isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(GalleryDesign.IconSizeSmall)
                        .then(if (isSyncing) Modifier.graphicsLayer { rotationZ = rotation } else Modifier)
                )
            }
            
            Spacer(modifier = Modifier.width(GalleryDesign.PaddingMedium))
            
            Text(
                text = if (isSyncing) "Refrescando..." else action.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (action.isSelected || isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (action.isSelected || isSyncing) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun FloatingMenuButton(
    action: HeaderAction,
    tint: Color? = null
) {
    val finalTint = tint ?: if (action.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val bgColor = if (action.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent

    IconButton(
        onClick = action.onClick,
        modifier = Modifier
            .size(GalleryDesign.IconSizeAction)
            .clip(GalleryDesign.CardShape)
            .background(bgColor)
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.description,
            tint = finalTint,
            modifier = Modifier.size(GalleryDesign.IconSizeNormal)
        )
    }
}

package com.my_gallery.ui.recovery

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.my_gallery.domain.model.TrashedMediaItem
import com.my_gallery.ui.components.PremiumAlertDialog
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.glassBackground
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

/**
 * Pantalla Premium de Papelera: muestra archivos eliminados recuperables
 * (solo disponible en Android 11+).
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RecoveryScreen(
    onClose: () -> Unit,
    viewModel: RecoveryViewModel = hiltViewModel()
) {
    val trashedMedia by viewModel.trashedMedia.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val message by viewModel.lastActionMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // --- ESTADOS DE DIÁLOGOS ---
    var showEmptyDialog by remember { mutableStateOf(false) }
    var showRestoreSelectedDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var pendingRestoreItem by remember { mutableStateOf<TrashedMediaItem?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<TrashedMediaItem?>(null) }

    // Vaciar papelera
    if (showEmptyDialog) {
        PremiumAlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            title = "Vaciar papelera",
            titleColor = MaterialTheme.colorScheme.error,
            text = { Text("Se eliminarán definitivamente ${trashedMedia.size} archivos. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { viewModel.emptyTrash(); showEmptyDialog = false }) {
                    Text("Vaciar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showEmptyDialog = false }) { Text("Cancelar") } }
        )
    }

    // Restaurar seleccionados
    if (showRestoreSelectedDialog) {
        PremiumAlertDialog(
            onDismissRequest = { showRestoreSelectedDialog = false },
            title = "Restaurar archivos",
            titleColor = MaterialTheme.colorScheme.primary,
            text = { Text("Se restaurarán ${selectedIds.size} archivos a su ubicación original.") },
            confirmButton = {
                TextButton(onClick = { viewModel.restoreSelected(); showRestoreSelectedDialog = false }) {
                    Text("Restaurar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showRestoreSelectedDialog = false }) { Text("Cancelar") } }
        )
    }

    // Eliminar seleccionados
    if (showDeleteSelectedDialog) {
        PremiumAlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = "Eliminar archivos",
            titleColor = MaterialTheme.colorScheme.error,
            text = { Text("Se eliminarán permanentemente ${selectedIds.size} archivos. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSelected(); showDeleteSelectedDialog = false }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteSelectedDialog = false }) { Text("Cancelar") } }
        )
    }

    // Restaurar archivo individual
    pendingRestoreItem?.let { item ->
        PremiumAlertDialog(
            onDismissRequest = { pendingRestoreItem = null },
            title = "Restaurar archivo",
            titleColor = MaterialTheme.colorScheme.primary,
            text = { Text("\"${item.name}\" volverá a su álbum original.") },
            confirmButton = {
                TextButton(onClick = { viewModel.restoreItem(item); pendingRestoreItem = null }) {
                    Text("Restaurar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { pendingRestoreItem = null }) { Text("Cancelar") } }
        )
    }

    // Eliminar archivo individual
    pendingDeleteItem?.let { item ->
        PremiumAlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            title = "Eliminar permanentemente",
            titleColor = MaterialTheme.colorScheme.error,
            text = { Text("\"${item.name}\" se eliminará definitivamente. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteItem(item); pendingDeleteItem = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteItem = null }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {

                // --- TOPBAR ---
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .premiumBorder(shape = GalleryDesign.FilterShape),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shape = GalleryDesign.FilterShape
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = GalleryDesign.PaddingMedium, vertical = GalleryDesign.PaddingSmall)
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cerrar")
                        }
                        Spacer(Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Papelera",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (trashedMedia.isNotEmpty()) {
                                Text(
                                    "${trashedMedia.size} archivos recuperables",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        // Acciones de selección múltiple
                        if (selectedIds.isNotEmpty()) {
                            IconButton(onClick = { showRestoreSelectedDialog = true }) {
                                Icon(Icons.Default.Restore, "Restaurar seleccionados", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { showDeleteSelectedDialog = true }) {
                                Icon(Icons.Default.Delete, "Eliminar seleccionados", tint = MaterialTheme.colorScheme.error)
                            }
                        } else if (trashedMedia.isNotEmpty()) {
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(Icons.Default.SelectAll, "Seleccionar todos")
                            }
                            IconButton(onClick = { showEmptyDialog = true }) {
                                Icon(Icons.Default.DeleteForever, "Vaciar papelera", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                if (!viewModel.isSupported) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            Spacer(Modifier.height(16.dp))
                            Text("Requiere Android 11 o superior", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                } else if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (trashedMedia.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            Spacer(Modifier.height(12.dp))
                            Text("La papelera está vacía", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(Modifier.height(4.dp))
                            Text("Los archivos eliminados aparecen aquí durante 30 días", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(GalleryDesign.PaddingSmall),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(trashedMedia, key = { it.id }) { item ->
                            TrashMediaCard(
                                item = item,
                                isSelected = item.id in selectedIds,
                                onTap = {
                                    if (selectedIds.isNotEmpty()) viewModel.toggleSelection(item.id)
                                },
                                onLongPress = { viewModel.toggleSelection(item.id) },
                                onRestore = { pendingRestoreItem = item },
                                onDelete = { pendingDeleteItem = item }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TrashMediaCard(
    item: TrashedMediaItem,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .combinedClickable(
                onClick = {
                    if (isSelected || showActions) {
                        showActions = false
                        onTap()
                    } else {
                        showActions = !showActions
                    }
                },
                onLongClick = {
                    showActions = false
                    onLongPress()
                }
            )
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Chip de días hasta expirar
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(bottomEnd = 6.dp),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text(
                "${item.daysUntilExpiry}d",
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    item.daysUntilExpiry <= 3 -> MaterialTheme.colorScheme.error
                    item.daysUntilExpiry <= 7 -> Color(0xFFFFA726)
                    else -> Color.White
                },
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }

        // Indicador de selección
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            )
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
            )
        }

        // Mini-acciones contextuales al tap
        AnimatedVisibility(
            visible = showActions && !isSelected,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.78f)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { showActions = false; onRestore() }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Restore, "Restaurar", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { showActions = false; onDelete() }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.DeleteForever, "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

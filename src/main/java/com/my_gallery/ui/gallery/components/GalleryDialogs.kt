package com.my_gallery.ui.gallery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.my_gallery.domain.model.AlbumItem
import com.my_gallery.ui.components.PremiumAlertDialog
import com.my_gallery.ui.components.PremiumLoadingOverlay
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.theme.GalleryDesign

@Composable
fun CreateAlbumDialog(
    viewModel: GalleryViewModel,
    onDismiss: () -> Unit
) {
    var albumName by remember { mutableStateOf("") }
    
    PremiumAlertDialog(
        onDismissRequest = onDismiss,
        title = "Nuevo Álbum Premium",
        text = {
            Column {
                Text("Ingresa el nombre del álbum que se creará en DCIM/Gallery_Pro", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(GalleryDesign.PaddingSmall))
                OutlinedTextField(
                    value = albumName,
                    onValueChange = { albumName = it },
                    placeholder = { Text("Ej: Vacaciones 2026") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = GalleryDesign.CardShape
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (albumName.isNotBlank()) viewModel.startAlbumCreation(albumName) },
                shape = GalleryDesign.CardShape
            ) {
                Text("Crear y Seleccionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun MoveToAlbumDialog(
    albums: List<AlbumItem>,
    viewModel: GalleryViewModel,
    onDismiss: () -> Unit
) {
    PremiumAlertDialog(
        onDismissRequest = onDismiss,
        title = "Mover a Álbum",
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .fillMaxWidth()
            ) {
                Text("Selecciona el álbum destino:", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(GalleryDesign.PaddingSmall))
                
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GalleryDesign.CardShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .clickable { 
                                    viewModel.hideMoveToAlbumDialog()
                                    viewModel.showCreateAlbumDialog() 
                                }
                                .padding(GalleryDesign.PaddingMedium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(GalleryDesign.PaddingSmall))
                            Text("Crear Nuevo Álbum", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    items(albums.size) { index ->
                        val album = albums[index]
                        if (album.id != "ALL_VIRTUAL_ALBUM" && !album.id.startsWith("TEMP")) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(GalleryDesign.CardShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .clickable { viewModel.moveSelectedToExistingAlbum(album.name) }
                                    .padding(GalleryDesign.PaddingMedium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(album.name, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.weight(1f))
                                Text("${album.count}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PremiumAlertDialog(
        onDismissRequest = onDismiss,
        title = "Eliminar Elementos",
        text = {
            Text(
                "¿Estás seguro de que deseas eliminar $count elementos de forma permanente? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = GalleryDesign.CardShape
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun LoadingOverlay(message: String) {
    PremiumLoadingOverlay(
        message = message
    )
}

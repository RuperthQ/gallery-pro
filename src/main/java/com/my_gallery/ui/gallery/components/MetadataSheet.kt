package com.my_gallery.ui.gallery.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.my_gallery.domain.model.MediaItem
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.theme.GalleryDesign
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MetadataSheetContent(item: MediaItem, viewModel: GalleryViewModel) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(GalleryDesign.PaddingLarge)
            .navigationBarsPadding()
    ) {
        // Fila de Metadata Unificada (Ahora actúa como Header Pro)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = GalleryDesign.PaddingMedium),
            horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall)
        ) {
            MetadataColumnItem(
                icon = Icons.Default.SdStorage,
                label = "Tamaño",
                value = formatFileSize(item.size),
                modifier = Modifier.weight(1f)
            )
            MetadataColumnItem(
                icon = Icons.Default.Straighten,
                label = "Resolución",
                value = if (item.width > 0) "${item.width}x${item.height}" else "---",
                modifier = Modifier.weight(1f)
            )
            MetadataColumnItem(
                icon = Icons.Default.Info,
                label = "Tipo",
                value = item.mimeType.split("/").last().uppercase(),
                modifier = Modifier.weight(1f)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall)) {
            MetadataEditItem(
                icon = Icons.Default.Title,
                label = "Nombre del archivo",
                initialValue = item.title,
                onSave = { newName -> viewModel.renameMedia(item, newName) }
            )
            MetadataItem(
                icon = Icons.Default.CalendarMonth,
                label = "Fecha de creación",
                value = dateFormatter.format(Date(item.dateAdded))
            )
            item.path?.let { path ->
                val displayPath = path.replace("/storage/emulated/0/", "")
                    .replace("/", " > ")
                MetadataItem(
                    icon = Icons.Default.Folder,
                    label = "Ruta",
                    value = displayPath
                )
            }
        }
        
        Spacer(modifier = Modifier.height(GalleryDesign.PaddingLarge))
    }
}

@Composable
fun MetadataEditItem(
    icon: ImageVector,
    label: String,
    initialValue: String,
    onSave: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    
    val lastDotIndex = initialValue.lastIndexOf('.')
    val namePart = if (lastDotIndex != -1) initialValue.substring(0, lastDotIndex) else initialValue
    val extPart = if (lastDotIndex != -1) initialValue.substring(lastDotIndex) else ""
    
    var textValue by remember(initialValue) { mutableStateOf(namePart) }
    val hasChanged = textValue != namePart

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GalleryDesign.CardShape)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            .padding(GalleryDesign.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(GalleryDesign.IconSizeNormal)
        )
        Spacer(modifier = Modifier.width(GalleryDesign.PaddingMedium))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEditing) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f, fill = false),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = extPart,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    Text(
                        text = initialValue,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        IconButton(
            onClick = {
                if (isEditing) {
                    if (hasChanged && textValue.isNotBlank()) {
                        onSave(textValue)
                    }
                    isEditing = false
                } else {
                    isEditing = true
                }
            }
        ) {
            Icon(
                imageVector = if (isEditing) {
                    if (hasChanged) Icons.Default.Check else Icons.Default.Close
                } else {
                    Icons.Default.Edit
                },
                contentDescription = if (isEditing) "Guardar" else "Editar",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(GalleryDesign.IconSizeSmall)
            )
        }
    }
}

@Composable
fun MetadataItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GalleryDesign.CardShape)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            .padding(GalleryDesign.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(GalleryDesign.IconSizeNormal)
        )
        Spacer(modifier = Modifier.width(GalleryDesign.PaddingMedium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
fun MetadataColumnItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(GalleryDesign.CardShape)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
            .padding(GalleryDesign.PaddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(GalleryDesign.IconSizeSmall)
        )
        Spacer(modifier = Modifier.height(GalleryDesign.PaddingTiny))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    val formattedValue = java.text.DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble()))
    return "$formattedValue ${units[digitGroups]}"
}

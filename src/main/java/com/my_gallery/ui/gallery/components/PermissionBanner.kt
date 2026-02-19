package com.my_gallery.ui.gallery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

@Composable
fun PermissionBanner(onGrantClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(GalleryDesign.PaddingMedium)
            .premiumBorder(shape = GalleryDesign.CardShape),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        shape = GalleryDesign.CardShape
    ) {
        Row(
            modifier = Modifier.padding(GalleryDesign.PaddingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(GalleryDesign.IconSizeAction)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SdStorage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(GalleryDesign.PaddingLarge))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Permiso de Edición Pro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Para renombrar y organizar tu galería sin interrupciones, activa el acceso avanzado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(GalleryDesign.PaddingMedium))
            Box(
                modifier = Modifier
                    .height(GalleryDesign.ButtonHeight)
                    .clip(GalleryDesign.FilterShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onGrantClick)
                    .padding(horizontal = GalleryDesign.PaddingLarge),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Activar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

package com.my_gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

@Composable
fun PremiumAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
        },
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        shape = GalleryDesign.HeaderShape,
        containerColor = containerColor,
        modifier = Modifier.premiumBorder(shape = GalleryDesign.HeaderShape)
    )
}

@Composable
fun PremiumLoadingOverlay(
    message: String = "Cargando...",
    subMessage: String? = "Esto puede tardar unos segundos"
) {
     Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(GalleryDesign.PaddingMedium))
            Text(message, color = Color.White, fontWeight = FontWeight.Bold)
            if (subMessage != null) {
                Text(subMessage, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

package com.my_gallery.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.glassBackground
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

@Composable
fun PremiumMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    items: List<PremiumMenuItem>
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Overlay para cerrar al tocar fuera
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = GalleryDesign.AlphaOverlay))
                    .clickable { onDismiss() }
            )
        }

        // El Menú Estilo BottomSheet Pro
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(GalleryDesign.ViewerAnimNormal)),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(GalleryDesign.ViewerAnimNormal)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val bottomSheetShape = GalleryDesign.BottomSheetShape
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // Respeta la barra de sistema de abajo
                    .clip(bottomSheetShape)
                    .glassBackground()
                    .premiumBorder(shape = bottomSheetShape)
                    .padding(vertical = GalleryDesign.PaddingMedium)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingTiny)) {
                    // Indicador de arrastre (Fino y Elegante)
                    Box(
                        modifier = Modifier
                            .size(width = GalleryDesign.DragIndicatorWidth, height = GalleryDesign.DragIndicatorHeight)
                            .clip(RoundedCornerShape(GalleryDesign.PaddingTiny / 2))
                            .background(Color.White.copy(alpha = GalleryDesign.AlphaDisable))
                            .align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(GalleryDesign.PaddingTiny))

                    items.forEach { item ->
                        PremiumMenuRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumMenuRow(item: PremiumMenuItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(GalleryDesign.MenuItemHeight) // Más compacto
            .clip(GalleryDesign.CardShape)
            .clickable { item.onClick() }
            .padding(horizontal = GalleryDesign.PaddingLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (item.isSelected) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.size(GalleryDesign.IconSizeNormal)
        )
        Spacer(modifier = Modifier.width(GalleryDesign.PaddingMedium))
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (item.isSelected) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.weight(1f)
        )
        if (item.showToggle) {
            Switch(
                checked = item.isSelected,
                onCheckedChange = { item.onClick() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

data class PremiumMenuItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isSelected: Boolean = false,
    val showToggle: Boolean = false
)

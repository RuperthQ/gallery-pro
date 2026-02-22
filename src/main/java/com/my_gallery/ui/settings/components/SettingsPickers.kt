package com.my_gallery.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.my_gallery.ui.gallery.MenuStyle
import com.my_gallery.ui.theme.AppThemeColor
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

@Composable
fun SegmentedMenuStyleSelector(
    currentStyle: MenuStyle,
    onStyleSelected: (MenuStyle) -> Unit
) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .premiumBorder(shape = GalleryDesign.FilterShape)
            .clip(GalleryDesign.FilterShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuStyleButton(
            label = "Superior",
            selected = currentStyle == MenuStyle.TOP_HEADER,
            onClick = { onStyleSelected(MenuStyle.TOP_HEADER) }
        )
        MenuStyleButton(
            label = "Flotante",
            selected = currentStyle == MenuStyle.BOTTOM_FLOATING,
            onClick = { onStyleSelected(MenuStyle.BOTTOM_FLOATING) }
        )
    }
}

@Composable
private fun MenuStyleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clip(GalleryDesign.FilterShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ThemeColorSelector(
    currentTheme: AppThemeColor,
    onThemeSelected: (AppThemeColor) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = GalleryDesign.PaddingSmall, start = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingMedium)
    ) {
        items(AppThemeColor.entries.toTypedArray()) { colorOpt ->
            ThemeColorCircle(
                colorOption = colorOpt,
                isSelected = currentTheme == colorOpt,
                onClick = { onThemeSelected(colorOpt) }
            )
        }
    }
}

@Composable
private fun ThemeColorCircle(
    colorOption: AppThemeColor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val displayColor = colorOption.colorValue ?: MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(displayColor)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) borderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (colorOption == AppThemeColor.SYSTEM) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                modifier = Modifier.size(20.dp)
            )
        } else if (colorOption == AppThemeColor.SYSTEM) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "System Color",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

package com.my_gallery.ui.gallery.header_actions

import androidx.compose.ui.graphics.vector.ImageVector

data class HeaderAction(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit,
    val isSelected: Boolean = false
)

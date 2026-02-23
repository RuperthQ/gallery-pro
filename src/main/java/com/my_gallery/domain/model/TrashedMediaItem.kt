package com.my_gallery.domain.model

import androidx.annotation.Keep

@Keep
data class TrashedMediaItem(
    val id: Long,
    val uri: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val dateAdded: Long,
    val dateExpires: Long,       // Fecha en la que se borrará permanentemente
    val width: Int = 0,
    val height: Int = 0
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val daysUntilExpiry: Int get() {
        val now = System.currentTimeMillis()
        val diff = dateExpires * 1000 - now
        return (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }
}

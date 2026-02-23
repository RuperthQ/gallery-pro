package com.my_gallery.ui.gallery

import android.content.IntentSender
import com.my_gallery.domain.model.MediaItem

data class MediaPendingActions(
    val rename: Pair<MediaItem, String>? = null,
    val deleteIds: List<String>? = null,
    val secureIds: List<String>? = null,
    val intentSender: IntentSender? = null
)

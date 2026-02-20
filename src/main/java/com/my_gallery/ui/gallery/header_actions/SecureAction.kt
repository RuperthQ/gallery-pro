package com.my_gallery.ui.gallery.header_actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import com.my_gallery.ui.gallery.GalleryViewModel

class SecureAction(
    private val viewModel: GalleryViewModel
) {
    operator fun invoke(): HeaderAction {
        return HeaderAction(
            icon = Icons.Default.Lock,
            description = "Asegurar",
            onClick = {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    viewModel.secureSelectedMedia()
                }
            }
        )
    }
}

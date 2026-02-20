package com.my_gallery.ui.gallery.header_actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import com.my_gallery.ui.gallery.GalleryViewModel

class UnsecureAction(
    private val viewModel: GalleryViewModel
) {
    operator fun invoke(): HeaderAction {
        return HeaderAction(
            icon = Icons.Default.LockOpen,
            description = "Desbloquear",
            onClick = {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    viewModel.unsecureSelectedMedia()
                }
            }
        )
    }
}

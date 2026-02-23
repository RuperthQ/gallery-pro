package com.my_gallery.ui.gallery.handlers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.my_gallery.domain.model.AlbumItem
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.gallery.utils.BiometricHandler
import com.my_gallery.ui.security.SecurityViewModel

@Composable
fun rememberAlbumClick(
    context: Context,
    viewModel: GalleryViewModel,
    securityViewModel: SecurityViewModel
): (AlbumItem) -> Unit {
    return remember(context, viewModel, securityViewModel) {
        { album ->
            if (album.id != "ALL_VIRTUAL_ALBUM" && securityViewModel.isAlbumLocked(album.id)) {
                BiometricHandler.authenticateAlbumAction(context, album, true, 
                    onSuccess = { viewModel.toggleAlbum(album.id) })
            } else viewModel.toggleAlbum(album.id)
        }
    }
}

@Composable
fun rememberAlbumLongClick(
    context: Context,
    securityViewModel: SecurityViewModel
): (AlbumItem) -> Unit {
    return remember(context, securityViewModel) {
        { album ->
            if (album.id != "ALL_VIRTUAL_ALBUM") {
                BiometricHandler.authenticateAlbumAction(context, album, securityViewModel.isAlbumLocked(album.id),
                    onSuccess = { securityViewModel.toggleAlbumLock(album.id) })
            }
        }
    }
}

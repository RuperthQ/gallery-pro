package com.my_gallery.ui.gallery.utils

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.my_gallery.domain.model.AlbumItem
import com.my_gallery.ui.security.BiometricPromptManager

object BiometricHandler {
    fun authenticateAlbumAction(
        context: Context,
        album: AlbumItem,
        isLocked: Boolean,
        onSuccess: () -> Unit,
        onFailure: (() -> Unit)? = null
    ) {
        val activity = context as? FragmentActivity ?: return onFailure?.invoke() ?: Unit
        val biom = BiometricPromptManager(activity)
        
        if (biom.canAuthenticate()) {
            biom.authenticate(
                title = if (isLocked) "Desbloquear Álbum" else "Bloquear Álbum",
                subtitle = "Autorización requerida para ${album.name}",
                onSuccess = onSuccess,
                onError = { onFailure?.invoke() }
            )
        } else {
            // Si no hay biometría, permitimos la acción por defecto o podrías manejar pin/patrón
            onSuccess()
        }
    }
}

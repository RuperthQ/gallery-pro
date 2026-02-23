package com.my_gallery.domain.usecase.permissions

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CheckEditPermissionUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(): Boolean {
        // En Android 11+ (R), el permiso de "Acceso a todos los archivos" es el ideal.
        val hasAllFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Environment.isExternalStorageManager()
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }

        // En Android 12+ (S), el permiso de "Administrador de medios" es la alternativa moderna.
        val hasMediaManagement = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                MediaStore.canManageMedia(context)
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }

        // Si tiene cualquiera de los dos, el permiso se considera concedido.
        if (hasAllFilesAccess || hasMediaManagement) return true

        // En Android 10 o inferior, Scoped Storage no impide operaciones básicas de edición 
        // con los permisos estándar READ/WRITE, por lo que el banner no es necesario.
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R
    }
}

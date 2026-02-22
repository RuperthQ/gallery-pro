package com.my_gallery.data.repository.media

import android.content.IntentSender

/**
 * Resultado para operaciones de renombrado.
 */
sealed class RenameResult {
    object Success : RenameResult()
    data class Error(val message: String) : RenameResult()
    data class PermissionRequired(val intentSender: IntentSender) : RenameResult()
}

/**
 * Resultado para operaciones de eliminación.
 */
sealed class DeleteResult {
    data class Success(val count: Int) : DeleteResult()
    data class Error(val message: String) : DeleteResult()
    data class PermissionRequired(val intentSender: IntentSender) : DeleteResult()
}

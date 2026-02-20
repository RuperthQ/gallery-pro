package com.my_gallery

import android.os.Bundle
import android.app.Activity
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.my_gallery.ui.theme.GalleryTheme
import com.my_gallery.ui.gallery.GalleryScreen
import com.my_gallery.ui.security.AppLockScreen
import com.my_gallery.ui.security.SecurityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val securityViewModel: SecurityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // PRIVACIDAD: Ocultar contenido en el selector de apps si el bloqueo está activo
        lifecycleScope.launch {
            securityViewModel.isAppLocked.collect { locked ->
                if (locked) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        setContent {
            GalleryTheme {
                val isAppLocked by securityViewModel.isAppLocked.collectAsStateWithLifecycle(initialValue = false)
                val isAuthenticated by securityViewModel.isAuthenticated.collectAsStateWithLifecycle()

                if (isAppLocked && !isAuthenticated) {
                    AppLockScreen(onAuthenticated = { securityViewModel.setAuthenticated(true) })
                } else {
                    GalleryScreen()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // AUTO-BLOQUEO: Cerrar sesión al salir de la app si el bloqueo está habilitado
        securityViewModel.logout()
    }
}
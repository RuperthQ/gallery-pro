package com.my_gallery

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.my_gallery.data.repository.SettingsRepository
import com.my_gallery.ui.gallery.GalleryScreen
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.security.AppLockScreen
import com.my_gallery.ui.security.SecurityViewModel
import com.my_gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val securityViewModel: SecurityViewModel by viewModels()
    private val galleryViewModel: GalleryViewModel by viewModels()

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        processIntent(intent)

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

        // SALIDA: Cerrar la app si se sale del visor en modo externo
        lifecycleScope.launch {
            galleryViewModel.exitAppEvent.collect {
                finish()
            }
        }

        setContent {
            val themeColor by settingsRepository.themeColor.collectAsStateWithLifecycle()
            GalleryTheme(appThemeColor = themeColor) {
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: android.content.Intent?) {
        if (intent == null) return
        
        when (intent.action) {
            android.content.Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    val mimeType = intent.type
                    galleryViewModel.handleExternalIntent(uri, mimeType)
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
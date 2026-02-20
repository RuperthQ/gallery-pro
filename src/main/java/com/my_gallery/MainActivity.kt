package com.my_gallery

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.my_gallery.ui.theme.GalleryTheme
import com.my_gallery.ui.gallery.GalleryScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my_gallery.ui.security.AppLockScreen
import com.my_gallery.ui.security.SecurityViewModel
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GalleryTheme {
                val securityViewModel: SecurityViewModel = hiltViewModel()
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
}
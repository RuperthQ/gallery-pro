package com.my_gallery.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.security.SecurityViewModel
import com.my_gallery.ui.settings.components.*
import com.my_gallery.ui.theme.GalleryDesign

/**
 * Pantalla de Ajustes Orquestadora.
 * Delega la UI a componentes modulares manteniendo la lógica de estado centralizada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    galleryViewModel: GalleryViewModel,
    securityViewModel: SecurityViewModel,
    onBack: () -> Unit
) {
    // --- ESTADO ---
    val menuStyle by galleryViewModel.menuStyle.collectAsStateWithLifecycle()
    val isAppLocked by securityViewModel.isAppLocked.collectAsStateWithLifecycle(initialValue = false)
    val columnCount by galleryViewModel.columnCount.collectAsStateWithLifecycle()
    val showEmptyAlbums by galleryViewModel.showEmptyAlbums.collectAsStateWithLifecycle()
    val albumBehavior by galleryViewModel.albumBehavior.collectAsStateWithLifecycle()
    val themeColor by galleryViewModel.themeColor.collectAsStateWithLifecycle()
    val autoplayEnabled by galleryViewModel.autoplayEnabled.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Ajustes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(GalleryDesign.PaddingLarge),
            verticalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingLarge)
        ) {
            // 1. SECCIÓN: PERSONALIZACIÓN
            PersonalizationSection(
                menuStyle = menuStyle,
                albumBehavior = albumBehavior,
                columnCount = columnCount,
                themeColor = themeColor,
                showEmptyAlbums = showEmptyAlbums,
                onMenuStyleChange = { galleryViewModel.setMenuStyle(it) },
                onAlbumBehaviorChange = { galleryViewModel.setAlbumBehavior(it) },
                onColumnCountChange = { galleryViewModel.changeColumns() },
                onThemeColorChange = { galleryViewModel.setThemeColor(it) },
                onToggleEmptyAlbums = { galleryViewModel.toggleShowEmptyAlbums() }
            )

            // 2. SECCIÓN: SEGURIDAD
            SecuritySection(
                isAppLocked = isAppLocked,
                onToggleAppLock = { securityViewModel.toggleAppLock(it) },
                onManageVault = { /* TODO: Navegar a gestión de bóveda */ }
            )

            // 3. SECCIÓN: REPRODUCTOR
            PlayerSection(
                autoplayEnabled = autoplayEnabled,
                onToggleAutoplay = { galleryViewModel.toggleAutoplay() }
            )

            // 4. SECCIÓN: ACERCA DE
            AboutSection()
            
            Spacer(modifier = Modifier.height(GalleryDesign.PaddingLarge))
        }
    }
}

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
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.my_gallery.ui.security.BiometricPromptManager
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.gallery.MenuStyle
import com.my_gallery.ui.security.SecurityViewModel
import com.my_gallery.ui.settings.components.*
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.components.PremiumAlertDialog

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
    val autoNavigateAfterMove by galleryViewModel.autoNavigateAfterMove.collectAsStateWithLifecycle()
    val startInLastAlbum by galleryViewModel.startInLastAlbum.collectAsStateWithLifecycle()
    val shortDateFilters by galleryViewModel.shortDateFilters.collectAsStateWithLifecycle()
    val showFilterType by galleryViewModel.showFilterType.collectAsStateWithLifecycle()
    val showFilterRes by galleryViewModel.showFilterRes.collectAsStateWithLifecycle()
    val showFilterExt by galleryViewModel.showFilterExt.collectAsStateWithLifecycle()

    val showVaultInCarousel by galleryViewModel.showVaultInCarousel.collectAsStateWithLifecycle()
    val lockSettingsScreen by galleryViewModel.lockSettingsScreen.collectAsStateWithLifecycle()

    val fmCreateVisible by galleryViewModel.floatingMenuCreateVisible.collectAsStateWithLifecycle()
    val fmFilterVisible by galleryViewModel.floatingMenuFilterVisible.collectAsStateWithLifecycle()
    val fmSelectVisible by galleryViewModel.floatingMenuSelectVisible.collectAsStateWithLifecycle()

    val tmCreateVisible by galleryViewModel.topMenuCreateVisible.collectAsStateWithLifecycle()
    val tmGridVisible by galleryViewModel.topMenuGridVisible.collectAsStateWithLifecycle()
    val tmFilterVisible by galleryViewModel.topMenuFilterVisible.collectAsStateWithLifecycle()
    val tmEmptyVisible by galleryViewModel.topMenuEmptyVisible.collectAsStateWithLifecycle()
    val tmSelectVisible by galleryViewModel.topMenuSelectVisible.collectAsStateWithLifecycle()

    var isSettingsAuthSuccess by remember { mutableStateOf(false) }
    var showEmptyVaultAlert by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(lockSettingsScreen) {
        if (lockSettingsScreen && !isSettingsAuthSuccess) {
            val biom = (context as? FragmentActivity)?.let { BiometricPromptManager(it) }
            if (biom?.canAuthenticate() == true) {
                biom.authenticate(
                    title = "Acceso a Ajustes",
                    subtitle = "Autorización requerida",
                    onSuccess = { isSettingsAuthSuccess = true },
                    onError = { onBack() }
                )
            } else {
                isSettingsAuthSuccess = true
            }
        } else {
            isSettingsAuthSuccess = true
        }
    }

    if (!isSettingsAuthSuccess && lockSettingsScreen) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    if (showEmptyVaultAlert) {
        PremiumAlertDialog(
            onDismissRequest = { showEmptyVaultAlert = false },
            title = "Bóveda vacía",
            text = {
                Text(
                    "Aún no tiene elementos guardados allí",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showEmptyVaultAlert = false }) {
                    Text("Aceptar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = null
        )
    }

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

            // 2. SECCIÓN: MENÚ FLOTANTE (Solo si estilo flotante está seleccionado)
            if (menuStyle == MenuStyle.BOTTOM_FLOATING) {
                FloatingMenuSettingsSection(
                    createVisible = fmCreateVisible,
                    filterVisible = fmFilterVisible,
                    selectVisible = fmSelectVisible,
                    onToggleCreate = { galleryViewModel.toggleFloatingMenuCreate() },
                    onToggleFilter = { galleryViewModel.toggleFloatingMenuFilter() },
                    onToggleSelect = { galleryViewModel.toggleFloatingMenuSelect() }
                )
            }

            if (menuStyle == MenuStyle.TOP_HEADER) {
                TopMenuSettingsSection(
                    createVisible = tmCreateVisible,
                    gridVisible = tmGridVisible,
                    filterVisible = tmFilterVisible,
                    emptyVisible = tmEmptyVisible,
                    selectVisible = tmSelectVisible,
                    onToggleCreate = { galleryViewModel.toggleTopMenuCreate() },
                    onToggleGrid = { galleryViewModel.toggleTopMenuGrid() },
                    onToggleFilter = { galleryViewModel.toggleTopMenuFilter() },
                    onToggleEmpty = { galleryViewModel.toggleTopMenuEmpty() },
                    onToggleSelect = { galleryViewModel.toggleTopMenuSelect() }
                )
            }

            // 3. SECCIÓN: SEGURIDAD
            SecuritySection(
                isAppLocked = isAppLocked,
                lockSettingsScreen = lockSettingsScreen,
                showVaultInCarousel = showVaultInCarousel,
                onToggleAppLock = { securityViewModel.toggleAppLock(it) },
                onToggleLockSettings = { galleryViewModel.toggleLockSettingsScreen() },
                onToggleVaultInCarousel = { galleryViewModel.toggleShowVaultInCarousel() },
                onManageVault = { 
                    galleryViewModel.checkAndOpenVault(
                        onEmpty = { showEmptyVaultAlert = true },
                        onOpen = { onBack() }
                    )
                }
            )

            // 4. SECCIÓN: REPRODUCTOR
            PlayerSection(
                autoplayEnabled = autoplayEnabled,
                onToggleAutoplay = { galleryViewModel.toggleAutoplay() }
            )

            // 5. SECCIÓN: INTERACCIÓN
            InteractionSection(
                autoNavigateEnabled = autoNavigateAfterMove,
                onToggleAutoNavigate = { galleryViewModel.toggleAutoNavigate() },
                shortDateEnabled = shortDateFilters,
                onToggleShortDate = { galleryViewModel.toggleShortDateFilters() },
                startInLastAlbumEnabled = startInLastAlbum,
                onToggleStartInLastAlbum = { galleryViewModel.toggleStartInLastAlbum() }
            )

            // 6. SECCIÓN: FILTROS
            FilterSettingsSection(
                showType = showFilterType,
                showRes = showFilterRes,
                showExt = showFilterExt,
                onToggleType = { galleryViewModel.toggleFilterType() },
                onToggleRes = { galleryViewModel.toggleFilterRes() },
                onToggleExt = { galleryViewModel.toggleFilterExt() }
            )

            // 7. SECCIÓN: ACERCA DE
            AboutSection()
            
            Spacer(modifier = Modifier.height(GalleryDesign.PaddingLarge))
        }
    }
}

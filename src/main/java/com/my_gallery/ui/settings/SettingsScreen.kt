package com.my_gallery.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.gallery.MenuStyle
import com.my_gallery.ui.gallery.AlbumBehavior
import com.my_gallery.ui.security.SecurityViewModel
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder
import com.my_gallery.ui.theme.AppThemeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    galleryViewModel: GalleryViewModel,
    securityViewModel: SecurityViewModel,
    onBack: () -> Unit
) {
    val menuStyle by galleryViewModel.menuStyle.collectAsStateWithLifecycle()
    val isAppLocked by securityViewModel.isAppLocked.collectAsStateWithLifecycle(initialValue = false)
    val columnCount by galleryViewModel.columnCount.collectAsStateWithLifecycle()
    val showEmptyAlbums by galleryViewModel.showEmptyAlbums.collectAsStateWithLifecycle()
    val albumBehavior by galleryViewModel.albumBehavior.collectAsStateWithLifecycle()
    val themeColor by galleryViewModel.themeColor.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Ajustes", 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
            // SECCIÓN: INTERFAZ
            SettingsSectionTitle("Personalización")
            
            SettingsCard {
                Column {
                    SettingsGroupLabel("Menú")
                    
                    SettingsOptionRow(
                        title = "Estilo de Menú",
                        description = null,
                        icon = Icons.Default.Style
                    ) {
                        SegmentedMenuStyleSelector(
                            currentStyle = menuStyle,
                            onStyleSelected = { galleryViewModel.setMenuStyle(it) }
                        )
                    }

                    AnimatedVisibility(visible = menuStyle == MenuStyle.BOTTOM_FLOATING) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = GalleryDesign.PaddingMedium),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            
                            SettingsToggleRow(
                                title = "Carrusel Flotante",
                                description = "El carrusel se mantiene flotando como un panel de cristal arriba.",
                                icon = Icons.Default.FilterFrames,
                                checked = albumBehavior == AlbumBehavior.FLOATING_TOP,
                                onCheckedChange = { checked ->
                                    if (checked) galleryViewModel.setAlbumBehavior(AlbumBehavior.FLOATING_TOP)
                                    else galleryViewModel.setAlbumBehavior(AlbumBehavior.FIXED_IN_GRID)
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = GalleryDesign.PaddingMedium),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            SettingsToggleRow(
                                title = "Carrusel Estático",
                                description = "Se mantiene arriba pero sin panel, dejando ver qué hay debajo.",
                                icon = Icons.Default.VerticalAlignTop,
                                checked = albumBehavior == AlbumBehavior.STATIC_TOP,
                                onCheckedChange = { checked ->
                                    if (checked) galleryViewModel.setAlbumBehavior(AlbumBehavior.STATIC_TOP)
                                    else galleryViewModel.setAlbumBehavior(AlbumBehavior.FIXED_IN_GRID)
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(GalleryDesign.PaddingSmall))
                    SettingsGroupLabel("Vista")
                    
                    SettingsOptionRow(
                        title = "Columnas del Grid",
                        description = "Ajusta cuántas fotos se ven por fila ($columnCount).",
                        icon = Icons.Default.GridView
                    ) {
                        IconButton(onClick = { galleryViewModel.changeColumns() }) {
                            Icon(
                                Icons.Default.SwapHoriz, 
                                contentDescription = "Cambiar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(GalleryDesign.PaddingSmall))
                    SettingsGroupLabel("Tema")
                    
                    SettingsOptionRow(
                        title = "Color de Tema",
                        description = "Elige tu tonalidad preferida.",
                        icon = Icons.Default.Palette
                    ) {}
                    
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = GalleryDesign.PaddingSmall, start = 30.dp), // Alinear con el texto
                        horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingMedium)
                    ) {
                        items(AppThemeColor.entries.toTypedArray()) { colorOpt ->
                            ThemeColorCircle(
                                colorOption = colorOpt,
                                isSelected = themeColor == colorOpt,
                                onClick = { galleryViewModel.setThemeColor(colorOpt) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(GalleryDesign.PaddingSmall))
                    SettingsGroupLabel("Privacidad")

                    SettingsToggleRow(
                        title = "Mostrar álbumes vacíos",
                        description = "Incluye carpetas que no tienen archivos visibles.",
                        icon = Icons.Default.FolderOpen,
                        checked = showEmptyAlbums,
                        onCheckedChange = { galleryViewModel.toggleShowEmptyAlbums() }
                    )
                }
            }

            // SECCIÓN: SEGURIDAD
            SettingsSectionTitle("Seguridad")
            
            SettingsCard {
                Column {
                    SettingsToggleRow(
                        title = "Bloqueo de Aplicación",
                        description = "Usa biometría para acceder a la galería.",
                        icon = Icons.Default.Fingerprint,
                        checked = isAppLocked,
                        onCheckedChange = { securityViewModel.toggleAppLock(it) }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = GalleryDesign.PaddingMedium),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    SettingsOptionRow(
                        title = "Bóveda Privada",
                        description = "Administra tus archivos cifrados.",
                        icon = Icons.Default.EnhancedEncryption,
                        onClick = { /* Navegar a gestion de bóveda */ }
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            // SECCIÓN: INFORMACIÓN
            SettingsSectionTitle("Acerca de")
            
            SettingsCard {
                SettingsOptionRow(
                    title = "Galería Pro",
                    description = "Versión 1.0.0 - Diseño Premium",
                    icon = Icons.Default.Info
                ) {
                    Text(
                        "Premium",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(GalleryDesign.PaddingLarge))
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .premiumBorder(shape = GalleryDesign.CardShape),
        shape = GalleryDesign.CardShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Box(modifier = Modifier.padding(GalleryDesign.PaddingLarge)) {
            content()
        }
    }
}

@Composable
fun SettingsOptionRow(
    title: String,
    description: String? = null,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(GalleryDesign.CardShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)).padding(GalleryDesign.PaddingSmall).clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(GalleryDesign.PaddingMedium))
        Column(modifier = Modifier.weight(1f).padding(end = GalleryDesign.PaddingSmall)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (!description.isNullOrEmpty()) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailingContent()
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    description: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsOptionRow(
        title = title,
        description = description,
        icon = icon
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                checkedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SegmentedMenuStyleSelector(
    currentStyle: MenuStyle,
    onStyleSelected: (MenuStyle) -> Unit
) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .premiumBorder(shape = GalleryDesign.FilterShape)
            .clip(GalleryDesign.FilterShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuStyleButton(
            label = "Superior",
            selected = currentStyle == MenuStyle.TOP_HEADER,
            onClick = { onStyleSelected(MenuStyle.TOP_HEADER) }
        )
        MenuStyleButton(
            label = "Flotante",
            selected = currentStyle == MenuStyle.BOTTOM_FLOATING,
            onClick = { onStyleSelected(MenuStyle.BOTTOM_FLOATING) }
        )
    }
}

@Composable
fun MenuStyleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clip(GalleryDesign.FilterShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun SettingsGroupLabel(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GalleryDesign.PaddingSmall, horizontal = GalleryDesign.PaddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.width(GalleryDesign.PaddingSmall))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun ThemeColorCircle(
    colorOption: AppThemeColor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val displayColor = colorOption.colorValue ?: MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(displayColor)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) borderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (colorOption == AppThemeColor.SYSTEM) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                modifier = Modifier.size(20.dp)
            )
        } else if (colorOption == AppThemeColor.SYSTEM) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "System Color",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

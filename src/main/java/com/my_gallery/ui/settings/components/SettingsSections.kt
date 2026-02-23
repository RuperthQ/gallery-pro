package com.my_gallery.ui.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.my_gallery.ui.gallery.AlbumBehavior
import com.my_gallery.ui.gallery.MenuStyle
import com.my_gallery.ui.theme.AppThemeColor
import com.my_gallery.ui.theme.GalleryDesign

@Composable
fun PersonalizationSection(
    menuStyle: MenuStyle,
    albumBehavior: AlbumBehavior,
    columnCount: Int,
    themeColor: AppThemeColor,
    showEmptyAlbums: Boolean,
    onMenuStyleChange: (MenuStyle) -> Unit,
    onAlbumBehaviorChange: (AlbumBehavior) -> Unit,
    onColumnCountChange: () -> Unit,
    onThemeColorChange: (AppThemeColor) -> Unit,
    onToggleEmptyAlbums: () -> Unit
) {
    Column {
        SettingsSectionTitle("Personalización")
        SettingsCard {
            Column {
                SettingsGroupLabel("Menú")
                SettingsOptionRow(
                    title = "Estilo de Menú",
                    icon = Icons.Default.Style
                ) {
                    SegmentedMenuStyleSelector(
                        currentStyle = menuStyle,
                        onStyleSelected = onMenuStyleChange
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
                                onAlbumBehaviorChange(if (checked) AlbumBehavior.FLOATING_TOP else AlbumBehavior.FIXED_IN_GRID)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = GalleryDesign.PaddingMedium), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant
                        )
                        SettingsToggleRow(
                            title = "Carrusel Estático",
                            description = "Se mantiene arriba pero sin panel, dejando ver qué hay debajo.",
                            icon = Icons.Default.VerticalAlignTop,
                            checked = albumBehavior == AlbumBehavior.STATIC_TOP,
                            onCheckedChange = { checked ->
                                onAlbumBehaviorChange(if (checked) AlbumBehavior.STATIC_TOP else AlbumBehavior.FIXED_IN_GRID)
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
                    IconButton(onClick = onColumnCountChange) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Cambiar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Spacer(modifier = Modifier.height(GalleryDesign.PaddingSmall))
                SettingsGroupLabel("Tema")
                SettingsOptionRow(
                    title = "Color de Tema",
                    description = "Elige tu tonalidad preferida.",
                    icon = Icons.Default.Palette
                )
                ThemeColorSelector(
                    currentTheme = themeColor,
                    onThemeSelected = onThemeColorChange
                )

                Spacer(modifier = Modifier.height(GalleryDesign.PaddingSmall))
                SettingsGroupLabel("Privacidad")
                SettingsToggleRow(
                    title = "Mostrar álbumes vacíos",
                    description = "Incluye carpetas que no tienen archivos visibles.",
                    icon = Icons.Default.FolderOpen,
                    checked = showEmptyAlbums,
                    onCheckedChange = { onToggleEmptyAlbums() }
                )
            }
        }
    }
}

@Composable
fun SecuritySection(
    isAppLocked: Boolean,
    onToggleAppLock: (Boolean) -> Unit,
    onManageVault: () -> Unit
) {
    Column {
        SettingsSectionTitle("Seguridad")
        SettingsCard {
            Column {
                SettingsToggleRow(
                    title = "Bloqueo de Aplicación",
                    description = "Usa biometría para acceder a la galería.",
                    icon = Icons.Default.Fingerprint,
                    checked = isAppLocked,
                    onCheckedChange = onToggleAppLock
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = GalleryDesign.PaddingMedium), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                SettingsOptionRow(
                    title = "Bóveda Privada",
                    description = "Administra tus archivos cifrados.",
                    icon = Icons.Default.EnhancedEncryption,
                    onClick = onManageVault
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun PlayerSection(
    autoplayEnabled: Boolean,
    onToggleAutoplay: (Boolean) -> Unit
) {
    Column {
        SettingsSectionTitle("Reproductor")
        SettingsCard {
            SettingsToggleRow(
                title = "Auto-reproducción",
                description = "Los videos comenzarán a reproducirse automáticamente al abrirlos.",
                icon = Icons.Default.PlayCircle,
                checked = autoplayEnabled,
                onCheckedChange = onToggleAutoplay
            )
        }
    }
}

@Composable
fun InteractionSection(
    autoNavigateEnabled: Boolean,
    onToggleAutoNavigate: (Boolean) -> Unit,
    shortDateEnabled: Boolean,
    onToggleShortDate: (Boolean) -> Unit,
    startInLastAlbumEnabled: Boolean,
    onToggleStartInLastAlbum: (Boolean) -> Unit
) {
    Column {
        SettingsSectionTitle("Interacción")
        SettingsCard {
            Column {
                SettingsToggleRow(
                    title = "Iniciar en último álbum",
                    description = "Al abrir la app, restaurar el último álbum visitado o mostrar todos los medios.",
                    icon = Icons.Default.Restore,
                    checked = startInLastAlbumEnabled,
                    onCheckedChange = onToggleStartInLastAlbum
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = GalleryDesign.PaddingMedium),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                SettingsToggleRow(
                    title = "Navegación Automática",
                    description = "Entra directamente al nuevo álbum tras crear o mover archivos.",
                    icon = Icons.Default.Launch,
                    checked = autoNavigateEnabled,
                    onCheckedChange = onToggleAutoNavigate
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = GalleryDesign.PaddingMedium),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                SettingsToggleRow(
                    title = "Fechas Cortas",
                    description = "Abrevia los meses en los filtros.\nEj: Ene 2024",
                    icon = Icons.Default.CalendarViewMonth,
                    checked = shortDateEnabled,
                    onCheckedChange = onToggleShortDate
                )
            }
        }
    }
}

@Composable
fun FilterSettingsSection(
    showType: Boolean,
    showRes: Boolean,
    showExt: Boolean,
    onToggleType: (Boolean) -> Unit,
    onToggleRes: (Boolean) -> Unit,
    onToggleExt: (Boolean) -> Unit
) {
    Column {
        SettingsSectionTitle("Filtros")
        SettingsCard {
            Column {
                SettingsToggleRow(
                    title = "Agrupar por Tipo",
                    description = "Ver filtros de Imagen y Video.",
                    icon = Icons.Default.FilterList,
                    checked = showType,
                    onCheckedChange = onToggleType
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = GalleryDesign.PaddingMedium), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = "Agrupar por Resolución",
                    description = "Ver filtros de 4K, 2K, 1080P, etc.",
                    icon = Icons.Default.AspectRatio,
                    checked = showRes,
                    onCheckedChange = onToggleRes
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = GalleryDesign.PaddingMedium), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    title = "Agrupar por Extensión",
                    description = "Ver filtros de JPG, PNG, RAW, MP4, etc.",
                    icon = Icons.Default.Extension,
                    checked = showExt,
                    onCheckedChange = onToggleExt
                )
            }
        }
    }
}

@Composable
fun AboutSection() {
    Column {
        SettingsSectionTitle("Acerca de")
        SettingsCard {
            SettingsOptionRow(
                title = "Galería Pro",
                description = "Versión 1.0.0 - Diseño Premium",
                icon = Icons.Default.Info
            ) {
                Text("Premium", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

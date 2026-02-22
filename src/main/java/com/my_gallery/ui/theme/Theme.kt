package com.my_gallery.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeColor(val title: String, val colorValue: Color?) {
    SYSTEM("Sistema", null),
    PINK("Rosa", Color(0xFFE91E63)),
    RED("Rojo", Color(0xFFF44336)),
    ORANGE("Naranja", Color(0xFFFF9800)),
    GREEN("Verde", Color(0xFF4CAF50)),
    BLUE("Azul", Color(0xFF2196F3)),
    PURPLE("Lila", Color(0xFF673AB7))
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    background = AmoledBlack,
    surface = AmoledBlack,
    surfaceVariant = Color(0xFF121212), // Un tono muy oscuro para tarjetas si es necesario
    error = ErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    background = BackgroundLight,
    surface = SurfaceLight
)

@Composable
fun GalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appThemeColor: AppThemeColor = AppThemeColor.SYSTEM,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        appThemeColor != AppThemeColor.SYSTEM -> {
            val base = if (darkTheme) DarkColorScheme else LightColorScheme
            base.copy(
                primary = appThemeColor.colorValue ?: base.primary
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val baseScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme) {
                baseScheme.copy(
                    background = AmoledBlack,
                    surface = AmoledBlack,
                    surfaceVariant = Color(0xFF121212)
                )
            } else baseScheme
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
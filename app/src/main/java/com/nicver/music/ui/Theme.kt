package com.nicver.music.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nicver.music.data.AppStore

class Accent(val name: String, val color: Color)

val ACCENTS = listOf(
    Accent("Biru", Color(0xFF3D6CF2)),
    Accent("Hijau", Color(0xFF2E9E6B)),
    Accent("Sakura", Color(0xFFE5619A)),
    Accent("Senja", Color(0xFFF08A3C)),
    Accent("Samudra", Color(0xFF1AA3B8)),
    Accent("Kopi", Color(0xFF8B5E3C)),
)

val THEME_MODES = listOf("Sistem", "Terang", "Gelap")

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = when (AppStore.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val base = ACCENTS[AppStore.accent.coerceIn(0, ACCENTS.lastIndex)].color
    val scheme = if (dark) {
        darkColorScheme(
            primary = lerp(base, Color.White, 0.35f),
            onPrimary = Color(0xFF101014),
            primaryContainer = lerp(base, Color.Black, 0.55f),
            onPrimaryContainer = Color.White,
            secondaryContainer = lerp(base, Color.Black, 0.6f),
            onSecondaryContainer = Color.White,
            background = Color(0xFF121214),
            onBackground = Color(0xFFEDEDF0),
            surface = Color(0xFF121214),
            onSurface = Color(0xFFEDEDF0),
            surfaceVariant = Color(0xFF26262B),
            onSurfaceVariant = Color(0xFFB9B9C2),
        )
    } else {
        darkLightScheme(base)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val c = WindowCompat.getInsetsController(window, view)
                c.isAppearanceLightStatusBars = !dark
                c.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private fun darkLightScheme(base: Color) = lightColorScheme(
    primary = base,
    onPrimary = Color.White,
    primaryContainer = lerp(base, Color.White, 0.82f),
    onPrimaryContainer = lerp(base, Color.Black, 0.6f),
    secondaryContainer = lerp(base, Color.White, 0.86f),
    onSecondaryContainer = lerp(base, Color.Black, 0.6f),
    background = Color(0xFFFCFCFE),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFCFCFE),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFEDEDF2),
    onSurfaceVariant = Color(0xFF55555E),
)

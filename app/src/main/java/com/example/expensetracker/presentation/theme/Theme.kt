package com.example.expensetracker.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.expensetracker.data.preferences.ThemePalette

fun buildThemeColorScheme(darkTheme: Boolean, palette: ThemePalette): ColorScheme {
    return when (palette) {
        ThemePalette.EMERALD -> if (darkTheme) EmeraldDarkColorScheme else EmeraldLightColorScheme
        ThemePalette.BLUE_GOLD -> if (darkTheme) BlueGoldDarkColorScheme else BlueGoldLightColorScheme
        ThemePalette.WARM_EARTH -> if (darkTheme) WarmEarthDarkColorScheme else WarmEarthLightColorScheme
        ThemePalette.SLATE_TEAL -> if (darkTheme) SlateTealDarkColorScheme else SlateTealLightColorScheme
    }
}

// ── 1. Emerald
private val EmeraldLightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldPrimaryContainerLight,
    onPrimaryContainer = EmeraldOnPrimaryContainerLight,
    secondary = EmeraldSecondaryLight,
    onSecondary = EmeraldOnSecondaryLight,
    secondaryContainer = EmeraldSecondaryContainerLight,
    onSecondaryContainer = EmeraldOnSecondaryContainerLight,
    tertiary = EmeraldTertiaryLight,
    onTertiary = EmeraldOnTertiaryLight,
    tertiaryContainer = EmeraldTertiaryContainerLight,
    onTertiaryContainer = EmeraldOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = EmeraldSurfaceLight,
    onBackground = EmeraldOnSurfaceLight,
    surface = EmeraldSurfaceLight,
    onSurface = EmeraldOnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = EmeraldSurfaceContainerLowLight,
    surfaceContainer = EmeraldSurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val EmeraldDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldOnPrimaryContainerDark,
    secondary = EmeraldSecondaryDark,
    onSecondary = EmeraldOnSecondaryDark,
    secondaryContainer = EmeraldSecondaryContainerDark,
    onSecondaryContainer = EmeraldOnSecondaryContainerDark,
    tertiary = EmeraldTertiaryDark,
    onTertiary = EmeraldOnTertiaryDark,
    tertiaryContainer = EmeraldTertiaryContainerDark,
    onTertiaryContainer = EmeraldOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = EmeraldSurfaceDark,
    onBackground = EmeraldOnSurfaceDark,
    surface = EmeraldSurfaceDark,
    onSurface = EmeraldOnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = EmeraldSurfaceContainerLowDark,
    surfaceContainer = EmeraldSurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

// ── 2. Blue & Gold Luxury
private val BlueGoldLightColorScheme = lightColorScheme(
    primary = BlueGoldPrimaryLight,
    onPrimary = BlueGoldOnPrimaryLight,
    primaryContainer = BlueGoldPrimaryContainerLight,
    onPrimaryContainer = BlueGoldOnPrimaryContainerLight,
    secondary = BlueGoldSecondaryLight,
    onSecondary = BlueGoldOnSecondaryLight,
    secondaryContainer = BlueGoldSecondaryContainerLight,
    onSecondaryContainer = BlueGoldOnSecondaryContainerLight,
    tertiary = BlueGoldTertiaryLight,
    onTertiary = BlueGoldOnTertiaryLight,
    tertiaryContainer = BlueGoldTertiaryContainerLight,
    onTertiaryContainer = BlueGoldOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BlueGoldSurfaceLight,
    onBackground = BlueGoldOnSurfaceLight,
    surface = BlueGoldSurfaceLight,
    onSurface = BlueGoldOnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = BlueGoldSurfaceContainerLowLight,
    surfaceContainer = BlueGoldSurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val BlueGoldDarkColorScheme = darkColorScheme(
    primary = BlueGoldPrimaryDark,
    onPrimary = BlueGoldOnPrimaryDark,
    primaryContainer = BlueGoldPrimaryContainerDark,
    onPrimaryContainer = BlueGoldOnPrimaryContainerDark,
    secondary = BlueGoldSecondaryDark,
    onSecondary = BlueGoldOnSecondaryDark,
    secondaryContainer = BlueGoldSecondaryContainerDark,
    onSecondaryContainer = BlueGoldOnSecondaryContainerDark,
    tertiary = BlueGoldTertiaryDark,
    onTertiary = BlueGoldOnTertiaryDark,
    tertiaryContainer = BlueGoldTertiaryContainerDark,
    onTertiaryContainer = BlueGoldOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BlueGoldSurfaceDark,
    onBackground = BlueGoldOnSurfaceDark,
    surface = BlueGoldSurfaceDark,
    onSurface = BlueGoldOnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = BlueGoldSurfaceContainerLowDark,
    surfaceContainer = BlueGoldSurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

// ── 3. Warm Earth Tones
private val WarmEarthLightColorScheme = lightColorScheme(
    primary = WarmEarthPrimaryLight,
    onPrimary = WarmEarthOnPrimaryLight,
    primaryContainer = WarmEarthPrimaryContainerLight,
    onPrimaryContainer = WarmEarthOnPrimaryContainerLight,
    secondary = WarmEarthSecondaryLight,
    onSecondary = WarmEarthOnSecondaryLight,
    secondaryContainer = WarmEarthSecondaryContainerLight,
    onSecondaryContainer = WarmEarthOnSecondaryContainerLight,
    tertiary = WarmEarthTertiaryLight,
    onTertiary = WarmEarthOnTertiaryLight,
    tertiaryContainer = WarmEarthTertiaryContainerLight,
    onTertiaryContainer = WarmEarthOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = WarmEarthSurfaceLight,
    onBackground = WarmEarthOnSurfaceLight,
    surface = WarmEarthSurfaceLight,
    onSurface = WarmEarthOnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = WarmEarthSurfaceContainerLowLight,
    surfaceContainer = WarmEarthSurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val WarmEarthDarkColorScheme = darkColorScheme(
    primary = WarmEarthPrimaryDark,
    onPrimary = WarmEarthOnPrimaryDark,
    primaryContainer = WarmEarthPrimaryContainerDark,
    onPrimaryContainer = WarmEarthOnPrimaryContainerDark,
    secondary = WarmEarthSecondaryDark,
    onSecondary = WarmEarthOnSecondaryDark,
    secondaryContainer = WarmEarthSecondaryContainerDark,
    onSecondaryContainer = WarmEarthOnSecondaryContainerDark,
    tertiary = WarmEarthTertiaryDark,
    onTertiary = WarmEarthOnTertiaryDark,
    tertiaryContainer = WarmEarthTertiaryContainerDark,
    onTertiaryContainer = WarmEarthOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = WarmEarthSurfaceDark,
    onBackground = WarmEarthOnSurfaceDark,
    surface = WarmEarthSurfaceDark,
    onSurface = WarmEarthOnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = WarmEarthSurfaceContainerLowDark,
    surfaceContainer = WarmEarthSurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

// ── 4. Cool Slate & Teal
private val SlateTealLightColorScheme = lightColorScheme(
    primary = SlateTealPrimaryLight,
    onPrimary = SlateTealOnPrimaryLight,
    primaryContainer = SlateTealPrimaryContainerLight,
    onPrimaryContainer = SlateTealOnPrimaryContainerLight,
    secondary = SlateTealSecondaryLight,
    onSecondary = SlateTealOnSecondaryLight,
    secondaryContainer = SlateTealSecondaryContainerLight,
    onSecondaryContainer = SlateTealOnSecondaryContainerLight,
    tertiary = SlateTealTertiaryLight,
    onTertiary = SlateTealOnTertiaryLight,
    tertiaryContainer = SlateTealTertiaryContainerLight,
    onTertiaryContainer = SlateTealOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = SlateTealSurfaceLight,
    onBackground = SlateTealOnSurfaceLight,
    surface = SlateTealSurfaceLight,
    onSurface = SlateTealOnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SlateTealSurfaceContainerLowLight,
    surfaceContainer = SlateTealSurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val SlateTealDarkColorScheme = darkColorScheme(
    primary = SlateTealPrimaryDark,
    onPrimary = SlateTealOnPrimaryDark,
    primaryContainer = SlateTealPrimaryContainerDark,
    onPrimaryContainer = SlateTealOnPrimaryContainerDark,
    secondary = SlateTealSecondaryDark,
    onSecondary = SlateTealOnSecondaryDark,
    secondaryContainer = SlateTealSecondaryContainerDark,
    onSecondaryContainer = SlateTealOnSecondaryContainerDark,
    tertiary = SlateTealTertiaryDark,
    onTertiary = SlateTealOnTertiaryDark,
    tertiaryContainer = SlateTealTertiaryContainerDark,
    onTertiaryContainer = SlateTealOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = SlateTealSurfaceDark,
    onBackground = SlateTealOnSurfaceDark,
    surface = SlateTealSurfaceDark,
    onSurface = SlateTealOnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SlateTealSurfaceContainerLowDark,
    surfaceContainer = SlateTealSurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: ThemePalette = ThemePalette.EMERALD,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> buildThemeColorScheme(darkTheme = darkTheme, palette = palette)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

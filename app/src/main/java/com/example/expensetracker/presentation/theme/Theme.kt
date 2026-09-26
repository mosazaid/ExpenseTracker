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
        ThemePalette.AMETHYST_VIOLET -> if (darkTheme) AmethystDarkColorScheme else AmethystLightColorScheme
        ThemePalette.CRIMSON_BURGUNDY -> if (darkTheme) CrimsonDarkColorScheme else CrimsonLightColorScheme
        ThemePalette.MIDNIGHT_OCEAN -> if (darkTheme) MidnightDarkColorScheme else MidnightLightColorScheme
        ThemePalette.FOREST_MINT -> if (darkTheme) ForestDarkColorScheme else ForestLightColorScheme
        ThemePalette.ROSE_GOLD -> if (darkTheme) RoseGoldDarkColorScheme else RoseGoldLightColorScheme
        ThemePalette.OBSIDIAN_GOLD -> if (darkTheme) ObsidianDarkColorScheme else ObsidianLightColorScheme
        ThemePalette.NORDIC_FROST -> if (darkTheme) NordicDarkColorScheme else NordicLightColorScheme
        ThemePalette.ESPRESSO_MOCHA -> if (darkTheme) EspressoDarkColorScheme else EspressoLightColorScheme
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

// ── 5. Amethyst & Violet
private val AmethystLightColorScheme = lightColorScheme(
    primary = AmethystPrimaryLight,
    onPrimary = AmethystOnPrimaryLight,
    primaryContainer = AmethystPrimaryContainerLight,
    onPrimaryContainer = AmethystOnPrimaryContainerLight,
    secondary = AmethystSecondaryLight,
    onSecondary = AmethystOnSecondaryLight,
    secondaryContainer = AmethystSecondaryContainerLight,
    onSecondaryContainer = AmethystOnSecondaryContainerLight,
    tertiary = AmethystTertiaryLight,
    onTertiary = AmethystOnTertiaryLight,
    tertiaryContainer = AmethystTertiaryContainerLight,
    onTertiaryContainer = AmethystOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = AmethystSurfaceLight,
    onBackground = AmethystOnSurfaceLight,
    surface = AmethystSurfaceLight,
    onSurface = AmethystOnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = AmethystSurfaceContainerLowLight,
    surfaceContainer = AmethystSurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val AmethystDarkColorScheme = darkColorScheme(
    primary = AmethystPrimaryDark,
    onPrimary = AmethystOnPrimaryDark,
    primaryContainer = AmethystPrimaryContainerDark,
    onPrimaryContainer = AmethystOnPrimaryContainerDark,
    secondary = AmethystSecondaryDark,
    onSecondary = AmethystOnSecondaryDark,
    secondaryContainer = AmethystSecondaryContainerDark,
    onSecondaryContainer = AmethystOnSecondaryContainerDark,
    tertiary = AmethystTertiaryDark,
    onTertiary = AmethystOnTertiaryDark,
    tertiaryContainer = AmethystTertiaryContainerDark,
    onTertiaryContainer = AmethystOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = AmethystSurfaceDark,
    onBackground = AmethystOnSurfaceDark,
    surface = AmethystSurfaceDark,
    onSurface = AmethystOnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = AmethystSurfaceContainerLowDark,
    surfaceContainer = AmethystSurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

// ── 6. Crimson & Burgundy
private val CrimsonLightColorScheme = lightColorScheme(
    primary = CrimsonPrimaryLight,
    onPrimary = CrimsonOnPrimaryLight,
    primaryContainer = CrimsonPrimaryContainerLight,
    onPrimaryContainer = CrimsonOnPrimaryContainerLight,
    secondary = CrimsonSecondaryLight,
    onSecondary = CrimsonOnSecondaryLight,
    secondaryContainer = CrimsonSecondaryContainerLight,
    onSecondaryContainer = CrimsonOnSecondaryContainerLight,
    tertiary = CrimsonTertiaryLight,
    onTertiary = CrimsonOnTertiaryLight,
    tertiaryContainer = CrimsonTertiaryContainerLight,
    onTertiaryContainer = CrimsonOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = CrimsonSurfaceLight,
    onBackground = CrimsonOnSurfaceLight,
    surface = CrimsonSurfaceLight,
    onSurface = CrimsonOnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = CrimsonSurfaceContainerLowLight,
    surfaceContainer = CrimsonSurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val CrimsonDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimaryDark,
    onPrimary = CrimsonOnPrimaryDark,
    primaryContainer = CrimsonPrimaryContainerDark,
    onPrimaryContainer = CrimsonOnPrimaryContainerDark,
    secondary = CrimsonSecondaryDark,
    onSecondary = CrimsonOnSecondaryDark,
    secondaryContainer = CrimsonSecondaryContainerDark,
    onSecondaryContainer = CrimsonOnSecondaryContainerDark,
    tertiary = CrimsonTertiaryDark,
    onTertiary = CrimsonOnTertiaryDark,
    tertiaryContainer = CrimsonTertiaryContainerDark,
    onTertiaryContainer = CrimsonOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = CrimsonSurfaceDark,
    onBackground = CrimsonOnSurfaceDark,
    surface = CrimsonSurfaceDark,
    onSurface = CrimsonOnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = CrimsonSurfaceContainerLowDark,
    surfaceContainer = CrimsonSurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

// ── 7. Midnight Ocean
private val MidnightLightColorScheme = lightColorScheme(
    primary = MidnightPrimaryLight,
    onPrimary = MidnightOnPrimaryLight,
    primaryContainer = MidnightPrimaryContainerLight,
    onPrimaryContainer = MidnightOnPrimaryContainerLight,
    secondary = MidnightSecondaryLight,
    onSecondary = MidnightOnSecondaryLight,
    secondaryContainer = MidnightSecondaryContainerLight,
    onSecondaryContainer = MidnightOnSecondaryContainerLight,
    tertiary = MidnightTertiaryLight,
    onTertiary = MidnightOnTertiaryLight,
    tertiaryContainer = MidnightTertiaryContainerLight,
    onTertiaryContainer = MidnightOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = MidnightSurfaceLight,
    onBackground = MidnightOnSurfaceLight,
    surface = MidnightSurfaceLight,
    onSurface = MidnightOnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = MidnightSurfaceContainerLowLight,
    surfaceContainer = MidnightSurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val MidnightDarkColorScheme = darkColorScheme(
    primary = MidnightPrimaryDark,
    onPrimary = MidnightOnPrimaryDark,
    primaryContainer = MidnightPrimaryContainerDark,
    onPrimaryContainer = MidnightOnPrimaryContainerDark,
    secondary = MidnightSecondaryDark,
    onSecondary = MidnightOnSecondaryDark,
    secondaryContainer = MidnightSecondaryContainerDark,
    onSecondaryContainer = MidnightOnSecondaryContainerDark,
    tertiary = MidnightTertiaryDark,
    onTertiary = MidnightOnTertiaryDark,
    tertiaryContainer = MidnightTertiaryContainerDark,
    onTertiaryContainer = MidnightOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = MidnightSurfaceDark,
    onBackground = MidnightOnSurfaceDark,
    surface = MidnightSurfaceDark,
    onSurface = MidnightOnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = MidnightSurfaceContainerLowDark,
    surfaceContainer = MidnightSurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

// ── 8. Forest & Sage Mint
private val ForestLightColorScheme = lightColorScheme(
    primary = ForestPrimaryLight,
    onPrimary = ForestOnPrimaryLight,
    primaryContainer = ForestPrimaryContainerLight,
    onPrimaryContainer = ForestOnPrimaryContainerLight,
    secondary = ForestSecondaryLight,
    onSecondary = ForestOnSecondaryLight,
    secondaryContainer = ForestSecondaryContainerLight,
    onSecondaryContainer = ForestOnSecondaryContainerLight,
    tertiary = ForestTertiaryLight,
    onTertiary = ForestOnTertiaryLight,
    tertiaryContainer = ForestTertiaryContainerLight,
    onTertiaryContainer = ForestOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = ForestSurfaceLight,
    onBackground = ForestOnSurfaceLight,
    surface = ForestSurfaceLight,
    onSurface = ForestOnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = ForestSurfaceContainerLowLight,
    surfaceContainer = ForestSurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val ForestDarkColorScheme = darkColorScheme(
    primary = ForestPrimaryDark,
    onPrimary = ForestOnPrimaryDark,
    primaryContainer = ForestPrimaryContainerDark,
    onPrimaryContainer = ForestOnPrimaryContainerDark,
    secondary = ForestSecondaryDark,
    onSecondary = ForestOnSecondaryDark,
    secondaryContainer = ForestSecondaryContainerDark,
    onSecondaryContainer = ForestOnSecondaryContainerDark,
    tertiary = ForestTertiaryDark,
    onTertiary = ForestOnTertiaryDark,
    tertiaryContainer = ForestTertiaryContainerDark,
    onTertiaryContainer = ForestOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = ForestSurfaceDark,
    onBackground = ForestOnSurfaceDark,
    surface = ForestSurfaceDark,
    onSurface = ForestOnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = ForestSurfaceContainerLowDark,
    surfaceContainer = ForestSurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

// ── 9. Rose Gold & Champagne
private val RoseGoldLightColorScheme = lightColorScheme(
    primary = RoseGoldPrimaryLight,
    onPrimary = RoseGoldOnPrimaryLight,
    primaryContainer = RoseGoldPrimaryContainerLight,
    onPrimaryContainer = RoseGoldOnPrimaryContainerLight,
    secondary = RoseGoldSecondaryLight,
    onSecondary = RoseGoldOnSecondaryLight,
    secondaryContainer = RoseGoldSecondaryContainerLight,
    onSecondaryContainer = RoseGoldOnSecondaryContainerLight,
    tertiary = RoseGoldTertiaryLight,
    onTertiary = RoseGoldOnTertiaryLight,
    tertiaryContainer = RoseGoldTertiaryContainerLight,
    onTertiaryContainer = RoseGoldOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = RoseGoldSurfaceLight,
    onBackground = RoseGoldOnSurfaceLight,
    surface = RoseGoldSurfaceLight,
    onSurface = RoseGoldOnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = RoseGoldSurfaceContainerLowLight,
    surfaceContainer = RoseGoldSurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val RoseGoldDarkColorScheme = darkColorScheme(
    primary = RoseGoldPrimaryDark,
    onPrimary = RoseGoldOnPrimaryDark,
    primaryContainer = RoseGoldPrimaryContainerDark,
    onPrimaryContainer = RoseGoldOnPrimaryContainerDark,
    secondary = RoseGoldSecondaryDark,
    onSecondary = RoseGoldOnSecondaryDark,
    secondaryContainer = RoseGoldSecondaryContainerDark,
    onSecondaryContainer = RoseGoldOnSecondaryContainerDark,
    tertiary = RoseGoldTertiaryDark,
    onTertiary = RoseGoldOnTertiaryDark,
    tertiaryContainer = RoseGoldTertiaryContainerDark,
    onTertiaryContainer = RoseGoldOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = RoseGoldSurfaceDark,
    onBackground = RoseGoldOnSurfaceDark,
    surface = RoseGoldSurfaceDark,
    onSurface = RoseGoldOnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = RoseGoldSurfaceContainerLowDark,
    surfaceContainer = RoseGoldSurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

// ── 10. Obsidian & Amber Gold
private val ObsidianLightColorScheme = lightColorScheme(
    primary = ObsidianPrimaryLight,
    onPrimary = ObsidianOnPrimaryLight,
    primaryContainer = ObsidianPrimaryContainerLight,
    onPrimaryContainer = ObsidianOnPrimaryContainerLight,
    secondary = ObsidianSecondaryLight,
    onSecondary = ObsidianOnSecondaryLight,
    secondaryContainer = ObsidianSecondaryContainerLight,
    onSecondaryContainer = ObsidianOnSecondaryContainerLight,
    tertiary = ObsidianTertiaryLight,
    onTertiary = ObsidianOnTertiaryLight,
    tertiaryContainer = ObsidianTertiaryContainerLight,
    onTertiaryContainer = ObsidianOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = ObsidianSurfaceLight,
    onBackground = ObsidianOnSurfaceLight,
    surface = ObsidianSurfaceLight,
    onSurface = ObsidianOnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = ObsidianSurfaceContainerLowLight,
    surfaceContainer = ObsidianSurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val ObsidianDarkColorScheme = darkColorScheme(
    primary = ObsidianPrimaryDark,
    onPrimary = ObsidianOnPrimaryDark,
    primaryContainer = ObsidianPrimaryContainerDark,
    onPrimaryContainer = ObsidianOnPrimaryContainerDark,
    secondary = ObsidianSecondaryDark,
    onSecondary = ObsidianOnSecondaryDark,
    secondaryContainer = ObsidianSecondaryContainerDark,
    onSecondaryContainer = ObsidianOnSecondaryContainerDark,
    tertiary = ObsidianTertiaryDark,
    onTertiary = ObsidianOnTertiaryDark,
    tertiaryContainer = ObsidianTertiaryContainerDark,
    onTertiaryContainer = ObsidianOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = ObsidianSurfaceDark,
    onBackground = ObsidianOnSurfaceDark,
    surface = ObsidianSurfaceDark,
    onSurface = ObsidianOnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = ObsidianSurfaceContainerLowDark,
    surfaceContainer = ObsidianSurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

// ── 11. Nordic Frost & Glacier
private val NordicLightColorScheme = lightColorScheme(
    primary = NordicPrimaryLight,
    onPrimary = NordicOnPrimaryLight,
    primaryContainer = NordicPrimaryContainerLight,
    onPrimaryContainer = NordicOnPrimaryContainerLight,
    secondary = NordicSecondaryLight,
    onSecondary = NordicOnSecondaryLight,
    secondaryContainer = NordicSecondaryContainerLight,
    onSecondaryContainer = NordicOnSecondaryContainerLight,
    tertiary = NordicTertiaryLight,
    onTertiary = NordicOnTertiaryLight,
    tertiaryContainer = NordicTertiaryContainerLight,
    onTertiaryContainer = NordicOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = NordicSurfaceLight,
    onBackground = NordicOnSurfaceLight,
    surface = NordicSurfaceLight,
    onSurface = NordicOnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = NordicSurfaceContainerLowLight,
    surfaceContainer = NordicSurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val NordicDarkColorScheme = darkColorScheme(
    primary = NordicPrimaryDark,
    onPrimary = NordicOnPrimaryDark,
    primaryContainer = NordicPrimaryContainerDark,
    onPrimaryContainer = NordicOnPrimaryContainerDark,
    secondary = NordicSecondaryDark,
    onSecondary = NordicOnSecondaryDark,
    secondaryContainer = NordicSecondaryContainerDark,
    onSecondaryContainer = NordicOnSecondaryContainerDark,
    tertiary = NordicTertiaryDark,
    onTertiary = NordicOnTertiaryDark,
    tertiaryContainer = NordicTertiaryContainerDark,
    onTertiaryContainer = NordicOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = NordicSurfaceDark,
    onBackground = NordicOnSurfaceDark,
    surface = NordicSurfaceDark,
    onSurface = NordicOnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = NordicSurfaceContainerLowDark,
    surfaceContainer = NordicSurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

// ── 12. Espresso & Warm Mocha
private val EspressoLightColorScheme = lightColorScheme(
    primary = EspressoPrimaryLight,
    onPrimary = EspressoOnPrimaryLight,
    primaryContainer = EspressoPrimaryContainerLight,
    onPrimaryContainer = EspressoOnPrimaryContainerLight,
    secondary = EspressoSecondaryLight,
    onSecondary = EspressoOnSecondaryLight,
    secondaryContainer = EspressoSecondaryContainerLight,
    onSecondaryContainer = EspressoOnSecondaryContainerLight,
    tertiary = EspressoTertiaryLight,
    onTertiary = EspressoOnTertiaryLight,
    tertiaryContainer = EspressoTertiaryContainerLight,
    onTertiaryContainer = EspressoOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = EspressoSurfaceLight,
    onBackground = EspressoOnSurfaceLight,
    surface = EspressoSurfaceLight,
    onSurface = EspressoOnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = EspressoSurfaceContainerLowLight,
    surfaceContainer = EspressoSurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val EspressoDarkColorScheme = darkColorScheme(
    primary = EspressoPrimaryDark,
    onPrimary = EspressoOnPrimaryDark,
    primaryContainer = EspressoPrimaryContainerDark,
    onPrimaryContainer = EspressoOnPrimaryContainerDark,
    secondary = EspressoSecondaryDark,
    onSecondary = EspressoOnSecondaryDark,
    secondaryContainer = EspressoSecondaryContainerDark,
    onSecondaryContainer = EspressoOnSecondaryContainerDark,
    tertiary = EspressoTertiaryDark,
    onTertiary = EspressoOnTertiaryDark,
    tertiaryContainer = EspressoTertiaryContainerDark,
    onTertiaryContainer = EspressoOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = EspressoSurfaceDark,
    onBackground = EspressoOnSurfaceDark,
    surface = EspressoSurfaceDark,
    onSurface = EspressoOnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = EspressoSurfaceContainerLowDark,
    surfaceContainer = EspressoSurfaceContainerDark,
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

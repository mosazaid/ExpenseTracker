package com.example.expensetracker

import androidx.compose.ui.graphics.toArgb
import com.example.expensetracker.data.preferences.ThemePalette
import com.example.expensetracker.presentation.theme.buildThemeColorScheme
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    @Test
    fun testAllPalettesHaveDistinctContainerAndOnContainerColors() {
        ThemePalette.entries.forEach { palette ->
            val lightScheme = buildThemeColorScheme(darkTheme = false, palette = palette)
            val darkScheme = buildThemeColorScheme(darkTheme = true, palette = palette)

            // Light scheme checks
            assertNotEquals(
                "Palette ${palette.name} Light primaryContainer should not equal onPrimaryContainer",
                lightScheme.primaryContainer,
                lightScheme.onPrimaryContainer
            )
            assertNotEquals(
                "Palette ${palette.name} Light secondaryContainer should not equal onSecondaryContainer",
                lightScheme.secondaryContainer,
                lightScheme.onSecondaryContainer
            )
            assertNotEquals(
                "Palette ${palette.name} Light tertiaryContainer should not equal onTertiaryContainer",
                lightScheme.tertiaryContainer,
                lightScheme.onTertiaryContainer
            )
            assertNotEquals(
                "Palette ${palette.name} Light surface should not equal onSurface",
                lightScheme.surface,
                lightScheme.onSurface
            )

            // Dark scheme checks
            assertNotEquals(
                "Palette ${palette.name} Dark primaryContainer should not equal onPrimaryContainer",
                darkScheme.primaryContainer,
                darkScheme.onPrimaryContainer
            )
            assertNotEquals(
                "Palette ${palette.name} Dark secondaryContainer should not equal onSecondaryContainer",
                darkScheme.secondaryContainer,
                darkScheme.onSecondaryContainer
            )
            assertNotEquals(
                "Palette ${palette.name} Dark tertiaryContainer should not equal onTertiaryContainer",
                darkScheme.tertiaryContainer,
                darkScheme.onTertiaryContainer
            )
            assertNotEquals(
                "Palette ${palette.name} Dark surface should not equal onSurface",
                darkScheme.surface,
                darkScheme.onSurface
            )
        }
    }
}

package com.example.expensetracker.data.preferences

import androidx.annotation.StringRes
import com.example.expensetracker.R

enum class ThemePalette(
    @StringRes val titleRes: Int,
    val primaryHex: Long,
    val secondaryHex: Long,
    val tertiaryHex: Long
) {
    EMERALD(
        titleRes = R.string.theme_emerald,
        primaryHex = 0xFF0F6D44,
        secondaryHex = 0xFF3B5F74,
        tertiaryHex = 0xFF865300
    ),
    BLUE_GOLD(
        titleRes = R.string.theme_blue_gold,
        primaryHex = 0xFF143D6A,
        secondaryHex = 0xFFC5A059,
        tertiaryHex = 0xFF8B4513
    ),
    WARM_EARTH(
        titleRes = R.string.theme_warm_earth,
        primaryHex = 0xFF8C3B1F,
        secondaryHex = 0xFF6D5E34,
        tertiaryHex = 0xFF855325
    ),
    SLATE_TEAL(
        titleRes = R.string.theme_slate_teal,
        primaryHex = 0xFF006876,
        secondaryHex = 0xFF486267,
        tertiaryHex = 0xFF006684
    ),
    AMETHYST_VIOLET(
        titleRes = R.string.theme_amethyst_violet,
        primaryHex = 0xFF6A1B9A,
        secondaryHex = 0xFF9C27B0,
        tertiaryHex = 0xFFFFB86B
    ),
    CRIMSON_BURGUNDY(
        titleRes = R.string.theme_crimson_burgundy,
        primaryHex = 0xFF9E1B32,
        secondaryHex = 0xFFC2185B,
        tertiaryHex = 0xFFFDB986
    ),
    MIDNIGHT_OCEAN(
        titleRes = R.string.theme_midnight_ocean,
        primaryHex = 0xFF0D47A1,
        secondaryHex = 0xFF00ACC1,
        tertiaryHex = 0xFFFFB59D
    ),
    FOREST_MINT(
        titleRes = R.string.theme_forest_mint,
        primaryHex = 0xFF1B5E20,
        secondaryHex = 0xFF4CAF50,
        tertiaryHex = 0xFFDFC74C
    )
}

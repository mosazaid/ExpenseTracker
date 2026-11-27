package com.mousa.mizani.presentation.feature.settings.appearance

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mousa.mizani.R
import com.mousa.mizani.data.preferences.ThemeMode
import com.mousa.mizani.data.preferences.ThemePalette
import com.mousa.mizani.presentation.common.AppTopBar
import com.mousa.mizani.presentation.feature.settings.SettingsViewModel
import com.mousa.mizani.presentation.feature.settings.components.SettingsChoiceRow
import com.mousa.mizani.presentation.feature.settings.components.SettingsGroupHeader

private val SwatchTileWidth = 104.dp

@Composable
fun AppearanceSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val themePalette by viewModel.themePalette.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_appearance),
                navController = navController,
                canNavigateBack = true
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            SettingsChoiceRow(
                title = stringResource(R.string.theme_mode_label),
                options = ThemeMode.entries,
                selected = themeMode,
                label = { stringResource(it.labelRes) },
                onSelect = viewModel::setThemeMode
            )
            SettingsGroupHeader(stringResource(R.string.theme_palette_label))
            PaletteSwatches(selected = themePalette, onSelect = viewModel::setThemePalette)
        }
    }
}

@get:StringRes
internal val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_mode_system
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteSwatches(selected: ThemePalette, onSelect: (ThemePalette) -> Unit) {
    // Tiles grow with the font size, so names wrap between words instead of inside them.
    val tileWidth = SwatchTileWidth * LocalDensity.current.fontScale.coerceAtLeast(1f)
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemePalette.entries.forEach { palette ->
            PaletteTile(
                palette = palette,
                isSelected = palette == selected,
                onClick = { onSelect(palette) },
                modifier = Modifier.width(tileWidth)
            )
        }
    }
}

@Composable
private fun PaletteTile(
    palette: ThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier,
        shape = shape,
        color = if (isSelected) colors.secondaryContainer else colors.surfaceContainerLow,
        contentColor = if (isSelected) colors.onSecondaryContainer else colors.onSurface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) colors.primary else colors.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
                .heightIn(min = 48.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PaletteSwatch(palette = palette, isSelected = isSelected)
            Text(
                text = stringResource(palette.titleRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PaletteSwatch(palette: ThemePalette, isSelected: Boolean) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Row(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        ) {
            listOf(palette.primaryHex, palette.secondaryHex, palette.tertiaryHex).forEach { hex ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .size(48.dp)
                        .background(Color(hex))
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(3.dp)
            )
        }
    }
}

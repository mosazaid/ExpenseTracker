package com.mousa.mizani.presentation.feature.settings.region

import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mousa.mizani.R
import com.mousa.mizani.core.format.CurrencyUtils
import com.mousa.mizani.core.locale.AppLanguage
import com.mousa.mizani.data.preferences.TimeFormatPreference
import com.mousa.mizani.presentation.common.AppTopBar
import com.mousa.mizani.presentation.feature.settings.SettingsViewModel
import com.mousa.mizani.presentation.feature.settings.components.SettingsChoiceRow
import kotlinx.coroutines.launch

@Composable
fun LanguageRegionSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val language by viewModel.language.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val timeFormat by viewModel.timeFormat.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val detectCurrency = rememberCurrencyLocationDetector { detection ->
        val message = when (detection) {
            is CurrencyDetection.Found -> {
                viewModel.setCurrencyCode(detection.currencyCode)
                context.getString(R.string.currency_detected, detection.currencyCode, detection.place)
            }
            CurrencyDetection.Unavailable ->
                context.getString(R.string.currency_location_unavailable)
        }
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_language_region),
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
                title = stringResource(R.string.language),
                options = AppLanguage.entries,
                selected = language,
                label = { stringResource(it.labelRes) },
                onSelect = {
                    // The navigation back stack is saved, so this page stays open.
                    viewModel.setLanguage(it)
                    activity?.recreate()
                }
            )
            CurrencySection(
                currencyCode = currencyCode,
                onSelect = viewModel::setCurrencyCode,
                onDetect = detectCurrency
            )
            SettingsChoiceRow(
                title = stringResource(R.string.time_format_title),
                options = TimeFormatPreference.entries,
                selected = timeFormat,
                label = { stringResource(it.labelRes) },
                onSelect = viewModel::setTimeFormat
            )
        }
    }
}

@get:StringRes
internal val AppLanguage.labelRes: Int
    get() = when (this) {
        AppLanguage.ENGLISH -> R.string.english
        AppLanguage.ARABIC -> R.string.arabic
    }

@get:StringRes
private val TimeFormatPreference.labelRes: Int
    get() = when (this) {
        TimeFormatPreference.H12 -> R.string.time_format_12h
        TimeFormatPreference.H24 -> R.string.time_format_24h
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencySection(
    currencyCode: String,
    onSelect: (String) -> Unit,
    onDetect: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val activeInfo = CurrencyUtils.getCurrencyInfo(currencyCode)

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        ExposedDropdownMenuBox(
            expanded = isMenuExpanded,
            onExpandedChange = { isMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = "${activeInfo.code} (${activeInfo.symbol} / ${activeInfo.symbolAr})",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.currency_settings_title)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMenuExpanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                CurrencyUtils.SUPPORTED_CURRENCIES.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text("${currency.code} — ${currency.symbol} (${currency.symbolAr})") },
                        onClick = {
                            onSelect(currency.code)
                            isMenuExpanded = false
                        }
                    )
                }
            }
        }
        TextButton(onClick = onDetect, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.currency_auto_detect))
        }
    }
}

package com.mousa.mizani.presentation.feature.settings.month

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mousa.mizani.R
import com.mousa.mizani.data.preferences.MonthMode
import com.mousa.mizani.presentation.common.AppTopBar
import com.mousa.mizani.presentation.feature.settings.SettingsViewModel
import com.mousa.mizani.presentation.feature.settings.components.SettingsChoiceRow
import com.mousa.mizani.presentation.feature.settings.components.SettingsNavRow
import com.mousa.mizani.presentation.feature.settings.components.SettingsSwitchRow
import com.mousa.mizani.presentation.navigation.CalculationSettings

@Composable
fun MonthCalculationSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val monthMode by viewModel.monthMode.collectAsState()
    val isVaultTransferEnabled by viewModel.enableDepositWalletTransfer.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_month_calculations),
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
                title = stringResource(R.string.month_mode_label),
                options = MonthMode.entries,
                selected = monthMode,
                label = { stringResource(it.labelRes) },
                onSelect = viewModel::setMonthMode
            )
            Text(
                text = stringResource(R.string.month_mode_scope_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            HorizontalDivider()
            SettingsNavRow(
                title = stringResource(R.string.calculation_rules_title),
                summary = stringResource(R.string.calculation_rules_entry_desc),
                icon = Icons.Outlined.Calculate,
                onClick = { navController.navigate(CalculationSettings) }
            )
            HorizontalDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.deposit_vault_transfers_config),
                summary = stringResource(R.string.deposit_vault_transfers_config_desc),
                checked = isVaultTransferEnabled,
                onCheckedChange = viewModel::setEnableDepositWalletTransfer
            )
        }
    }
}

@get:StringRes
internal val MonthMode.labelRes: Int
    get() = when (this) {
        MonthMode.SALARY -> R.string.salary_month
        MonthMode.CALENDAR -> R.string.calendar_month
    }

package com.mousa.mizani.presentation.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mousa.mizani.BuildConfig
import com.mousa.mizani.R
import com.mousa.mizani.core.debug.isDatabaseViewerVisible
import com.mousa.mizani.presentation.common.AppTopBar
import com.mousa.mizani.presentation.feature.settings.appearance.labelRes
import com.mousa.mizani.presentation.feature.settings.backup.lastBackupLabel
import com.mousa.mizani.presentation.feature.settings.components.SettingsGroupHeader
import com.mousa.mizani.presentation.feature.settings.components.SettingsNavRow
import com.mousa.mizani.presentation.feature.settings.components.SettingsSwitchRow
import com.mousa.mizani.presentation.feature.settings.month.labelRes
import com.mousa.mizani.presentation.feature.settings.region.labelRes
import com.mousa.mizani.presentation.navigation.AccountBalancesSettings
import com.mousa.mizani.presentation.navigation.AppearanceSettings
import com.mousa.mizani.presentation.navigation.BackupSettings
import com.mousa.mizani.presentation.navigation.DatabaseBrowser
import com.mousa.mizani.presentation.navigation.ImportExportSettings
import com.mousa.mizani.presentation.navigation.LanguageRegionSettings
import com.mousa.mizani.presentation.navigation.MonthCalculationSettings
import com.mousa.mizani.presentation.navigation.NotificationSettings

private val VersionRowMinHeight = 48.dp

/** The Settings list (spec 3.1): grouped rows that open the settings pages. */
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsIndexViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val themePalette by viewModel.themePalette.collectAsState()
    val language by viewModel.language.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val monthMode by viewModel.monthMode.collectAsState()
    val lastBackupCreatedAt by viewModel.lastBackupCreatedAt.collectAsState()
    val isAppLockEnabled by viewModel.appLockEnabled.collectAsState()
    val isViewerUnlocked by viewModel.databaseViewerUnlocked.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(context.getString(it)) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings),
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
                .padding(bottom = 16.dp)
        ) {
            SettingsGroupHeader(stringResource(R.string.settings_group_general))
            SettingsNavRow(
                title = stringResource(R.string.settings_appearance),
                summary = twoParts(stringResource(themeMode.labelRes), stringResource(themePalette.titleRes)),
                icon = Icons.Outlined.Palette,
                onClick = { navController.navigate(AppearanceSettings) }
            )
            SettingsNavRow(
                title = stringResource(R.string.settings_language_region),
                summary = currencyCode?.let { twoParts(stringResource(language.labelRes), it) }
                    ?: stringResource(language.labelRes),
                icon = Icons.Outlined.Language,
                onClick = { navController.navigate(LanguageRegionSettings) }
            )

            HorizontalDivider()
            SettingsGroupHeader(stringResource(R.string.settings_group_money))
            SettingsNavRow(
                title = stringResource(R.string.settings_accounts_balances),
                summary = null,
                icon = Icons.Outlined.AccountBalanceWallet,
                onClick = { navController.navigate(AccountBalancesSettings) }
            )
            SettingsNavRow(
                title = stringResource(R.string.settings_month_calculations),
                summary = stringResource(monthMode.labelRes),
                icon = Icons.Outlined.CalendarMonth,
                onClick = { navController.navigate(MonthCalculationSettings) }
            )

            HorizontalDivider()
            SettingsGroupHeader(stringResource(R.string.settings_group_alerts))
            SettingsNavRow(
                title = stringResource(R.string.settings_notifications),
                summary = null,
                icon = Icons.Outlined.Notifications,
                onClick = { navController.navigate(NotificationSettings) }
            )

            HorizontalDivider()
            SettingsGroupHeader(stringResource(R.string.settings_group_data_security))
            SettingsSwitchRow(
                title = stringResource(R.string.biometric_lock),
                summary = stringResource(R.string.app_lock_summary),
                checked = isAppLockEnabled,
                onCheckedChange = viewModel::setAppLockEnabled
            )
            SettingsNavRow(
                title = stringResource(R.string.settings_backup_restore),
                summary = lastBackupLabel(lastBackupCreatedAt),
                icon = Icons.Outlined.Backup,
                onClick = { navController.navigate(BackupSettings) }
            )
            SettingsNavRow(
                title = stringResource(R.string.settings_import_export),
                summary = null,
                icon = Icons.Outlined.ImportExport,
                onClick = { navController.navigate(ImportExportSettings) }
            )

            HorizontalDivider()
            if (isDatabaseViewerVisible(BuildConfig.DEBUG, isViewerUnlocked)) {
                SettingsNavRow(
                    title = stringResource(R.string.database_browser),
                    summary = stringResource(R.string.database_browser_desc),
                    icon = Icons.Outlined.Storage,
                    onClick = { navController.navigate(DatabaseBrowser) }
                )
            }
            VersionRow(onClick = { viewModel.onVersionTapped(BuildConfig.DEBUG) })
        }
    }
}

@Composable
private fun twoParts(first: String, second: String): String =
    stringResource(R.string.settings_summary_two_parts, first, second)

@Composable
private fun VersionRow(onClick: () -> Unit) {
    Text(
        text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = VersionRowMinHeight)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

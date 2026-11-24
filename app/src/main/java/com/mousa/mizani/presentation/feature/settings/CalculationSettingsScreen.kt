package com.mousa.mizani.presentation.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mousa.mizani.R
import com.mousa.mizani.data.preferences.CalculationPreferences
import com.mousa.mizani.presentation.common.AppTopBar

@Composable
fun CalculationSettingsScreen(
    navController: NavController,
    viewModel: CalculationSettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.calculationPreferences.collectAsState()
    val isDefault = prefs == CalculationPreferences()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.calculation_rules_title),
                navController = navController,
                actions = {
                    if (!isDefault) {
                        TextButton(onClick = { viewModel.resetToDefaults() }) {
                            Icon(
                                imageVector = Icons.Outlined.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.calculation_rules_reset))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.calculation_rules_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Section 1: Income Calculations
            CalculationSectionCard(
                title = stringResource(R.string.calculation_section_income),
                icon = Icons.AutoMirrored.Outlined.TrendingUp
            ) {
                CalculationCheckboxRow(
                    title = stringResource(R.string.calc_include_deposit_income_title),
                    subtitle = stringResource(R.string.calc_include_deposit_income_desc),
                    checked = prefs.includeDepositIncome,
                    onCheckedChange = viewModel::setIncludeDepositIncome
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                CalculationCheckboxRow(
                    title = stringResource(R.string.calc_deduct_loans_income_title),
                    subtitle = stringResource(R.string.calc_deduct_loans_income_desc),
                    checked = prefs.deductLoansFromIncome,
                    onCheckedChange = viewModel::setDeductLoansFromIncome
                )
            }

            // Section 2: Expense Calculations
            CalculationSectionCard(
                title = stringResource(R.string.calculation_section_expense),
                icon = Icons.AutoMirrored.Outlined.TrendingDown
            ) {
                CalculationCheckboxRow(
                    title = stringResource(R.string.calc_include_wallet_expense_title),
                    subtitle = stringResource(R.string.calc_include_wallet_expense_desc),
                    checked = prefs.includeWalletExpenses,
                    onCheckedChange = viewModel::setIncludeWalletExpenses
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                CalculationCheckboxRow(
                    title = stringResource(R.string.calc_include_deposit_expense_title),
                    subtitle = stringResource(R.string.calc_include_deposit_expense_desc),
                    checked = prefs.includeDepositExpenses,
                    onCheckedChange = viewModel::setIncludeDepositExpenses
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                CalculationCheckboxRow(
                    title = stringResource(R.string.calc_include_loan_expense_title),
                    subtitle = stringResource(R.string.calc_include_loan_expense_desc),
                    checked = prefs.includeLoanLinkedExpenses,
                    onCheckedChange = viewModel::setIncludeLoanLinkedExpenses
                )
            }

            // Section 3: Smart Financial Insights
            CalculationSectionCard(
                title = stringResource(R.string.calculation_section_insights),
                icon = Icons.Outlined.Insights
            ) {
                CalculationCheckboxRow(
                    title = stringResource(R.string.calc_insights_carried_forward_title),
                    subtitle = stringResource(R.string.calc_insights_carried_forward_desc),
                    checked = prefs.includeCarriedForwardInInsights,
                    onCheckedChange = viewModel::setIncludeCarriedForwardInInsights
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                CalculationCheckboxRow(
                    title = stringResource(R.string.calc_insights_liquid_floor_title),
                    subtitle = stringResource(R.string.calc_insights_liquid_floor_desc),
                    checked = prefs.useAvailableBalanceFloorInInsights,
                    onCheckedChange = viewModel::setUseAvailableBalanceFloorInInsights
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                CalculationCheckboxRow(
                    title = stringResource(R.string.calc_insights_reserve_wallet_title),
                    subtitle = stringResource(R.string.calc_insights_reserve_wallet_desc),
                    checked = prefs.reserveWalletInInsights,
                    onCheckedChange = viewModel::setReserveWalletInInsights
                )
            }

            // Section 4: Statistics Calculations
            CalculationSectionCard(
                title = stringResource(R.string.calculation_section_statistics),
                icon = Icons.Outlined.BarChart
            ) {
                CalculationCheckboxRow(
                    title = stringResource(R.string.calc_stats_apply_global_title),
                    subtitle = stringResource(R.string.calc_stats_apply_global_desc),
                    checked = prefs.statsApplyGlobalIncomeExpenseRules,
                    onCheckedChange = viewModel::setStatsApplyGlobalRules
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                CalculationCheckboxRow(
                    title = stringResource(R.string.calc_stats_include_excluded_title),
                    subtitle = stringResource(R.string.calc_stats_include_excluded_desc),
                    checked = prefs.statsIncludeManuallyExcluded,
                    onCheckedChange = viewModel::setStatsIncludeManuallyExcluded
                )
            }

            // Section 5: Other Financial Tools (Runway & Zakah)
            CalculationSectionCard(
                title = stringResource(R.string.calculation_section_tools),
                icon = Icons.Outlined.Calculate
            ) {
                CalculationCheckboxRow(
                    title = stringResource(R.string.calc_runway_include_vault_title),
                    subtitle = stringResource(R.string.calc_runway_include_vault_desc),
                    checked = prefs.runwayIncludeVaultBalance,
                    onCheckedChange = viewModel::setRunwayIncludeVault
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                CalculationCheckboxRow(
                    title = stringResource(R.string.calc_runway_exclude_wallet_deposit_title),
                    subtitle = stringResource(R.string.calc_runway_exclude_wallet_deposit_desc),
                    checked = prefs.runwayExcludeWalletAndDepositBurn,
                    onCheckedChange = viewModel::setRunwayExcludeWalletDeposit
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                CalculationCheckboxRow(
                    title = stringResource(R.string.calc_zakah_include_vault_title),
                    subtitle = stringResource(R.string.calc_zakah_include_vault_desc),
                    checked = prefs.zakahIncludeVaultOnAutoSync,
                    onCheckedChange = viewModel::setZakahIncludeVault
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CalculationSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun CalculationCheckboxRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(role = Role.Checkbox) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = null // handled by Row's clickable for larger 48dp+ touch target
        )
    }
}

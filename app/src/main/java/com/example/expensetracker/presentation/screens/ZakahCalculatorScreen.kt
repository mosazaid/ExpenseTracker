package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.domain.ZakahCalculator
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.navigation.AddTransaction
import com.example.expensetracker.presentation.theme.FinanceNegative
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.withTabularNums
import com.example.expensetracker.presentation.viewModel.OverviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakahCalculatorScreen(
    navController: NavController,
    overviewViewModel: OverviewViewModel = hiltViewModel()
) {
    val monthSummary by overviewViewModel.monthSummary.collectAsState()
    val cash = monthSummary.currentBalances.cash ?: 0.0
    val bank = monthSummary.currentBalances.bank ?: 0.0

    var goldPriceStr by remember { mutableStateOf("55.0") }
    var otherAssetsStr by remember { mutableStateOf("0.0") }
    var debtsOwedStr by remember { mutableStateOf("0.0") }

    val otherAssets = otherAssetsStr.toDoubleOrNull() ?: 0.0
    val debtsOwed = debtsOwedStr.toDoubleOrNull() ?: 0.0
    val goldPrice = goldPriceStr.toDoubleOrNull() ?: ZakahCalculator.DEFAULT_GOLD_PRICE_PER_GRAM

    val assessment = remember(cash, bank, otherAssets, debtsOwed, goldPrice) {
        ZakahCalculator.calculateZakah(
            cashBalance = cash,
            bankBalance = bank,
            otherAssets = otherAssets,
            immediateDebts = debtsOwed,
            goldPricePerGram = goldPrice
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.zakah_calculator_title),
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Result Hero Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (assessment.isNisabReached) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(
                    1.dp,
                    if (assessment.isNisabReached) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (assessment.isNisabReached) FinancePositive.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (assessment.isNisabReached) Icons.Outlined.CheckCircle else Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = if (assessment.isNisabReached) FinancePositive else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Text(
                        text = if (assessment.isNisabReached) stringResource(R.string.zakah_due_label)
                        else stringResource(R.string.zakah_below_nisab_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = CurrencyUtils.formatCurrency(assessment.zakahDue),
                        style = MaterialTheme.typography.headlineLarge.withTabularNums(),
                        fontWeight = FontWeight.ExtraBold,
                        color = if (assessment.isNisabReached) FinancePositive else MaterialTheme.colorScheme.outline
                    )

                    Text(
                        text = stringResource(
                            R.string.zakah_nisab_threshold_info,
                            CurrencyUtils.formatCurrency(assessment.nisabThreshold)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (assessment.isNisabReached) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                navController.navigate(
                                    AddTransaction()
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VolunteerActivism,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.record_zakah_payment_btn))
                        }
                    }
                }
            }

            // Breakdown & Inputs Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.zakah_breakdown_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Current Cash & Bank (Read only loaded from app)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Cash & Bank", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Tracked automatically by app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            text = CurrencyUtils.formatCurrency(cash + bank),
                            style = MaterialTheme.typography.bodyLarge.withTabularNums(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Other zakatable assets
                    OutlinedTextField(
                        value = otherAssetsStr,
                        onValueChange = { otherAssetsStr = CurrencyUtils.cleanDecimalInput(it) },
                        label = { Text("Other Zakatable Assets (Gold, Trade Goods)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Short-term liabilities / debts
                    OutlinedTextField(
                        value = debtsOwedStr,
                        onValueChange = { debtsOwedStr = CurrencyUtils.cleanDecimalInput(it) },
                        label = { Text("Immediate Short-term Debts (Deducted)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Gold price per gram
                    OutlinedTextField(
                        value = goldPriceStr,
                        onValueChange = { goldPriceStr = CurrencyUtils.cleanDecimalInput(it) },
                        label = { Text("Gold Price per Gram (85g Nisab)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Net Zakatable Wealth", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = CurrencyUtils.formatCurrency(assessment.netZakatableWealth),
                                style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

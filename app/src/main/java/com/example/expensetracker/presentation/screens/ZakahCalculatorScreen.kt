package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
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
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.navigation.AddTransaction
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.withTabularNums
import com.example.expensetracker.presentation.viewModel.ZakahViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakahCalculatorScreen(
    navController: NavController,
    viewModel: ZakahViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val assessment = state.assessment

    var goldPriceInput by remember { mutableStateOf("55.0") }
    var otherAssetsInput by remember { mutableStateOf("0.0") }
    var debtsInput by remember { mutableStateOf("0.0") }

    var goldGramsInput by remember { mutableStateOf("") }
    var selectedKarat by remember { mutableIntStateOf(21) }
    var showGoldCalculator by remember { mutableStateOf(false) }

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
            if (assessment != null) {
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
                                onClick = { navController.navigate(AddTransaction()) },
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
            }

            // Hawl Lunar Tracker Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Hawl Lunar Tracker (حول الزكاة)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "${state.hawlDaysRemaining} days to maturity",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (state.hawlDaysPassed / 354f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Color(0xFF10B981)
                    )

                    Text(
                        text = "1 Lunar Year = 354 days. Zakah becomes obligatorily due once net wealth remains at or above Nisab for a full lunar year.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Breakdown & Inputs Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.zakah_breakdown_header),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.syncBalances() }) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Cash & Bank
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Liquid Cash & Bank", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Synced live from accounts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            text = CurrencyUtils.formatCurrency(state.cashBalance + state.bankBalance),
                            style = MaterialTheme.typography.bodyLarge.withTabularNums(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Deposit Vault Toggle
                    if (state.vaultBalance > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Deposit Vault Capital", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Savings/capital reserves in vault", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = CurrencyUtils.formatCurrency(state.vaultBalance),
                                    style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Checkbox(
                                    checked = state.includeVault,
                                    onCheckedChange = { viewModel.setIncludeVault(it) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Gold Weight & Karat Calculator toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showGoldCalculator = !showGoldCalculator },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gold Register Calculator",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (showGoldCalculator) "▲ Hide" else "▼ Expand",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (showGoldCalculator) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = goldGramsInput,
                                    onValueChange = {
                                        goldGramsInput = CurrencyUtils.cleanDecimalInput(it)
                                        viewModel.setGoldCalculator(goldGramsInput.toDoubleOrNull() ?: 0.0, selectedKarat)
                                    },
                                    label = { Text("Weight (Grams)") },
                                    placeholder = { Text("e.g. 50") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                // Karat selector
                                Column {
                                    Text("Karat", style = MaterialTheme.typography.labelSmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(18, 21, 24).forEach { k ->
                                            FilterChip(
                                                selected = selectedKarat == k,
                                                onClick = {
                                                    selectedKarat = k
                                                    viewModel.setGoldCalculator(goldGramsInput.toDoubleOrNull() ?: 0.0, k)
                                                },
                                                label = { Text("${k}K") }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Other assets
                    OutlinedTextField(
                        value = otherAssetsInput,
                        onValueChange = {
                            otherAssetsInput = CurrencyUtils.cleanDecimalInput(it)
                            viewModel.setOtherAssets(otherAssetsInput.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("Other Assets (Trade Goods, Investments)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Immediate Debts
                    OutlinedTextField(
                        value = debtsInput,
                        onValueChange = {
                            debtsInput = CurrencyUtils.cleanDecimalInput(it)
                            viewModel.setImmediateDebts(debtsInput.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("Immediate Short-term Debts (Deducted)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Gold price per gram
                    OutlinedTextField(
                        value = goldPriceInput,
                        onValueChange = {
                            goldPriceInput = CurrencyUtils.cleanDecimalInput(it)
                            viewModel.setGoldPricePerGram(goldPriceInput.toDoubleOrNull() ?: 55.0)
                        },
                        label = { Text("Gold Price per Gram (85g Nisab)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (assessment != null) {
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
}

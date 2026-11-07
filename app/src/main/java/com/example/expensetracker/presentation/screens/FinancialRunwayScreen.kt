package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.domain.currency.CurrencyRates
import com.example.expensetracker.domain.runway.RunwayVulnerability
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.viewModel.FinancialRunwayViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialRunwayScreen(
    navController: NavController,
    viewModel: FinancialRunwayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val runway = state.runwayResult

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Emergency Runway Stress-Tester",
                navController = navController,
                canNavigateBack = true
            )
        }
    ) { padding ->
        if (state.isLoading || runway == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val (statusText, statusColor) = when (runway.vulnerability) {
                RunwayVulnerability.CRITICAL -> "CRITICAL VULNERABILITY" to Color(0xFFEF4444)
                RunwayVulnerability.VULNERABLE -> "FRAGILE BUFFER" to Color(0xFFF59E0B)
                RunwayVulnerability.STABLE -> "STABLE RUNWAY" to Color(0xFF0284C7)
                RunwayVulnerability.FORTIFIED -> "ROCK-SOLID CUSHION" to Color(0xFF10B981)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Runway Gauge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.12f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(statusColor.copy(alpha = 0.35f))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "%.1f Months", runway.runwayMonths),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )

                        Text(
                            text = "You can survive without income for ~${runway.daysOfSurvival} days based on your actual 90-day essential burn rate.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                // Financial Fundamentals Grid
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Runway Fundamentals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Liquid Wealth", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = CurrencyRates.formatAmount(runway.liquidWealth, "JOD"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Monthly Essential Burn", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = CurrencyRates.formatAmount(runway.monthlyEssentialBurn, "JOD") + " / mo",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recommended 3-Mo Cushion", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = CurrencyRates.formatAmount(runway.recommendedSafetyBuffer, "JOD"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                // Interactive Stress-Testing Simulator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Emergency Shock Simulator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Test your financial resilience against a sudden emergency expense (e.g. car breakdown, dental surgery, urgent travel):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Simulated Emergency Expense: " + CurrencyRates.formatAmount(state.emergencySimulatedExpense, "JOD"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )

                        Slider(
                            value = state.emergencySimulatedExpense.toFloat(),
                            onValueChange = { viewModel.setSimulatedEmergency(it.toDouble()) },
                            valueRange = 0f..1500f,
                            steps = 14
                        )

                        state.simulatedRunwayMonths?.let { simMonths ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Post-Shock Runway:", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = String.format(Locale.US, "%.1f Months", simMonths),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (simMonths < 1.0) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


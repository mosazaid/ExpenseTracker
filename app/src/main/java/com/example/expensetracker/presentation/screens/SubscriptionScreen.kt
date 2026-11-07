package com.example.expensetracker.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.domain.monetization.PlanTier
import com.example.expensetracker.domain.monetization.ProTrialManager
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.withTabularNums
import com.example.expensetracker.presentation.viewModel.SubscriptionViewModel

@Composable
fun SubscriptionScreen(
    navController: NavController,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val status = uiState.status
    val snackbarHostState = remember { SnackbarHostState() }
    var ownerTapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "ExpenseTracker Pro",
                navController = navController,
                canNavigateBack = true
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Header with secret 5-tap Owner Bypass
            Surface(
                shape = CircleShape,
                color = if (status.isOwnerBypass) FinancePositive.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime > 1500L) {
                            ownerTapCount = 1
                        } else {
                            ownerTapCount++
                        }
                        lastTapTime = now
                        if (ownerTapCount >= 5) {
                            ownerTapCount = 0
                            viewModel.toggleOwnerBypass()
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (status.isOwnerBypass) Icons.Outlined.Verified else Icons.Outlined.WorkspacePremium,
                        contentDescription = "Owner Bypass Trigger (tap 5x)",
                        tint = if (status.isOwnerBypass) FinancePositive else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            if (status.isOwnerBypass) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = FinancePositive.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, FinancePositive.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = FinancePositive, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Owner Lifetime Access Active", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = FinancePositive)
                            Text("All Pro+ features unlocked permanently. Tap crown icon 5 times to toggle.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Upgrade to ExpenseTracker Pro",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Unlock Islamic financial tools, multi-currency travel mode, and protected vaults.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // 90-Day Trial Status Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (status.isSubscribed) FinancePositive.copy(alpha = 0.12f)
                else if (status.isTrialActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                border = BorderStroke(
                    1.dp,
                    if (status.isSubscribed) FinancePositive.copy(alpha = 0.4f)
                    else if (status.isTrialActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            Icon(
                                imageVector = if (status.isSubscribed) Icons.Filled.CheckCircle
                                else if (status.isTrialActive) Icons.Outlined.HourglassTop
                                else Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = if (status.isSubscribed) FinancePositive
                                else if (status.isTrialActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (status.isSubscribed) "Active Subscription"
                                else if (status.isTrialActive) "3-Month Free Device Trial"
                                else "Free Trial Expired",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (status.isTrialActive) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = FinancePositive.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${status.trialDaysRemaining} days left",
                                    style = MaterialTheme.typography.labelMedium.withTabularNums(),
                                    fontWeight = FontWeight.Bold,
                                    color = FinancePositive,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (status.isTrialActive) {
                        Text(
                            text = "Every feature in Pro+ is 100% free and unlocked on this device for 90 days. No account or credit card required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { status.trialPercentageElapsed },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    } else if (status.isSubscribed) {
                        Text(
                            text = "Thank you for supporting ExpenseTracker! All current and future Pro features are permanently enabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Your 90-day device trial has concluded. Subscribe below to restore access to your Vault, Multi-Currency, and advanced insights.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Billing Cycle Selector (Monthly vs Annual)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (uiState.isAnnualBilling) MaterialTheme.colorScheme.surface
                        else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectBillingCycle(true) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Annual (Save 30%)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (uiState.isAnnualBilling) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.isAnnualBilling) FinancePositive else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!uiState.isAnnualBilling) MaterialTheme.colorScheme.surface
                        else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectBillingCycle(false) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (!uiState.isAnnualBilling) FontWeight.Bold else FontWeight.Medium,
                                color = if (!uiState.isAnnualBilling) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Plans Selector Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlanCard(
                    tier = PlanTier.PRO,
                    priceText = if (uiState.isAnnualBilling) "$24.99/yr" else "$2.99/mo",
                    isSelected = uiState.selectedTier == PlanTier.PRO,
                    badgeText = "Popular",
                    onSelect = { viewModel.selectTier(PlanTier.PRO) },
                    modifier = Modifier.weight(1f)
                )

                PlanCard(
                    tier = PlanTier.PRO_PLUS,
                    priceText = if (uiState.isAnnualBilling) "$39.99/yr" else "$4.99/mo",
                    isSelected = uiState.selectedTier == PlanTier.PRO_PLUS,
                    badgeText = "Full Suite",
                    onSelect = { viewModel.selectTier(PlanTier.PRO_PLUS) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Features Breakdown Checklist
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "What's Included in ${uiState.selectedTier.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val features = if (uiState.selectedTier == PlanTier.PRO) {
                        listOf(
                            "Unlimited Categories & Subcategories",
                            "Biometrically Protected Deposit Vault",
                            "Multi-Split Items & Receipt Attachment",
                            "Day-Normalized Spending Pacing Engine",
                            "Detailed Subcategory Comparison Tree",
                            "Executive 1-Page PDF & Excel Reports"
                        )
                    } else {
                        listOf(
                            "Everything in Pro Tier",
                            "Multi-Currency & Travel Trip Pacing",
                            "Smart Inflation Memory & Split Prices",
                            "Financial Runway Stress-Tester",
                            "Murabaha Islamic Loan Simulator",
                            "Virtual Sadaqah Jar & Friday Reminders",
                            "Auto-Sync Zakah & 354-Day Hawl Tracker"
                        )
                    }

                    features.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = FinancePositive,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Subscribe Button
            Button(
                onClick = { viewModel.subscribe(uiState.selectedTier) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Stars,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (status.isSubscribed) "Switch to ${uiState.selectedTier.displayName}"
                    else if (status.isTrialActive) "Subscribe Now • Keep Access After Trial"
                    else "Subscribe to ${uiState.selectedTier.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Restore Purchases & Disclaimers
            TextButton(
                onClick = { viewModel.restorePurchases() },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Restore Purchases",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Subscriptions auto-renew unless cancelled at least 24 hours before the end of the current period. Manage or cancel subscriptions in your Google Play Store account settings.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PlanCard(
    tier: PlanTier,
    priceText: String,
    isSelected: Boolean,
    badgeText: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            2.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Text(
                text = tier.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = priceText,
                style = MaterialTheme.typography.bodyLarge.withTabularNums(),
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


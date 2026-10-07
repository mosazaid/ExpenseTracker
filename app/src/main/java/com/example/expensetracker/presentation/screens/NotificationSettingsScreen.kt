package com.example.expensetracker.presentation.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import android.app.Activity
import androidx.core.app.ActivityCompat
import com.example.expensetracker.R
import com.example.expensetracker.core.permission.PermissionHelper
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.components.PermissionRationaleDialog
import com.example.expensetracker.presentation.components.PermissionType
import com.example.expensetracker.presentation.navigation.Alerts
import com.example.expensetracker.presentation.viewModel.NotificationSettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    navController: NavController,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val loanAlerts by viewModel.loanAlertsEnabled.collectAsState()
    val debtAlerts by viewModel.debtAlertsEnabled.collectAsState()
    val salaryAlerts by viewModel.salaryAlertsEnabled.collectAsState()
    val budgetAlerts by viewModel.budgetAlertsEnabled.collectAsState()
    val dailyReminder by viewModel.dailyReminderEnabled.collectAsState()

    var systemNotificationsEnabled by remember {
        mutableStateOf(viewModel.areSystemNotificationsEnabled())
    }
    var showNotificationRationaleDialog by remember { mutableStateOf(false) }
    var isPermanentlyDeniedNotification by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        systemNotificationsEnabled = isGranted || viewModel.areSystemNotificationsEnabled()
        if (!isGranted) {
            val isPermanently = (context as? Activity)?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
            } ?: false
            isPermanentlyDeniedNotification = isPermanently
            showNotificationRationaleDialog = true
        }
    }

    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shouldShowRationale = (context as? Activity)?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
            } ?: false
            if (shouldShowRationale) {
                isPermanentlyDeniedNotification = false
                showNotificationRationaleDialog = true
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            PermissionHelper.openNotificationSettings(context)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.notification_settings_title),
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
            // System Notification Status Banner (if disabled)
            if (!systemNotificationsEnabled) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.notifications_system_disabled_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = stringResource(R.string.notifications_system_disabled_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = requestNotificationPermission,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(stringResource(R.string.enable_system_notifications))
                            }
                        }
                    }
                }
            }

            // Inbox CTA Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Inbox,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.active_alerts_inbox),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.view_active_alerts_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    IconButton(onClick = { navController.navigate(Alerts) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = stringResource(R.string.view_all_history),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.alert_categories_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 1. Loan Reminders
            AlertPreferenceRow(
                icon = Icons.Outlined.AccountBalance,
                iconColor = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.alert_loan_title),
                description = stringResource(R.string.alert_loan_desc),
                checked = loanAlerts,
                onCheckedChange = { viewModel.setLoanAlerts(it) }
            )

            // 2. Debt Reminders
            AlertPreferenceRow(
                icon = Icons.Outlined.Handshake,
                iconColor = MaterialTheme.colorScheme.secondary,
                title = stringResource(R.string.alert_debt_title),
                description = stringResource(R.string.alert_debt_desc),
                checked = debtAlerts,
                onCheckedChange = { viewModel.setDebtAlerts(it) }
            )

            // 3. Salary & Recurring Reminders
            AlertPreferenceRow(
                icon = Icons.Outlined.Payments,
                iconColor = Color(0xFF10B981),
                title = stringResource(R.string.alert_salary_title),
                description = stringResource(R.string.alert_salary_desc),
                checked = salaryAlerts,
                onCheckedChange = { viewModel.setSalaryAlerts(it) }
            )

            // 4. Budget Overspend & Warnings
            AlertPreferenceRow(
                icon = Icons.Outlined.PieChart,
                iconColor = Color(0xFFF59E0B),
                title = stringResource(R.string.alert_budget_title),
                description = stringResource(R.string.alert_budget_desc),
                checked = budgetAlerts,
                onCheckedChange = { viewModel.setBudgetAlerts(it) }
            )

            // 5. Daily Expense Logging Reminder
            AlertPreferenceRow(
                icon = Icons.Outlined.Alarm,
                iconColor = Color(0xFF8B5CF6),
                title = stringResource(R.string.alert_daily_title),
                description = stringResource(R.string.alert_daily_desc),
                checked = dailyReminder,
                onCheckedChange = { viewModel.setDailyReminder(it) }
            )
        }
    }

    if (showNotificationRationaleDialog) {
        PermissionRationaleDialog(
            permissionType = PermissionType.NOTIFICATIONS,
            isPermanentlyDenied = isPermanentlyDeniedNotification,
            onConfirm = {
                showNotificationRationaleDialog = false
                if (isPermanentlyDeniedNotification) {
                    PermissionHelper.openNotificationSettings(context)
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        PermissionHelper.openNotificationSettings(context)
                    }
                }
            },
            onDismiss = { showNotificationRationaleDialog = false }
        )
    }
}

@Composable
private fun AlertPreferenceRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

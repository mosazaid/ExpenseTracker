package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.navigation.AppRoutes

@Composable
fun MoreScreen(navController: NavController) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.nav_more),
                navController = navController,
                canNavigateBack = false
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
        ) {
            // ── Section 1: Financial Hub
            item {
                Text(
                    text = stringResource(R.string.more_financial_tools),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                MoreMenuCard(
                    title = stringResource(R.string.loans_title),
                    subtitle = stringResource(R.string.loans_subtitle),
                    icon = Icons.Default.AccountBalance,
                    onClick = { navController.navigate(AppRoutes.LOANS) }
                )
            }

            item {
                MoreMenuCard(
                    title = stringResource(R.string.debts_tracker_title),
                    subtitle = stringResource(R.string.debts_subtitle),
                    icon = Icons.Default.Handshake,
                    onClick = { navController.navigate(AppRoutes.DEBTS) }
                )
            }

            item {
                MoreMenuCard(
                    title = stringResource(R.string.categories_budgets_title),
                    subtitle = stringResource(R.string.categories_budgets_subtitle),
                    icon = Icons.Default.Category,
                    onClick = { navController.navigate(AppRoutes.CATEGORIES) }
                )
            }

            // ── Section 2: Tools & System
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.more_system_tools),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                MoreMenuCard(
                    title = stringResource(R.string.database_browser),
                    subtitle = stringResource(R.string.database_browser_desc),
                    icon = Icons.Default.Storage,
                    onClick = { navController.navigate(AppRoutes.DATABASE_BROWSER) }
                )
            }

            item {
                MoreMenuCard(
                    title = stringResource(R.string.settings),
                    subtitle = stringResource(R.string.settings_desc),
                    icon = Icons.Default.Settings,
                    onClick = { navController.navigate(AppRoutes.SETTINGS) }
                )
            }
        }
    }
}

@Composable
private fun MoreMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

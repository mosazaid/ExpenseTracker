package com.mousa.mizani.presentation.feature.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mousa.mizani.BuildConfig
import com.mousa.mizani.R
import com.mousa.mizani.presentation.common.AppTopBar
import com.mousa.mizani.presentation.feature.main.more.MoreEntry
import com.mousa.mizani.presentation.feature.main.more.MoreGroup
import com.mousa.mizani.presentation.feature.main.more.MoreMenu
import com.mousa.mizani.presentation.feature.main.more.MoreTileRow
import com.mousa.mizani.presentation.feature.main.more.MoreTileSpacing
import com.mousa.mizani.presentation.feature.main.more.moreColumns
import com.mousa.mizani.presentation.feature.settings.components.SettingsGroupHeader
import com.mousa.mizani.presentation.feature.settings.components.SettingsNavRow

private val ScreenPadding = 16.dp

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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val columns = moreColumns(
                availableWidthDp = (maxWidth - ScreenPadding * 2).value,
                spacingDp = MoreTileSpacing.value,
                fontScale = LocalDensity.current.fontScale
            )
            val onEntryClick: (MoreEntry) -> Unit = { navController.navigate(it.destination) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                MoreMenu.entries.groupBy { it.group }.forEach { (group, entries) ->
                    moreGroup(group, entries, columns, onEntryClick)
                }
                item { MoreFooter() }
            }
        }
    }
}

/** App entries are full-width rows; every other group is a grid of tiles. */
private fun LazyListScope.moreGroup(
    group: MoreGroup,
    entries: List<MoreEntry>,
    columns: Int,
    onEntryClick: (MoreEntry) -> Unit
) {
    item { SettingsGroupHeader(title = stringResource(group.titleRes())) }

    if (group == MoreGroup.APP) {
        entries.forEach { entry ->
            item {
                SettingsNavRow(
                    title = stringResource(entry.labelRes),
                    summary = null,
                    icon = entry.icon,
                    onClick = { onEntryClick(entry) }
                )
            }
        }
    } else {
        entries.chunked(columns).forEach { rowEntries ->
            item {
                MoreTileRow(
                    entries = rowEntries,
                    columns = columns,
                    onEntryClick = onEntryClick,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = MoreTileSpacing / 2)
                )
            }
        }
    }
}

@StringRes
private fun MoreGroup.titleRes(): Int = when (this) {
    MoreGroup.EVERYDAY -> R.string.more_group_everyday
    MoreGroup.PLAN_TRAVEL -> R.string.more_group_plan_travel
    MoreGroup.ISLAMIC -> R.string.more_group_islamic
    MoreGroup.APP -> R.string.more_group_app
}

@Composable
private fun MoreFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.offline_storage_secure),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

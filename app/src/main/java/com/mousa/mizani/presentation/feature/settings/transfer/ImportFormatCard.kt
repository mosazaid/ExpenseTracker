package com.mousa.mizani.presentation.feature.settings.transfer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mousa.mizani.R
import com.mousa.mizani.core.export.CsvImportFormat

private val MinTouchHeight = 48.dp

/** Collapsible help for the CSV import format, with the blank template download. */
@Composable
fun ImportFormatCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    onDownloadTemplate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(
                            if (expanded) R.string.import_format_hide else R.string.import_format_show
                        ),
                        onClick = onToggle
                    )
                    .heightIn(min = MinTouchHeight),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.import_csv_format),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            Text(
                stringResource(R.string.import_csv_format_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (expanded) {
                FormatDetails()
                OutlinedButton(
                    onClick = onDownloadTemplate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MinTouchHeight)
                ) {
                    Text(stringResource(R.string.download_import_template))
                }
            }
        }
    }
}

@Composable
private fun FormatDetails() {
    LabeledValue(R.string.import_required_columns, CsvImportFormat.REQUIRED_COLUMNS.joinToString(", "), true)
    LabeledValue(R.string.import_optional_columns, CsvImportFormat.OPTIONAL_COLUMNS.joinToString(", "), true)
    LabeledValue(R.string.import_type_values, CsvImportFormat.TYPE_VALUES, false)
    LabeledValue(R.string.import_account_values, CsvImportFormat.ACCOUNT_VALUES, false)
    LabeledValue(R.string.import_date_format, CsvImportFormat.DATE_FORMAT, true)
    Text(stringResource(R.string.import_example_row), style = MaterialTheme.typography.labelMedium)
    CodeBlock(CsvImportFormat.HEADER)
    CodeBlock(CsvImportFormat.EXAMPLE_ROW)
    Text(
        stringResource(R.string.import_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun LabeledValue(labelRes: Int, value: String, isCode: Boolean) {
    Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium)
    Text(
        value,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = if (isCode) FontFamily.Monospace else null
    )
}

@Composable
private fun CodeBlock(text: String) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
        Text(
            text,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

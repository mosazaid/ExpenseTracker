package com.example.expensetracker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.database.entities.Category

@Composable
fun CategoryRow(
    category: Category,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    onSetBudget: ((Category) -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = category.icon,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(category.name, style = MaterialTheme.typography.bodyLarge)
                    if (category.isDefault) {
                        Text(
                            "Default",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            Row {
                if (onSetBudget != null && category.type == com.example.expensetracker.data.database.entities.TransactionType.EXPENSE) {
                    TextButton(onClick = { onSetBudget(category) }) { Text("Budget") }
                }
                TextButton(onClick = { onEdit(category) }) { Text("Edit") }
                TextButton(
                    onClick = { onDelete(category) },
                    enabled = !category.isDefault
                ) {
                    Text(
                        "Delete",
                        color = if (category.isDefault)
                            MaterialTheme.colorScheme.outline
                        else Color.Red
                    )
                }
            }
        }
    }
}

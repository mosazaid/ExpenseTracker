package com.example.expensetracker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.database.entities.Category

@Composable
fun CategoryRow(
    category: Category,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Text(
                    text = category.icon,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(category.name)
            }
            Row {
                TextButton(onClick = { onEdit(category) }) { Text("Edit") }
                TextButton(onClick = { onDelete(category) }) { Text("Delete", color = Color.Red) }
            }
        }
    }
}
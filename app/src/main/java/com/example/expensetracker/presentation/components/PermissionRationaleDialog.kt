package com.example.expensetracker.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

enum class PermissionType {
    CAMERA,
    LOCATION,
    NOTIFICATIONS
}

/**
 * Clean, accessible dialog informing users why a runtime permission is needed,
 * adhering to UI/UX Pro Max guidelines with visible hierarchy and clear CTAs.
 */
@Composable
fun PermissionRationaleDialog(
    permissionType: PermissionType,
    isPermanentlyDenied: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, description, icon) = when (permissionType) {
        PermissionType.CAMERA -> Triple(
            "Camera Access Needed",
            if (isPermanentlyDenied) {
                "Camera permission was previously denied. Please open Settings and enable Camera permission to capture receipts."
            } else {
                "Camera permission allows you to take photos of paper receipts and attach them directly to your transactions for effortless tracking."
            },
            Icons.Outlined.CameraAlt
        )
        PermissionType.LOCATION -> Triple(
            "Location Access Needed",
            if (isPermanentlyDenied) {
                "Location permission was previously denied. Please open Settings and enable Location permission to auto-detect your city, currency, and Qiblah direction."
            } else {
                "Location permission is used offline to detect your city for accurate prayer times, Qiblah compass, and default currency."
            },
            Icons.Outlined.LocationOn
        )
        PermissionType.NOTIFICATIONS -> Triple(
            "Notifications Needed",
            if (isPermanentlyDenied) {
                "Notification permission is currently blocked. Please open Settings to enable alerts for upcoming loan dues, budget thresholds, and daily reminders."
            } else {
                "Allow notifications to receive timely alerts for loan payments, salary cycle renewals, and spending budget warnings."
            },
            Icons.Outlined.Notifications
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPermanentlyDenied) Icons.Outlined.Settings else icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Not Now")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isPermanentlyDenied) "Open Settings" else "Continue")
                    }
                }
            }
        }
    }
}

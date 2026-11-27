package com.mousa.mizani.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mousa.mizani.R

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
    val (titleRes, descriptionRes, icon) = when (permissionType) {
        PermissionType.CAMERA -> Triple(
            R.string.permission_camera_title,
            if (isPermanentlyDenied) {
                R.string.permission_camera_denied_desc
            } else {
                R.string.permission_camera_desc
            },
            Icons.Outlined.CameraAlt
        )
        PermissionType.LOCATION -> Triple(
            R.string.permission_location_title,
            if (isPermanentlyDenied) {
                R.string.permission_location_denied_desc
            } else {
                R.string.permission_location_desc
            },
            Icons.Outlined.LocationOn
        )
        PermissionType.NOTIFICATIONS -> Triple(
            R.string.permission_notifications_title,
            if (isPermanentlyDenied) {
                R.string.permission_notifications_denied_desc
            } else {
                R.string.permission_notifications_desc
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
                    .verticalScroll(rememberScrollState())
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
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(descriptionRes),
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
                        Text(stringResource(R.string.permission_not_now))
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            stringResource(
                                if (isPermanentlyDenied) {
                                    R.string.permission_open_settings
                                } else {
                                    R.string.permission_continue
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

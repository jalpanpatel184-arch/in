package com.nova.assistant.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*
import com.nova.assistant.ui.theme.*

data class PermissionItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val isRequired: Boolean,
    val onRequest: () -> Unit
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current

    val microphonePermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val notificationPermission = rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    val contactsPermission = rememberPermissionState(android.Manifest.permission.READ_CONTACTS)
    val smsPermission = rememberPermissionState(android.Manifest.permission.SEND_SMS)

    val permissions = listOf(
        PermissionItem("Microphone", "Required to hear your voice commands", Icons.Default.Mic,
            microphonePermission.status.isGranted, true) { microphonePermission.launchPermissionRequest() },
        PermissionItem("Accessibility Service", "Allows Nova to control apps and UI elements",
            Icons.Default.Accessibility, false, true) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        },
        PermissionItem("Overlay Permission", "Shows floating Nova bubble over other apps",
            Icons.Default.BubbleChart, Settings.canDrawOverlays(context), true) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        },
        PermissionItem("Notifications", "Nova can post notifications and read them for you",
            Icons.Default.Notifications, notificationPermission.status.isGranted, false) {
            notificationPermission.launchPermissionRequest()
        },
        PermissionItem("Contacts", "Find contacts for messaging commands",
            Icons.Default.Contacts, contactsPermission.status.isGranted, false) {
            contactsPermission.launchPermissionRequest()
        },
        PermissionItem("SMS", "Send text messages by voice",
            Icons.Default.Sms, smsPermission.status.isGranted, false) {
            smsPermission.launchPermissionRequest()
        }
    )

    val allRequiredGranted = permissions.filter { it.isRequired }.all { it.isGranted }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Text("Setup Nova", style = MaterialTheme.typography.headlineLarge, color = NovaCyan)
        Spacer(Modifier.height(8.dp))
        Text("Grant permissions to unlock Nova's full power",
            style = MaterialTheme.typography.bodyLarge, color = NovaTextSecondary)
        Spacer(Modifier.height(32.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(permissions) { perm ->
                PermissionCard(item = perm)
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAllGranted,
            enabled = allRequiredGranted,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NovaCyan,
                contentColor = NovaBlack,
                disabledContainerColor = NovaTextMuted.copy(alpha = 0.3f)
            )
        ) {
            Text(if (allRequiredGranted) "Launch Nova →" else "Grant required permissions first",
                style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun PermissionCard(item: PermissionItem) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (item.isGranted) NovaCyan.copy(alpha = 0.07f) else NovaDarkSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(item.icon, null,
                tint = if (item.isGranted) NovaCyan else NovaTextMuted,
                modifier = Modifier.size(26.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.title, style = MaterialTheme.typography.bodyLarge, color = NovaTextPrimary)
                    if (item.isRequired) {
                        Text("REQUIRED", style = MaterialTheme.typography.labelSmall, color = NovaAmber)
                    }
                }
                Text(item.description, style = MaterialTheme.typography.bodyMedium, color = NovaTextSecondary)
            }
            if (item.isGranted) {
                Icon(Icons.Default.CheckCircle, null, tint = NovaGreen, modifier = Modifier.size(22.dp))
            } else {
                OutlinedButton(
                    onClick = item.onRequest,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NovaCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NovaCyan.copy(alpha = 0.5f)))
                ) { Text("Grant", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

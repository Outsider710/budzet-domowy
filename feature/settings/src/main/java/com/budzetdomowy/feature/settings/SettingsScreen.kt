package com.budzetdomowy.feature.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.budzetdomowy.core.data.ThemeMode
import com.budzetdomowy.core.ui.BudzetTopBar
import com.budzetdomowy.core.ui.R
import com.budzetdomowy.core.ui.ScrollColumn

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onCategories: () -> Unit,
    onRecurring: () -> Unit,
    appName: String,
    versionName: String,
    versionCode: Int,
    privacyUrl: String
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDisableDialog by remember { mutableStateOf(false) }
    var systemNotificationsAllowed by remember {
        mutableStateOf(areSystemNotificationsAllowed(context))
    }

    fun syncWithSystem() {
        val allowed = areSystemNotificationsAllowed(context)
        systemNotificationsAllowed = allowed
        if (notificationsEnabled != allowed) {
            onNotificationsEnabledChange(allowed)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                syncWithSystem()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val allowed = areSystemNotificationsAllowed(context)
        systemNotificationsAllowed = allowed
        if (granted && allowed) {
            onNotificationsEnabledChange(true)
        } else {
            // Denied or OEM still blocks — open system notification settings.
            openSystemNotificationSettings(context)
        }
    }

    fun enableNotifications() {
        if (areSystemNotificationsAllowed(context)) {
            systemNotificationsAllowed = true
            onNotificationsEnabledChange(true)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        openSystemNotificationSettings(context)
    }

    fun disableNotifications() {
        onNotificationsEnabledChange(false)
        openSystemNotificationSettings(context)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BudzetTopBar(
                title = stringResource(R.string.settings),
                onBack = onBack
            )
        }
    ) { padding ->
        ScrollColumn(contentPadding = padding) {
            Text(appName, style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(
                    R.string.version_format,
                    versionName,
                    versionCode
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.theme_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                }
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.settings_notifications),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.settings_notifications_enabled),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = systemNotificationsAllowed,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            enableNotifications()
                        } else {
                            showDisableDialog = true
                        }
                    }
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.settings_privacy_body),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = onCategories,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.categories_title))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRecurring,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.recurring_title))
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_no_browser),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_privacy_open_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.privacy_policy))
            }
        }
    }

    if (showDisableDialog) {
        AlertDialog(
            onDismissRequest = { showDisableDialog = false },
            title = { Text(stringResource(R.string.settings_notifications_disable_title)) },
            text = { Text(stringResource(R.string.settings_notifications_disable_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisableDialog = false
                        disableNotifications()
                    }
                ) {
                    Text(stringResource(R.string.settings_notifications_disable_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun areSystemNotificationsAllowed(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return false
    }
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun openSystemNotificationSettings(context: Context) {
    val appNotifications = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
        putExtra("app_package", context.packageName)
        putExtra("app_uid", context.applicationInfo.uid)
    }
    try {
        context.startActivity(appNotifications)
        return
    } catch (_: Exception) {
        // Fall through to app details.
    }
    val details = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(details)
}

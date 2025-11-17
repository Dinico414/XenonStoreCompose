package com.xenonware.store.ui.res

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

data class VersionInfo(
    val appVersion: String,
    val xenonUIVersion: String,
    val xenonCommonsVersion: String
)

@Composable
fun DialogVersionNumber(
    onDismiss: () -> Unit,
    versionInfo: VersionInfo
) {
    val context = LocalContext.current

    val onMoreInfoClick: () -> Unit = {
        onDismiss()

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    XenonDialog(
        onDismissRequest = onDismiss,
        title = "Version",
        confirmButtonText = "More Infos",
        onConfirmButtonClick = onMoreInfoClick,
        properties = DialogProperties(usePlatformDefaultWidth = true),

        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            VersionItem(label = "App Version", version = versionInfo.appVersion)
            VersionItem(label = "XenonUi Version", version = versionInfo.xenonUIVersion)
            VersionItem(label = "XenonCommons Version", version = versionInfo.xenonCommonsVersion)
        }
    }
}

@Composable
private fun VersionItem(label: String, version: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = version,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

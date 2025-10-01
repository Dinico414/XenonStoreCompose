package com.xenon.store.viewmodel.classes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenon.store.InstallMethod
import com.xenon.store.R
import com.xenon.store.ui.res.SettingsSwitchTile
import com.xenon.store.ui.values.LargerPadding
import com.xenon.store.ui.values.SmallSpacing
import com.xenon.store.viewmodel.DevSettingsViewModel

@Composable
fun DevSettingsItems(
    viewModel: DevSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val isDeveloperModeEnabled by viewModel.devModeToggleState.collectAsState()
    val isShowDummyProfileEnabled by viewModel.showDummyProfileState.collectAsState()
    val currentInstallMethod by viewModel.installMethodState.collectAsState()

    var showInstallMethodDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(LargerPadding)
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.dev_settings_description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = LargerPadding)
        )

        SettingsSwitchTile(
            title = stringResource(id = R.string.developer_options_title),
            subtitle = "",
            checked = isDeveloperModeEnabled,
            onCheckedChange = {
                viewModel.setDeveloperModeEnabled(it)
            },
            onClick = {
                viewModel.setDeveloperModeEnabled(!isDeveloperModeEnabled)
            }
        )

        if (isDeveloperModeEnabled) {
            Spacer(modifier = Modifier.height(SmallSpacing))

            SettingsSwitchTile(
                title = stringResource(id = R.string.show_dummy_profile_title),
                subtitle = "",
                checked = isShowDummyProfileEnabled,
                onCheckedChange = {
                    viewModel.setShowDummyProfileEnabled(it)
                },
                onClick = {
                    viewModel.setShowDummyProfileEnabled(!isShowDummyProfileEnabled)
                }
            )

            Spacer(modifier = Modifier.height(SmallSpacing))

            // Install Method Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showInstallMethodDialog = true }
                    .padding(vertical = LargerPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.select_install_method_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = getInstallMethodName(currentInstallMethod),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showInstallMethodDialog) {
                AlertDialog(
                    onDismissRequest = { showInstallMethodDialog = false },
                    title = { Text(stringResource(R.string.select_install_method_title)) },
                    text = {
                        Column {
                            InstallMethod.values().filter { it != InstallMethod.SHIZUKU }.forEach { method ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setInstallMethod(method)
                                            showInstallMethodDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (method == currentInstallMethod),
                                        onClick = null // Click handled by Row
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = getInstallMethodName(method))
                                }
                            }
                        }
                    },
                    confirmButton = { },
                    dismissButton = {
                        TextButton(onClick = { showInstallMethodDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun getInstallMethodName(method: InstallMethod): String {
    return when (method) {
        InstallMethod.DEFAULT -> "Default"
        InstallMethod.ROOT -> "Root"
        InstallMethod.SHIZUKU -> "Shizuku" // Keep for display if it's the saved preference
    }
}

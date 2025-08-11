package com.xenon.store.viewmodel.classes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xenon.store.R
import com.xenon.store.viewmodel.DevSettingsViewModel
import com.xenon.store.ui.res.SettingsSwitchTile
import com.xenon.store.ui.values.LargerPadding
import com.xenon.store.ui.values.SmallSpacing

@Composable
fun DevSettingsItems(
    viewModel: DevSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val isDeveloperModeEnabled by viewModel.devModeToggleState.collectAsState()
    val isShowDummyProfileEnabled by viewModel.showDummyProfileState.collectAsState()

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
            onCheckedChange = { newCheckedState ->
                viewModel.setDeveloperModeEnabled(newCheckedState)
            },
            onClick = {
                val newCheckedState = !isDeveloperModeEnabled
                viewModel.setDeveloperModeEnabled(newCheckedState)
            }
        )

        if (isDeveloperModeEnabled) {
            Spacer(modifier = Modifier.height(SmallSpacing))

            SettingsSwitchTile(
                title = stringResource(id = R.string.show_dummy_profile_title),
                subtitle = "",
                checked = isShowDummyProfileEnabled,
                onCheckedChange = { newCheckedState ->
                    viewModel.setShowDummyProfileEnabled(newCheckedState)
                },
                onClick = {
                    val newCheckedState = !isShowDummyProfileEnabled
                    viewModel.setShowDummyProfileEnabled(newCheckedState)
                }
            )
        }
    }
}

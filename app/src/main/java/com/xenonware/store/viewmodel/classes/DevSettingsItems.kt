package com.xenonware.store.viewmodel.classes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.values.ExtraLargeSpacing
import com.xenon.mylibrary.values.LargerPadding
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenon.mylibrary.values.NoCornerRadius
import com.xenon.mylibrary.values.SmallSpacing
import com.xenon.mylibrary.values.SmallestCornerRadius
import com.xenonware.store.InstallMethod
import com.xenonware.store.R
import com.xenonware.store.ui.res.SettingsSwitchTile
import com.xenonware.store.ui.res.SettingsTile
import com.xenonware.store.viewmodel.DevSettingsViewModel
import com.xenonware.store.viewmodel.SettingsViewModel

@Composable
fun DevSettingsItems(
    settingsViewModel: SettingsViewModel,
    viewModel: DevSettingsViewModel,
    modifier: Modifier = Modifier,
    innerGroupRadius: Dp = SmallestCornerRadius,
    outerGroupRadius: Dp = MediumCornerRadius,
    innerGroupSpacing: Dp = SmallSpacing,
    outerGroupSpacing: Dp = ExtraLargeSpacing,
    tileBackgroundColor: Color = MaterialTheme.colorScheme.surfaceBright,
    tileContentColor: Color = MaterialTheme.colorScheme.onSurface,
    tileSubtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tileShapeOverride: Shape? = null,
    tileHorizontalPadding: Dp = LargerPadding,
    tileVerticalPadding: Dp = LargerPadding,
    useGroupStyling: Boolean = true,
) {
    val isDeveloperModeEnabled by viewModel.devModeToggleState.collectAsState()
    val isShowDummyProfileEnabled by viewModel.showDummyProfileState.collectAsState()

    LocalContext.current
    LocalHapticFeedback.current

    val actualInnerGroupRadius = if (useGroupStyling) innerGroupRadius else 0.dp
    val actualOuterGroupRadius = if (useGroupStyling) outerGroupRadius else 0.dp
    if (useGroupStyling) innerGroupSpacing else 0.dp
    outerGroupSpacing // outerGroupSpacing is used directly

    SwitchDefaults.colors()

    val isShizukuAvailable by viewModel.isShizukuAvailable.collectAsState()
    var showInstallMethodDialog by remember { mutableStateOf(false) }
    val currentInstallMethod by viewModel.installMethodState.collectAsState()


    val topShape = if (useGroupStyling) RoundedCornerShape(
        bottomStart = actualInnerGroupRadius,
        bottomEnd = actualInnerGroupRadius,
        topStart = actualOuterGroupRadius,
        topEnd = actualOuterGroupRadius
    ) else RoundedCornerShape(NoCornerRadius)

    val middleShape = if (useGroupStyling) RoundedCornerShape(
        topStart = actualInnerGroupRadius,
        topEnd = actualInnerGroupRadius,
        bottomStart = actualInnerGroupRadius,
        bottomEnd = actualInnerGroupRadius
    ) else RoundedCornerShape(NoCornerRadius)

    val bottomShape = if (useGroupStyling) RoundedCornerShape(
        topStart = actualInnerGroupRadius,
        topEnd = actualInnerGroupRadius,
        bottomStart = actualOuterGroupRadius,
        bottomEnd = actualOuterGroupRadius
    ) else RoundedCornerShape(NoCornerRadius)

    val standaloneShape = if (useGroupStyling) RoundedCornerShape(actualOuterGroupRadius)
    else RoundedCornerShape(NoCornerRadius)

    Column(
        modifier = modifier
            .padding(LargerPadding)
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.dev_settings_description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = LargerPadding).align(alignment = Alignment.CenterHorizontally)
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
            },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
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
                },
                shape = tileShapeOverride ?: bottomShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                horizontalPadding = tileHorizontalPadding,
                verticalPadding = tileVerticalPadding
            )

            Spacer(
                modifier = Modifier.height(
                    ExtraLargeSpacing
                )
            )

            SettingsTile(
                title = stringResource(R.string.select_install_method_title),
                subtitle = getInstallMethodName(currentInstallMethod),
                onClick = { showInstallMethodDialog = true },
                shape = tileShapeOverride ?: standaloneShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,

                )

            if (showInstallMethodDialog) {
                AlertDialog(
                    onDismissRequest = { showInstallMethodDialog = false },
                    title = { Text(stringResource(R.string.select_install_method_title)) },
                    text = {
                        Column {
                            InstallMethod.values().forEach { method ->
                                val isEnabled =
                                    if (method == InstallMethod.SHIZUKU) isShizukuAvailable else true
                                Row(Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isEnabled) {
                                        viewModel.setInstallMethod(method)
                                        showInstallMethodDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = (method == currentInstallMethod),
                                        onClick = null,
                                        enabled = isEnabled
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = getInstallMethodName(method),
                                            color = if (isEnabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.38f
                                            )
                                        )
                                        if (method == InstallMethod.SHIZUKU && !isShizukuAvailable) {
                                            Text(
                                                text = stringResource(R.string.disabled),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.38f
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { },
                    dismissButton = {
                        TextButton(onClick = { showInstallMethodDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    })
            }
        }
    }
}

@Composable
private fun getInstallMethodName(method: InstallMethod): String {
    return when (method) {
        InstallMethod.DEFAULT -> "Default"
        InstallMethod.SHIZUKU -> "Shizuku"
        InstallMethod.ROOT -> "Root"
    }
}

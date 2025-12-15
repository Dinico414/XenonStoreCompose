package com.xenonware.store.viewmodel.classes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.SettingsSwitchMenuTile
import com.xenon.mylibrary.res.SettingsSwitchTile
import com.xenon.mylibrary.res.SettingsTile
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.values.ExtraLargeSpacing
import com.xenon.mylibrary.values.LargerPadding
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenon.mylibrary.values.NoCornerRadius
import com.xenon.mylibrary.values.SmallSpacing
import com.xenon.mylibrary.values.SmallestCornerRadius
import com.xenonware.store.data.InstallMethod
import com.xenonware.store.R
import com.xenonware.store.ui.res.DialogGitHubApps
import com.xenonware.store.util.Util.Companion.getCurrentLanguage
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
    val isAddButtonEnabled by viewModel.addButtonState.collectAsState()
    val editingApp by viewModel.editingApp.collectAsState()

    val context = LocalContext.current
    LocalHapticFeedback.current

    val actualInnerGroupRadius = if (useGroupStyling) innerGroupRadius else 0.dp
    val actualOuterGroupRadius = if (useGroupStyling) outerGroupRadius else 0.dp
    if (useGroupStyling) innerGroupSpacing else 0.dp
    outerGroupSpacing // outerGroupSpacing is used directly

    SwitchDefaults.colors()

    val isShizukuAvailable by viewModel.isShizukuAvailable.collectAsState()
    var showInstallMethodDialog by remember { mutableStateOf(false) }
    val currentInstallMethod by viewModel.installMethodState.collectAsState()
    var showGithubAppDialog by remember { mutableStateOf(false) }
    var showAddEditGithubAppDialog by remember { mutableStateOf(false) }


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
            modifier = Modifier
                .padding(bottom = LargerPadding)
                .align(alignment = Alignment.CenterHorizontally)
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
            },
            shape = tileShapeOverride ?: if (!isDeveloperModeEnabled) standaloneShape else topShape,
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
                onCheckedChange = {
                    viewModel.setShowDummyProfileEnabled(it)
                },
                onClick = {
                    viewModel.setShowDummyProfileEnabled(!isShowDummyProfileEnabled)
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

            Spacer(
                modifier = Modifier.height(
                    ExtraLargeSpacing
                )
            )

            SettingsSwitchMenuTile(
                title = stringResource(R.string.github_app),
                subtitle = stringResource(R.string.github_app_description),
                checked = isAddButtonEnabled,
                onCheckedChange = { viewModel.setAddButtonEnabled(it) },
                onClick = { showGithubAppDialog = true },
                shape = tileShapeOverride ?: standaloneShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,

                )


            if (showInstallMethodDialog) {
                XenonDialog(
                    onDismissRequest = { showInstallMethodDialog = false },
                    title = stringResource(R.string.select_install_method_title),
                    properties = DialogProperties(usePlatformDefaultWidth = true),
                ) {
                    Column {
                        InstallMethod.values().forEach { method ->
                            val isEnabled =
                                if (method == InstallMethod.SHIZUKU) isShizukuAvailable else true
                            Row(modifier = Modifier
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
                }
            }
            if (showGithubAppDialog) {
                val githubApps by viewModel.githubApps.collectAsState()
                XenonDialog(
                    onDismissRequest = { showGithubAppDialog = false },
                    title = stringResource(R.string.manage_github_apps_title),
                    confirmButtonText = stringResource(R.string.add),
                    onConfirmButtonClick = { showAddEditGithubAppDialog = true },
                    properties = DialogProperties(usePlatformDefaultWidth = true),
                ) {
                    Column {
                        if (githubApps.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_github_apps_added),
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            githubApps.forEach { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(onClick = {
                                        viewModel.onEditGitHubApp(app)
                                        showAddEditGithubAppDialog = true
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.edit)
                                        )
                                    }
                                    Text(
                                        text = app.getName(getCurrentLanguage(context.resources)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    )
                                    IconButton(onClick = { viewModel.onDeleteGitHubApp(app) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (showAddEditGithubAppDialog) {
                var owner by remember(editingApp) { mutableStateOf(editingApp?.owner ?: "") }
                var repo by remember(editingApp) { mutableStateOf(editingApp?.repo ?: "") }
                var packageName by remember(editingApp) { mutableStateOf(editingApp?.packageName ?: "") }
                var pat by remember { mutableStateOf("") }

                LaunchedEffect(editingApp) {
                    owner = editingApp?.owner ?: ""
                    repo = editingApp?.repo ?: ""
                    packageName = editingApp?.packageName ?: ""
                }

                DialogGitHubApps(
                    onDismissRequest = {
                        viewModel.onDialogDismiss()
                        showAddEditGithubAppDialog = false
                    },
                    onConfirm = {
                        viewModel.onSaveGitHubApp(owner, repo, packageName)
                        showAddEditGithubAppDialog = false
                    },
                    owner = owner,
                    onOwnerChange = { owner = it },
                    repo = repo,
                    onRepoChange = { repo = it },
                    packageName = packageName,
                    onPackageNameChange = { packageName = it },
                    gitHubPAT = pat,
                    onGitHubPATChange = { pat = it }
                )
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

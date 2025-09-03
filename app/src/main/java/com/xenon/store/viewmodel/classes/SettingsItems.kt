package com.xenon.store.viewmodel.classes

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenon.store.R
import com.xenon.store.ui.res.SettingsGoogleTile
import com.xenon.store.ui.res.SettingsSwitchMenuTile
import com.xenon.store.ui.res.SettingsSwitchTile
import com.xenon.store.ui.res.SettingsTile
import com.xenon.store.ui.values.ExtraLargeSpacing
import com.xenon.store.ui.values.LargerPadding
import com.xenon.store.ui.values.MediumCornerRadius
import com.xenon.store.ui.values.NoCornerRadius
import com.xenon.store.ui.values.SmallSpacing
import com.xenon.store.ui.values.SmallestCornerRadius
import com.xenon.store.viewmodel.DevSettingsViewModel
import com.xenon.store.viewmodel.SettingsViewModel


@Composable
fun SettingsItems(
    viewModel: SettingsViewModel,
    devSettingsViewModel: DevSettingsViewModel = viewModel(),
    currentThemeTitle: String,
    applyCoverTheme: Boolean,
    coverThemeEnabled: Boolean,
    currentLanguage: String,
    currentFormat: String,
    appVersion: String,
    onNavigateToDeveloperOptions: () -> Unit,
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
    switchColorsOverride: SwitchColors? = null,
    useGroupStyling: Boolean = true,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val blackedOutEnabled by viewModel.blackedOutModeEnabled.collectAsState()
    val developerModeEnabled by viewModel.developerModeEnabled.collectAsState()
    val checkForPreReleases by viewModel.checkForPreReleases.collectAsState() // Collect pre-release state

    val actualInnerGroupRadius = if (useGroupStyling) innerGroupRadius else 0.dp
    val actualOuterGroupRadius = if (useGroupStyling) outerGroupRadius else 0.dp
    val actualInnerGroupSpacing = if (useGroupStyling) innerGroupSpacing else 0.dp
    val actualOuterGroupSpacing = outerGroupSpacing // outerGroupSpacing is used directly

    val defaultSwitchColors = SwitchDefaults.colors()

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

    val showDummyProfile by devSettingsViewModel.showDummyProfileState.collectAsState()
    val isDeveloperModeEnabled by devSettingsViewModel.devModeToggleState.collectAsState() // This seems to be from DevSettingsViewModel, ensure it's the correct one for overall dev mode visibility

    if (isDeveloperModeEnabled && showDummyProfile) { // Assuming isDeveloperModeEnabled from devSettingsViewModel is the primary toggle for the dummy profile
        SettingsGoogleTile(
            title = "Your Name",
            subtitle = "your.email@gmail.com",
            onClick = {
                Toast.makeText(
                    context,
                    "Dummy Unit, open Google Account coming soon",
                    Toast.LENGTH_SHORT
                ).show()
            },
            shape = tileShapeOverride ?: standaloneShape,
            backgroundColor = Color.Transparent,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
        Spacer(Modifier.height(actualOuterGroupSpacing))
    }

    SettingsTile(
        title = stringResource(id = R.string.theme),
        subtitle = "${stringResource(id = R.string.current)} $currentThemeTitle",
        onClick = { viewModel.onThemeSettingClicked() },
        icon = {
            Icon(
                painterResource(R.drawable.themes),
                stringResource(R.string.theme),
                tint = tileSubtitleColor
            )
        },
        shape = tileShapeOverride ?: topShape,
        backgroundColor = tileBackgroundColor,
        contentColor = tileContentColor,
        subtitleColor = tileSubtitleColor,
        horizontalPadding = tileHorizontalPadding,
        verticalPadding = tileVerticalPadding
    )
    Spacer(Modifier.height(actualInnerGroupSpacing))
    SettingsSwitchTile(
        title = stringResource(R.string.blacked_out),
        subtitle = stringResource(R.string.blacked_out_description),
        checked = blackedOutEnabled,
        onCheckedChange = { viewModel.setBlackedOutEnabled(it) },
        onClick = { viewModel.setBlackedOutEnabled(!blackedOutEnabled) },
        icon = {
            Icon(
                painterResource(R.drawable.blacked_out),
                stringResource(R.string.blacked_out),
                tint = tileSubtitleColor
            )
        },
        shape = tileShapeOverride ?: middleShape,
        backgroundColor = tileBackgroundColor,
        contentColor = tileContentColor,
        subtitleColor = tileSubtitleColor,
        dividerColor = Color.Transparent,
        horizontalPadding = tileHorizontalPadding,
        verticalPadding = tileVerticalPadding,
        switchColors = switchColorsOverride ?: defaultSwitchColors
    )
    Spacer(Modifier.height(actualInnerGroupSpacing))
    SettingsSwitchMenuTile(
        title = stringResource(R.string.cover_screen_mode),
        subtitle = "${stringResource(R.string.cover_screen_mode_description)} (${
            if (applyCoverTheme) stringResource(
                R.string.enabled
            ) else stringResource(R.string.disabled)
        })",
        checked = coverThemeEnabled,
        onCheckedChange = { viewModel.setCoverThemeEnabled(it) },
        onClick = { viewModel.onCoverThemeClicked() },
        icon = {
            Icon(
                painterResource(R.drawable.cover_screen),
                stringResource(R.string.cover_screen_mode),
                tint = tileSubtitleColor
            )
        },
        shape = tileShapeOverride ?: bottomShape,
        backgroundColor = tileBackgroundColor,
        contentColor = tileContentColor,
        subtitleColor = tileSubtitleColor,
        horizontalPadding = tileHorizontalPadding,
        verticalPadding = tileVerticalPadding,
        switchColors = switchColorsOverride ?: defaultSwitchColors
    )

    Spacer(Modifier.height(outerGroupSpacing))

    SettingsTile(
        title = stringResource(R.string.language),
        subtitle = "${stringResource(R.string.current)} $currentLanguage",
        onClick = { viewModel.onLanguageSettingClicked(context) },
        icon = {
            Icon(
                painterResource(R.drawable.language),
                stringResource(R.string.language),
                tint = tileSubtitleColor
            )
        },
        shape = tileShapeOverride ?: standaloneShape,
        backgroundColor = tileBackgroundColor,
        contentColor = tileContentColor,
        subtitleColor = tileSubtitleColor,
        horizontalPadding = tileHorizontalPadding,
        verticalPadding = tileVerticalPadding
    )
    LaunchedEffect(Unit) { viewModel.updateCurrentLanguage() }

    Spacer(Modifier.height(outerGroupSpacing))

    SettingsTile(
        title = stringResource(R.string.clear_data),
        subtitle = stringResource(R.string.clear_data_description),
        onClick = { viewModel.onClearDataClicked(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
        icon = {
            Icon(
                painterResource(R.drawable.reset),
                stringResource(R.string.clear_data),
                tint = tileSubtitleColor
            )
        },
        shape = tileShapeOverride ?: topShape,
        backgroundColor = tileBackgroundColor,
        contentColor = tileContentColor,
        subtitleColor = tileSubtitleColor,
        horizontalPadding = tileHorizontalPadding,
        verticalPadding = tileVerticalPadding
    )
    Spacer(Modifier.height(actualInnerGroupSpacing))
    SettingsTile(
        title = stringResource(R.string.reset_settings),
        subtitle = "",
        onClick = {
            viewModel.onResetSettingsClicked(); haptic.performHapticFeedback(
            HapticFeedbackType.LongPress
        )
        },
        icon = {
            Icon(
                painterResource(R.drawable.reset_settings),
                stringResource(R.string.reset_settings),
                tint = tileSubtitleColor
            )
        },
        shape = tileShapeOverride ?: middleShape,
        backgroundColor = tileBackgroundColor,
        contentColor = tileContentColor,
        subtitleColor = tileSubtitleColor,
        horizontalPadding = tileHorizontalPadding,
        verticalPadding = tileVerticalPadding
    )
    Spacer(Modifier.height(actualInnerGroupSpacing))
    SettingsTile(
        title = stringResource(R.string.version),
        subtitle = "v $appVersion" + if (developerModeEnabled) " (Developer)" else "", // developerModeEnabled from SettingsViewModel
        onClick = { viewModel.onInfoTileClicked(context) },
        onLongClick = { viewModel.openImpressum(context) },
        icon = {
            Icon(
                painterResource(R.drawable.info),
                stringResource(R.string.version),
                tint = tileSubtitleColor
            )
        },
        shape = tileShapeOverride ?: bottomShape,
        backgroundColor = tileBackgroundColor,
        contentColor = tileContentColor,
        subtitleColor = tileSubtitleColor,
        horizontalPadding = tileHorizontalPadding,
        verticalPadding = tileVerticalPadding
    )

    // Use developerModeEnabled from SettingsViewModel for controlling visibility of this section
    if (developerModeEnabled) {
        Spacer(Modifier.height(actualOuterGroupSpacing))
        SettingsTile(
            title = stringResource(
                R.string.developer_options_title
            ),
            subtitle = stringResource(
                R.string.dev_settings_description
            ),
            onClick = {
                onNavigateToDeveloperOptions()
            },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.developer),
                    contentDescription = stringResource(R.string.developer_options_title),
                    tint = tileSubtitleColor
                )
            },
            // This tile is now the top of a group of two
            shape = tileShapeOverride ?: topShape, 
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
        Spacer(Modifier.height(actualInnerGroupSpacing)) // Spacer between dev options and pre-release switch
        SettingsSwitchTile(
            title = "Check for pre-releases", // TODO: Replace with stringResource
            subtitle = "Include pre-release versions when checking for updates", // TODO: Replace with stringResource
            checked = checkForPreReleases,
            onCheckedChange = { viewModel.setCheckForPreReleases(it) },
            onClick = { viewModel.setCheckForPreReleases(!checkForPreReleases) },
            icon = {
                Icon(
                    painterResource(R.drawable.developer), // TODO: Replace with actual pre-release/beta icon
                    contentDescription = "Check for pre-releases", // TODO: Replace with stringResource
                    tint = tileSubtitleColor
                )
            },
            // This tile is now the bottom of a group of two
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding,
            switchColors = switchColorsOverride ?: defaultSwitchColors
        )
    }
}

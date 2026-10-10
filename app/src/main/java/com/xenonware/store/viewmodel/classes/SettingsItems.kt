package com.xenonware.store.viewmodel.classes

import android.content.Intent
import androidx.compose.foundation.layout.Column
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
import androidx.core.net.toUri
import com.xenon.mylibrary.R
import com.xenon.mylibrary.res.SettingsGoogleTile
import com.xenon.mylibrary.res.SettingsSwitchMenuTile
import com.xenon.mylibrary.res.SettingsSwitchTile
import com.xenon.mylibrary.res.SettingsTile
import com.xenon.mylibrary.values.ExtraLargerCornerRadius
import com.xenon.mylibrary.values.LargestSpacing
import com.xenon.mylibrary.values.NoCornerRadius
import com.xenon.mylibrary.values.SmallMediumCornerRadius
import com.xenon.mylibrary.values.SmallerSpacing
import com.xenonware.store.R.drawable.pre_release
import com.xenonware.store.R.string
import com.xenonware.store.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.store.presentation.sign_in.SignInState
import com.xenonware.store.viewmodel.SettingsViewModel


@Composable
fun SettingsItems(
    viewModel: SettingsViewModel,
    currentThemeTitle: String,
    applyCoverTheme: Boolean,
    coverThemeEnabled: Boolean,
    currentLanguage: String,
    appVersion: String,
    onNavigateToDeveloperOptions: () -> Unit,
    innerGroupRadius: Dp = SmallMediumCornerRadius,
    outerGroupRadius: Dp = ExtraLargerCornerRadius,
    innerGroupSpacing: Dp = SmallerSpacing,
    outerGroupSpacing: Dp = LargestSpacing,
    tileBackgroundColor: Color = MaterialTheme.colorScheme.surfaceBright,
    tileContentColor: Color = MaterialTheme.colorScheme.onSurface,
    tileSubtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tileShapeOverride: Shape? = null,
    switchColorsOverride: SwitchColors? = null,
    useGroupStyling: Boolean = true,
    state: SignInState,
    googleAuthUiClient: GoogleAuthUiClient,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val blackedOutEnabled by viewModel.blackedOutModeEnabled.collectAsState()
    val developerModeEnabled by viewModel.developerModeEnabled.collectAsState()
    val checkForPreReleases by viewModel.checkForPreReleases.collectAsState()
    val userData by lazy { googleAuthUiClient.getSignedInUser() }

    val actualInnerGroupRadius = if (useGroupStyling) innerGroupRadius else 0.dp
    val actualOuterGroupRadius = if (useGroupStyling) outerGroupRadius else 0.dp
    val actualInnerGroupSpacing = if (useGroupStyling) innerGroupSpacing else 0.dp

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

    SettingsGoogleTile(
        title = if (state.isSignInSuccessful) userData?.username ?: stringResource(string.log_in) else stringResource(string.sign_in_with_google),
        subtitle = if (state.isSignInSuccessful) userData?.email else null,
        profilePictureUrl = userData?.profilePictureUrl,
        noAccIcon = painterResource(R.drawable.ic_default_icon),
        isSignedIn = state.isSignInSuccessful,
        onClick = if (state.isSignInSuccessful) onSignOutClick else onSignInClick,
        shape = tileShapeOverride ?: standaloneShape,
        backgroundColor = Color.Transparent,
        contentColor = tileContentColor,
        subtitleColor = tileSubtitleColor,
        iconContentDescription = stringResource(string.profile_picture)
    )
    Spacer(Modifier.height(outerGroupSpacing))

    Column {
        SettingsTile(
            title = stringResource(string.theme),
            subtitle = "${stringResource(string.current)} $currentThemeTitle",
            onClick = { viewModel.onThemeSettingClicked() },
            icon = { Icon(painterResource(R.drawable.ic_themes), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsSwitchTile(
            title = stringResource(string.blacked_out),
            subtitle = stringResource(string.blacked_out_description),
            checked = blackedOutEnabled,
            onCheckedChange = { viewModel.setBlackedOutEnabled(it) },
            onClick = { viewModel.setBlackedOutEnabled(!blackedOutEnabled) },
            icon = { Icon(painterResource(R.drawable.ic_blacked_out), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            switchColors = switchColorsOverride ?: defaultSwitchColors
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))

        SettingsSwitchMenuTile(
            title = stringResource(string.cover_screen_mode),
            subtitle = "${stringResource(string.selected_cover_screen)}\n(${if (applyCoverTheme) stringResource(string.active) else stringResource(string.inactive)})",
            checked = coverThemeEnabled,
            onCheckedChange = { viewModel.setCoverThemeEnabled(it) },
            onClick = { viewModel.onCoverThemeClicked() },
            icon = { Icon(painterResource(R.drawable.ic_cover_screen), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            switchColors = switchColorsOverride ?: defaultSwitchColors
        )
    }

    Spacer(Modifier.height(outerGroupSpacing))

    Column {
        SettingsTile(
            title = stringResource(string.language),
            subtitle = "${stringResource(string.current)} $currentLanguage",
            onClick = { viewModel.onLanguageSettingClicked(context) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_language),
                    null,
                    tint = tileSubtitleColor
                )
            },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor
        )
        LaunchedEffect(Unit) { viewModel.updateCurrentLanguage() }

        Spacer(Modifier.height(actualInnerGroupSpacing))

        SettingsSwitchTile(
            title = stringResource(string.check_pre_release),
            subtitle = stringResource(string.check_pre_release_describtion),
            checked = checkForPreReleases,
            onCheckedChange = { viewModel.setCheckForPreReleases(it) },
            onClick = { viewModel.setCheckForPreReleases(!checkForPreReleases) },
            icon = {
                Icon(
                    painterResource(pre_release),
                    null,
                    tint = tileSubtitleColor
                )
            },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
//        horizontalPadding = tileHorizontalPadding,
//        verticalPadding = tileVerticalPadding,
            switchColors = switchColorsOverride ?: defaultSwitchColors
        )
    }

    Spacer(Modifier.height(outerGroupSpacing))

    Column {
        SettingsTile(
            title = stringResource(string.clear_data),
            subtitle = stringResource(string.clear_data_description),
            onClick = {
                viewModel.onClearDataClicked(); haptic.performHapticFeedback(
                HapticFeedbackType.LongPress
            )
            },
            icon = { Icon(painterResource(R.drawable.ic_reset), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(string.reset_settings),
            subtitle = stringResource(string.reset_all_settings_description),
            onClick = {
                viewModel.onResetSettingsClicked(); haptic.performHapticFeedback(
                HapticFeedbackType.LongPress
            )
            },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_reset_settings),
                    null,
                    tint = tileSubtitleColor
                )
            },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(string.version),
            subtitle = "v $appVersion" + if (developerModeEnabled) " (${stringResource(string.developer)})" else "",
            onClick = { viewModel.onInfoTileClicked() },
            onLongClick = { viewModel.openImpressum(context) },
            icon = { Icon(painterResource(R.drawable.ic_info), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor
        )
        Spacer(Modifier.height(outerGroupSpacing))
        SettingsTile(
            title = stringResource(string.buy_me_a_coffee),
            subtitle = stringResource(string.buy_me_a_coffee_description, stringResource(string.app_name)),
            onClick = {
                val intent =
                    Intent(Intent.ACTION_VIEW, "https://www.buymeacoffee.com/xenonware".toUri())
                context.startActivity(intent)
            },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_buy_me_a_coffee),
                    null,
                    tint = tileSubtitleColor
                )
            },
            shape = tileShapeOverride ?: standaloneShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            showTrailingIcon = true,
            subtitleColor = tileSubtitleColor
        )
    }

    // --- dev ---
    if (developerModeEnabled) {
        Spacer(Modifier.height(outerGroupSpacing))
        SettingsTile(
            title = stringResource(string.developer_options_title),
            subtitle = stringResource(string.dev_settings_description),
            onClick = onNavigateToDeveloperOptions,
            icon = { Icon(painterResource(R.drawable.ic_developer), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: standaloneShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor
        )
    }
}

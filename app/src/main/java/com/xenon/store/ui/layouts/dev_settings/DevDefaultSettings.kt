package com.xenon.store.ui.layouts.dev_settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenon.store.R
import com.xenon.store.ui.layouts.ActivityScreen
import com.xenon.store.ui.values.LargestPadding
import com.xenon.store.ui.values.MediumPadding
import com.xenon.store.ui.values.NoSpacing
import com.xenon.store.viewmodel.DevSettingsViewModel
import com.xenon.store.viewmodel.LayoutType
import com.xenon.store.viewmodel.classes.DevSettingsItems
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun DevDefaultSettings(
    onNavigateBack: () -> Unit,
    viewModel: DevSettingsViewModel,
    layoutType: LayoutType,
    isLandscape: Boolean,
) {
    val hazeState = rememberHazeState()
    val context = LocalContext.current

    ActivityScreen(
        titleText = stringResource(id = R.string.developer_options_title),

        navigationIconStartPadding = MediumPadding,
        navigationIconPadding = MediumPadding,
        navigationIconSpacing = NoSpacing,
        navigationIconContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.navigate_back_description),
                modifier = Modifier.size(24.dp)
            )
        },
        onNavigationIconClick = onNavigateBack,
        hasNavigationIconExtraContent = false,
        appBarActions = {
            IconButton(onClick = {
                val packageManager = context.packageManager
                val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                val componentName = intent?.component
                val mainIntent = Intent.makeRestartActivityTask(componentName)
                context.startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }) {
                Icon(
                    imageVector = Icons.Filled.RestartAlt,
                    contentDescription = stringResource(R.string.restart_app_description)
                )
            }
        },
        modifier = Modifier.hazeSource(hazeState),
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = LargestPadding,
                        end = LargestPadding,
                        top = LargestPadding,
                        bottom = WindowInsets.safeDrawing
                            .asPaddingValues()
                            .calculateBottomPadding() + LargestPadding
                    )
            ) {
                DevSettingsItems(
                    viewModel = viewModel
                )
            }
        }
    )
}

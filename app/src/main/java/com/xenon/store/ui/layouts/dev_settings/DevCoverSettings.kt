package com.xenon.store.ui.layouts.dev_settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xenon.store.viewmodel.DevSettingsViewModel

@Composable
fun DevCoverSettings(
    onNavigateBack: () -> Unit,
    viewModel: DevSettingsViewModel
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Developer Cover Settings (To be implemented)")
    }
}
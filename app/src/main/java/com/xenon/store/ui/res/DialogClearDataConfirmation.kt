package com.xenon.store.ui.res

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.xenon.store.R
import com.xenon.store.ui.res.XenonDialog


@Composable
fun DialogClearDataConfirmation(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.onErrorContainer

    XenonDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.clear_data_dialog_title),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        dismissIconButtonContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
        dismissIconButtonContentColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
        confirmContainerColor = MaterialTheme.colorScheme.error,
        confirmContentColor = MaterialTheme.colorScheme.onError,
        confirmButtonText = stringResource(R.string.confirm),
        onConfirmButtonClick = { onConfirm() },
        properties = DialogProperties(usePlatformDefaultWidth = true),
        contentManagesScrolling = false,
    ) {
        Text(
            text = stringResource(R.string.clear_data_dialog_description),
            color = textColor
        )
    }
}


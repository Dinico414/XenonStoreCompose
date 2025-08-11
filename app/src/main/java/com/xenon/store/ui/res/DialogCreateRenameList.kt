package com.xenon.store.ui.res

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.DialogProperties
import com.xenon.store.R
import com.xenon.store.ui.res.XenonDialog
import com.xenon.store.ui.res.XenonTextField

@Composable
fun DialogCreateRenameList(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    title: String,
    confirmButtonText: String,
    initialName: String = "",
) {
    if (showDialog) {
        var textState by remember(initialName) { mutableStateOf(initialName) }

        XenonDialog(
            onDismissRequest = onDismiss,
            title = title,
            confirmButtonText = confirmButtonText,
            onConfirmButtonClick = {
                if (textState.isNotBlank()) {
                    onSave(textState)
                }
            },
            isConfirmButtonEnabled = textState.isNotBlank(),
            properties = DialogProperties(usePlatformDefaultWidth = true),
            contentManagesScrolling = false,
        ) {
            XenonTextField(
                value = textState,
                onValueChange = { textState = it },
                placeholder = { Text(stringResource(R.string.list_name_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrectEnabled = true,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
            )
        }
    }
}

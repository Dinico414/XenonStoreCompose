package com.xenonware.store.ui.res

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.res.XenonTextField

@Composable
fun DialogGitHubLogin(
    onDismissRequest: () -> Unit,
    onLogin: (String) -> Unit
) {
    var token by remember { mutableStateOf("") }

    XenonDialog(
        onDismissRequest = onDismissRequest,
        title = "Log in to GitHub",
        confirmButtonText = "Login",
        onConfirmButtonClick = { onLogin(token) },
        properties = DialogProperties(usePlatformDefaultWidth = true),
        contentManagesScrolling = true,
    ) {
        Column {
            Text(
                text = "Enter your GitHub Token to log in and avoid rate limits.",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            XenonTextField(
                value = token,
                onValueChange = { token = it },
                placeholder = { Text("GitHub Token *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true
            )
        }
    }
}

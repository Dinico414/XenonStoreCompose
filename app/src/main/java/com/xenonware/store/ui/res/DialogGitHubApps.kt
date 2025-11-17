package com.xenonware.store.ui.res

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonTextFieldV2
import com.xenonware.store.R

@Composable
fun DialogGitHubApps (
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    owner: String,
    onOwnerChange: (String) -> Unit,
    repo: String,
    onRepoChange: (String) -> Unit,
    packageName: String,
    onPackageNameChange: (String) -> Unit,
    gitHubPAT: String,
    onGitHubPATChange: (String) -> Unit
) {

    XenonDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.add_git_repo),
        confirmButtonText = stringResource(R.string.add),
        onConfirmButtonClick = { onConfirm() },
        properties = DialogProperties(usePlatformDefaultWidth = true),
        contentManagesScrolling = true,
    ) {
        Column {
            XenonTextFieldV2(
                value = owner,
                onValueChange = onOwnerChange,
                placeholder = { Text("Developer / Owner *")},
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            XenonTextFieldV2(
                value = repo,
                onValueChange = onRepoChange,
                placeholder = { Text("Repository *")},
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            XenonTextFieldV2(
                value = packageName,
                onValueChange = onPackageNameChange,
                placeholder = { Text("Package Name *")},
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            XenonTextFieldV2(
                value = gitHubPAT,
                onValueChange = onGitHubPATChange,
                placeholder = { Text("GitHub PAT *")},
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true
            )
        }
    }
}

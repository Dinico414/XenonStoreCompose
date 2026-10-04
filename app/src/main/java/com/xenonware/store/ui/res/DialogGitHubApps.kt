package com.xenonware.store.ui.res

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.rememberCoroutineScope
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.res.XenonTextField
import com.xenonware.store.R
import com.xenonware.store.util.GitHubPackageResolver
import com.xenonware.store.viewmodel.classes.GitHubRepoItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun parseGitHubUrl(input: String): Pair<String, String>? {
    val clean = input.trim()
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("github.com/")
        .removePrefix("www.github.com/")
        .trimEnd('/')

    val parts = clean.split('/')
    if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
        val owner = parts[0]
        val repo = parts[1].removeSuffix(".git")
        return Pair(owner, repo)
    }
    return null
}

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
    searchResults: List<GitHubRepoItem> = emptyList(),
    onSearchQueryChange: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var resolveJob by remember { mutableStateOf<Job?>(null) }

    fun triggerPackageResolution(targetOwner: String, targetRepo: String) {
        if (targetOwner.isBlank() || targetRepo.isBlank()) return
        resolveJob?.cancel()
        resolveJob = coroutineScope.launch {
            delay(250) // debounce
            val resolved = GitHubPackageResolver.resolvePackageName(targetOwner, targetRepo)
            if (resolved.isNotBlank()) {
                onPackageNameChange(resolved)
            }
        }
    }

    fun processInput(input: String) {
        val parsed = parseGitHubUrl(input)
        if (parsed != null) {
            onOwnerChange(parsed.first)
            onRepoChange(parsed.second)
            triggerPackageResolution(parsed.first, parsed.second)
        }
    }

    XenonDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.add_git_repo),
        confirmButtonText = stringResource(R.string.add),
        onConfirmButtonClick = { onConfirm() },
        properties = DialogProperties(usePlatformDefaultWidth = true),
        contentManagesScrolling = true,
    ) {
        Column {
            Text(text = "Search GitHub Repositories or Paste URL", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                XenonTextField(
                    value = searchQuery,
                    onValueChange = { newValue ->
                        searchQuery = newValue
                        processInput(newValue)
                        onSearchQueryChange(newValue)
                    },
                    placeholder = { Text("URL or search (e.g. komi-store/komi-store)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            if (searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)) {
                    LazyColumn {
                        items(searchResults) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onOwnerChange(item.owner.login)
                                        onRepoChange(item.name)
                                        triggerPackageResolution(item.owner.login, item.name)
                                        searchQuery = ""
                                        onSearchQueryChange("")
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(text = item.fullName, style = MaterialTheme.typography.bodyMedium)
                                item.description?.let {
                                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Repository Details", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))

            XenonTextField(
                value = owner,
                onValueChange = { newValue ->
                    processInput(newValue)
                    onOwnerChange(newValue)
                    if (repo.isNotBlank()) {
                        triggerPackageResolution(newValue, repo)
                    }
                },
                placeholder = { Text("GitHub Owner / Username *")},
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            XenonTextField(
                value = repo,
                onValueChange = { newValue ->
                    processInput(newValue)
                    onRepoChange(newValue)
                    if (owner.isNotBlank()) {
                        triggerPackageResolution(owner, newValue)
                    }
                },
                placeholder = { Text("Repository *")},
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            XenonTextField(
                value = packageName,
                onValueChange = onPackageNameChange,
                placeholder = { Text("Package Name (Optional - auto-detected)")},
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true
            )
        }
    }
}

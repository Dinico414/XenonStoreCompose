package com.xenon.store.ui.res

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xenon.store.ui.theme.StoreTheme
import com.xenon.store.viewmodel.classes.AppEntryState
import com.xenon.store.viewmodel.classes.StoreItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreItemCell(
    storeItem: StoreItem,
    onInstall: (StoreItem) -> Unit,
    onUninstall: (StoreItem) -> Unit,
    onOpen: (StoreItem) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val language = configuration.locales[0].language

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val drawableId = storeItem.getDrawableId(context)
                if (drawableId != 0) {
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                }
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(
                        text = storeItem.getName(language),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when (storeItem.state) {
                            AppEntryState.NOT_INSTALLED -> "Version: N/A -> ${storeItem.newVersion}"
                            AppEntryState.INSTALLED, AppEntryState.INSTALLED_AND_OUTDATED -> "Version: ${storeItem.installedVersion}"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED) {
                Text(
                    text = "${storeItem.installedVersion}\n->\n${storeItem.newVersion}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(MaterialTheme.shapes.small)
                        .height(48.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        when (storeItem.state) {
            AppEntryState.DOWNLOADING -> {
                LinearProgressIndicator(
                    progress = { storeItem.bytesDownloaded.toFloat() / storeItem.fileSize.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
            AppEntryState.NOT_INSTALLED -> {
                Button(
                    onClick = { onInstall(storeItem) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = "Install")
                }
            }
            AppEntryState.INSTALLED -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { onOpen(storeItem) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Open")
                    }
                    Button(
                        onClick = { onUninstall(storeItem) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Uninstall")
                    }
                }
            }
            AppEntryState.INSTALLED_AND_OUTDATED -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { onInstall(storeItem) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Update")
                    }
                    Button(
                        onClick = { onOpen(storeItem) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Open")
                    }
                    Button(
                        onClick = { onUninstall(storeItem) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Uninstall")
                    }
                }
            }
        }
    }
}
ght(1f)) {
                            Text(text = "Open")
                        }
                        Button(onClick = { onUninstall(storeItem) }, modifier = Modifier.weight(0.5f)) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Uninstall"
                            )
                        }
                    }
                }
                AppEntryState.INSTALLED_AND_OUTDATED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { onInstall(

@Preview(showBackground = true, name = "Store Item States")
@Composable
private fun StoreItemCellPreview() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Not Installed State
        StoreItemCell(
            storeItem = StoreItem(
                nameMap = hashMapOf("en" to "Not Installed App"),
                packageName = "com.sample.app.notinstalled",
                githubUrl = "...", iconPath = ""
            ).apply {
                state = AppEntryState.NOT_INSTALLED
                installedVersion = "N/A"
                newVersion = "1.0.0"
            },
            onInstall = {}, onUninstall = {}, onOpen = {}
        )

        // Installed State
        StoreItemCell(
            storeItem = StoreItem(
                nameMap = hashMapOf("en" to "Installed App"),
                packageName = "com.sample.app.installed",
                githubUrl = "...", iconPath = ""
            ).apply {
                state = AppEntryState.INSTALLED
                installedVersion = "1.0.0"
                newVersion = "1.0.0"
            },
            onInstall = {}, onUninstall = {}, onOpen = {}
        )

        // Outdated State
        StoreItemCell(
            storeItem = StoreItem(
                nameMap = hashMapOf("en" to "Outdated App"),
                packageName = "com.sample.app.outdated",
                githubUrl = "...", iconPath = ""
            ).apply {
                state = AppEntryState.INSTALLED_AND_OUTDATED
                installedVersion = "1.0.0"
                newVersion = "1.1.0"
            },
            onInstall = {}, onUninstall = {}, onOpen = {}
        )
    }
}
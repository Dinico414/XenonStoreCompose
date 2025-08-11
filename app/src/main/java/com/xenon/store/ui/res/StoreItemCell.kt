package com.xenon.store.ui.res

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xenon.store.viewmodel.classes.AppEntryState
import com.xenon.store.viewmodel.classes.StoreItem

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
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val drawableId = storeItem.getDrawableId(context)
                if (drawableId != 0) {
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(text = storeItem.getName(language))
                    if (storeItem.isOutdated()) {
                        Text(text = "Version: ${storeItem.installedVersion} -> ${storeItem.newVersion}")
                    } else {
                        Text(text = "Version: ${storeItem.installedVersion}")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (storeItem.state) {
                AppEntryState.NOT_INSTALLED -> {
                    Button(onClick = { onInstall(storeItem) }) {
                        Text(text = "Install")
                    }
                }
                AppEntryState.DOWNLOADING -> {
                    LinearProgressIndicator(
                        progress = { storeItem.bytesDownloaded.toFloat() / storeItem.fileSize.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AppEntryState.INSTALLED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpen(storeItem) }) {
                            Text(text = "Open")
                        }
                        Button(onClick = { onUninstall(storeItem) }) {
                            Text(text = "Uninstall")
                        }
                    }
                }
                AppEntryState.INSTALLED_AND_OUTDATED -> {
                    Button(onClick = { onInstall(storeItem) }) {
                        Text(text = "Update")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StoreItemCellPreview() {
    // Create a direct instance of StoreItem with mock data for the preview.
    // Use hashMapOf() to create a HashMap as required by the constructor.
    val mockItem = StoreItem(
        nameMap = hashMapOf("en" to "Sample App", "es" to "Aplicación de Muestra"),
        packageName = "com.sample.app",
        githubUrl = "https://github.com/sample/app",
        iconPath = "" // Assuming the real getDrawableId handles an empty path
    ).apply {
        // Set the mutable state properties for the preview
        state = AppEntryState.NOT_INSTALLED
        installedVersion = "N/A"
        newVersion = "1.0.0"
    }

    StoreItemCell(
        storeItem = mockItem,
        onInstall = {},
        onUninstall = {},
        onOpen = {}
    )
}
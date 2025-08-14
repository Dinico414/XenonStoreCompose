package com.xenon.store.ui.res

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xenon.store.R
import com.xenon.store.ui.values.MediumCornerRadius
import com.xenon.store.ui.values.MediumPadding
import com.xenon.store.util.Util
import com.xenon.store.viewmodel.classes.AppEntryState
import com.xenon.store.viewmodel.classes.StoreItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreItemCell(
    storeItem: StoreItem,
    onInstall: (StoreItem) -> Unit,
    onUninstall: (StoreItem) -> Unit,
    onOpen: (StoreItem) -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val language = Util.getCurrentLanguage(context.resources)

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .weight(1f),
            shape = RoundedCornerShape(MediumCornerRadius)
        ) {
            Column(modifier = Modifier.padding(MediumPadding)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val drawableId = storeItem.getDrawableId(context)
                    Image(
                        painter = if (drawableId != 0) painterResource(id = drawableId)
                        else painterResource(id = R.drawable.blacked_out),
                        contentDescription = storeItem.getName(language) + " icon",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                    Text(
                        text = storeItem.getName(language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (storeItem.state) {
                    AppEntryState.DOWNLOADING -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(
                                progress = {
                                    if (storeItem.fileSize > 0) {
                                        storeItem.bytesDownloaded.toFloat() / storeItem.fileSize.toFloat()
                                    } else {
                                        0f
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (storeItem.fileSize > 0) {
                                    "Downloading: ${storeItem.bytesDownloaded / (1024*1024)}MB / ${storeItem.fileSize / (1024*1024)}MB"
                                } else {
                                    "Downloading..."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    AppEntryState.NOT_INSTALLED -> {
                        Button(
                            onClick = { onInstall(storeItem) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Text(text = "Install")
                        }
                    }

                    AppEntryState.INSTALLED -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Button(
                                onClick = { onOpen(storeItem) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            ) {
                                Text(text = "Open")
                            }
                            OutlinedButton(
                                onClick = { onUninstall(storeItem) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(0.5f)
                                    .height(40.dp)
                            ) {
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
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Button(
                                onClick = { onInstall(storeItem) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            ) {
                                Text(text = "Update")
                            }
                            Button(
                                onClick = { onOpen(storeItem) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            ) {
                                Text(text = "Open")

                            }
                            OutlinedButton(
                                onClick = { onUninstall(storeItem) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(0.75f)
                                    .height(40.dp)
                            ) {
                                Text("Uninstall ${storeItem.installedVersion}")
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Uninstall"
                                )
                            }
                        }
                    }
                }
            }
        }

        if (storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED &&
            storeItem.installedVersion.isNotEmpty() && storeItem.newVersion.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .height(IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${storeItem.installedVersion}->${storeItem.newVersion}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .graphicsLayer(rotationZ = -90f)
                )
            }
        }
    } // End Row
}

@Preview(showBackground = true, name = "Not Installed", widthDp = 380)
@Composable
private fun StoreItemCellPreviewNotInstalled() {
    MaterialTheme {
        StoreItemCell(
            storeItem = StoreItem(
                nameMap = hashMapOf("en" to "Amazing New Application"),
                packageName = "com.sample.app.notinstalled",
                githubUrl = "...",
                iconPath = "" // Will use placeholder
            ).apply {
                state = AppEntryState.NOT_INSTALLED
                newVersion = "1.0.0"
            },
            onInstall = {},
            onUninstall = {},
            onOpen = {}
        )
    }
}

@Preview(showBackground = true, name = "Downloading", widthDp = 380)
@Composable
private fun StoreItemCellPreviewDownloading() {
    MaterialTheme {
        StoreItemCell(
            storeItem = StoreItem(
                nameMap = hashMapOf("en" to "Super Downloader App"),
                packageName = "com.sample.app.downloading",
                githubUrl = "...",
                iconPath = ""
            ).apply {
                state = AppEntryState.DOWNLOADING
                bytesDownloaded = 50 * 1024 * 1024 // 50MB
                fileSize = 100 * 1024 * 1024      // 100MB
                installedVersion = "N/A"
                newVersion = "2.1.0"
            },
            onInstall = {},
            onUninstall = {},
            onOpen = {}
        )
    }
}

@Preview(showBackground = true, name = "Installed", widthDp = 380)
@Composable
private fun StoreItemCellPreviewInstalled() {
    MaterialTheme {
        StoreItemCell(
            storeItem = StoreItem(
                nameMap = hashMapOf("en" to "My Favorite Installed App"),
                packageName = "com.sample.app.installed",
                githubUrl = "...",
                iconPath = ""
            ).apply {
                state = AppEntryState.INSTALLED
                installedVersion = "1.0.0"
                newVersion = "1.0.0" // Or could be empty if no newer version checked/found
            },
            onInstall = {},
            onUninstall = {},
            onOpen = {}
        )
    }
}

@Preview(showBackground = true, name = "Outdated", widthDp = 380)
@Composable
private fun StoreItemCellPreviewOutdated() {
    MaterialTheme {
        StoreItemCell(
            storeItem = StoreItem(
                nameMap = hashMapOf("en" to "Old But Gold App (Update Available!)"),
                packageName = "com.sample.app.outdated",
                githubUrl = "...",
                iconPath = ""
            ).apply {
                state = AppEntryState.INSTALLED_AND_OUTDATED
                installedVersion = "1.0.0"
                newVersion = "1.1.0"
            },
            onInstall = {},
            onUninstall = {},
            onOpen = {}
        )
    }
}

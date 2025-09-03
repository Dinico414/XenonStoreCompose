package com.xenon.store.ui.res

import android.graphics.Paint
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.xenon.store.R
import com.xenon.store.ui.values.MediumCornerRadius
import com.xenon.store.ui.values.MediumPadding
import com.xenon.store.util.Util
import com.xenon.store.viewmodel.classes.AppEntryState
import com.xenon.store.viewmodel.classes.StoreItem

// Helper function to get mipmap name from GitHub URL (fallback)
private fun getRepoMipmapName(githubUrl: String): String {
    return githubUrl.substringAfterLast('/').replace("-", "_").replace(".","").lowercase()
}

// Helper function to parse iconPath like "@mipmap/ic_launcher"
private fun getDrawableIdFromPath(context: android.content.Context, iconPath: String?): Int {
    if (iconPath.isNullOrBlank()) return 0
    val iconRegex = "^@([^/]+)/([^/]+)".toRegex()
    val matchResult = iconRegex.find(iconPath)
    val iconDirectory = matchResult?.groups?.get(1)?.value
    val iconName = matchResult?.groups?.get(2)?.value
    if (iconDirectory == null || iconName == null) return 0
    return context.resources.getIdentifier(iconName, iconDirectory, context.packageName)
}

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

    // Button visibility and text state variables based on old logic
    val showInstallButton = storeItem.state == AppEntryState.NOT_INSTALLED || storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED
    val showOpenUninstallButtons = storeItem.state == AppEntryState.INSTALLED || storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED || (storeItem.state == AppEntryState.DOWNLOADING && storeItem.isOutdated())
    val showProgressBar = storeItem.state == AppEntryState.DOWNLOADING

    val installButtonText = when (storeItem.state) {
        AppEntryState.NOT_INSTALLED -> context.getString(R.string.install)
        AppEntryState.INSTALLED_AND_OUTDATED -> context.getString(R.string.update)
        AppEntryState.DOWNLOADING -> "" // Old code set text to "" for actionButton during download
        else -> context.getString(R.string.install)
    }

    val showVersionInfoHorizontal = storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED || (storeItem.state == AppEntryState.DOWNLOADING && storeItem.isOutdated())

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .weight(1f),
            shape = RoundedCornerShape(MediumCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            )
        ) {
            Column(modifier = Modifier.padding(MediumPadding)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    var iconResId = getDrawableIdFromPath(context, storeItem.iconPath)
                    if (iconResId == 0 && storeItem.githubUrl.isNotBlank()) { // Fallback to githubUrl derived mipmap if iconPath fails
                        val repoMipmapName = getRepoMipmapName(storeItem.githubUrl)
                        if (repoMipmapName.isNotBlank()) {
                            iconResId = context.resources.getIdentifier(repoMipmapName, "mipmap", context.packageName)
                            if (iconResId != 0) {
                                Log.d("StoreItemCell", "Using mipmap '$repoMipmapName' (ID: $iconResId) from GitHub URL for ${storeItem.packageName}")
                            } else {
                                Log.w("StoreItemCell", "Fallback Mipmap '$repoMipmapName' from GitHub URL not found for ${storeItem.packageName}.")
                            }
                        }
                    }

                    if (iconResId != 0) {
                        AndroidView(
                            factory = { ctx -> ImageView(ctx) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.small),
                            update = { imageView ->
                                try {
                                    val drawable = ResourcesCompat.getDrawable(context.resources, iconResId, null)
                                    imageView.setImageDrawable(drawable)
                                    imageView.visibility = View.VISIBLE
                                } catch (e: Exception) {
                                    Log.e("StoreItemCell", "Error loading drawable ID: $iconResId for ${storeItem.packageName}", e)
                                    imageView.visibility = View.GONE // Hide if error
                                }
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                        Log.w("StoreItemCell", "No valid icon resource found for ${storeItem.packageName}. Displaying Spacer.")
                    }

                    Spacer(modifier = Modifier.width(MediumPadding))

                    Text(
                        text = storeItem.getName(language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (showVersionInfoHorizontal && storeItem.newVersion.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (storeItem.installedVersion.isNotEmpty()) {
                            Text(
                                text = context.getString(R.string.version_with_prefix, storeItem.installedVersion),
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = context.getString(R.string.version_with_prefix, storeItem.newVersion),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showProgressBar) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = {
                                if (storeItem.fileSize > 0) {
                                    storeItem.bytesDownloaded.toFloat() / storeItem.fileSize.toFloat()
                                } else {
                                    0f // Indeterminate if no size
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
                                "Downloading: ${storeItem.bytesDownloaded / (1024 * 1024)}MB / ${storeItem.fileSize / (1024 * 1024)}MB"
                            } else {
                                "Downloading..."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Button layout mimicking old logic
                val mainActionButtonVisible = storeItem.state == AppEntryState.NOT_INSTALLED ||
                                          storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED ||
                                          storeItem.state == AppEntryState.DOWNLOADING

                val openAndUninstallRowVisible = storeItem.state == AppEntryState.INSTALLED ||
                                               storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED ||
                                               (storeItem.state == AppEntryState.DOWNLOADING && storeItem.isOutdated())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (mainActionButtonVisible && openAndUninstallRowVisible) Arrangement.spacedBy(4.dp) else Arrangement.Center
                ) {
                    if (mainActionButtonVisible) {
                        Button(
                            onClick = { onInstall(storeItem) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .then(if (openAndUninstallRowVisible) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                .height(40.dp),
                            enabled = storeItem.state != AppEntryState.DOWNLOADING // Disabled while downloading, text is already empty
                        ) {
                            if (installButtonText.isNotEmpty()) { // Only show text if not downloading
                                Text(text = installButtonText)
                            }
                        }
                    }

                    if (openAndUninstallRowVisible) {
                        // This Spacer is used to mimic the marginStart for buttonLayout in the old XML
                        if (mainActionButtonVisible) {
                           // Spacer is handled by Arrangement.spacedBy if both are visible
                        } else {
                            // If only Open/Uninstall is visible, no extra space needed as Row is centered
                        }

                        Button(
                            onClick = { onOpen(storeItem) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text(text = context.getString(R.string.open))
                        }
                        Spacer(modifier = Modifier.width(4.dp)) // Spacing between Open and Uninstall
                        OutlinedButton(
                            onClick = { onUninstall(storeItem) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .width(72.dp) // As per old layout, uninstall is smaller
                                .height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = context.getString(R.string.uninstall)
                            )
                        }
                    }
                }
            }
        }

        // Vertical version display (original from Compose)
        // This is kept if the item is outdated, but horizontal one above is more like old XML
        if (storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED &&
            storeItem.installedVersion.isNotEmpty() && storeItem.newVersion.isNotEmpty() && !showVersionInfoHorizontal) {
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

// Dummy StoreItem for Preview, assuming iconPath exists
@Preview(showBackground = true, name = "Not Installed", widthDp = 380)
@Composable
private fun StoreItemCellPreviewNotInstalled() {
    MaterialTheme {
        StoreItemCell(
            storeItem = StoreItem(
                nameMap = hashMapOf("en" to "Amazing New Application"),
                packageName = "com.sample.app.notinstalled",
                githubUrl = "Dinico414/Xenon-App",
                iconPath = "@mipmap/ic_launcher" // Example iconPath
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
                githubUrl = "Dinico414/downloader",
                iconPath = "@mipmap/ic_launcher_round"
            ).apply {
                state = AppEntryState.DOWNLOADING
                bytesDownloaded = 50 * 1024 * 1024 // 50MB
                fileSize = 100 * 1024 * 1024      // 100MB
                installedVersion = "1.0.0" // For isOutdated() check during download
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
                githubUrl = "User/My-Favorite-App.Repo",
                iconPath = "@drawable/xenon_icon" // Example drawable path
            ).apply {
                state = AppEntryState.INSTALLED
                installedVersion = "1.0.0"
                newVersion = "1.0.0"
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
                githubUrl = "",
                iconPath = "@mipmap/ic_launcher" 
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

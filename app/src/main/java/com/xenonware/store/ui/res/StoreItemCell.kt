@file:Suppress("DEPRECATION")

package com.xenonware.store.ui.res

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.outlinedButtonBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.xenon.mylibrary.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenon.mylibrary.values.MediumPadding
import com.xenonware.store.R
import com.xenonware.store.utils.Utils
import com.xenonware.store.viewmodel.classes.AppEntryState
import com.xenonware.store.viewmodel.classes.StoreItem
import androidx.compose.ui.res.stringResource

private fun Dp.toPx(context: android.content.Context): Float {
    return this.value * context.resources.displayMetrics.density
}


private fun getRepoOwner(githubUrl: String): String {
    val urlParts = githubUrl.trimEnd('/').split('/')
    return urlParts.getOrNull(urlParts.size - 2) ?: ""
}

private fun getRepoMipmapName(githubUrl: String): String {
    return githubUrl.substringAfterLast('/').replace("-", "_").replace(".", "").lowercase()
}

@SuppressLint("DiscouragedApi")
private fun getDrawableIdFromPath(context: android.content.Context, iconPath: String?): Int {
    if (iconPath.isNullOrBlank()) return 0
    val iconRegex = "^@([^/]+)/([^/]+)".toRegex()
    val matchResult = iconRegex.find(iconPath)
    val iconDirectory = matchResult?.groups?.get(1)?.value
    val iconName = matchResult?.groups?.get(2)?.value
    if (iconDirectory == null || iconName == null) return 0
    return context.resources.getIdentifier(iconName, iconDirectory, context.packageName)
}

@SuppressLint("DiscouragedApi", "LocalContextResourcesRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreItemCell(
    storeItem: StoreItem,
    onInstall: (StoreItem) -> Unit,
    onUninstall: (StoreItem) -> Unit,
    onOpen: (StoreItem) -> Unit,
) {
    val context = LocalContext.current
    val language = Utils.getCurrentLanguage(context.resources)

    val installButtonText = when (storeItem.state) {
        AppEntryState.NOT_INSTALLED -> stringResource(R.string.install)
        AppEntryState.INSTALLED_AND_OUTDATED -> stringResource(R.string.update)
        AppEntryState.INSTALLING -> {
            if (storeItem.installedVersion.isNotEmpty()) stringResource(R.string.update)
            else stringResource(R.string.install)
        }

        else -> stringResource(R.string.install)
    }

    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(MediumCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    MediumPadding
                )
            ) {
                var isExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    var iconResId = getDrawableIdFromPath(context, storeItem.iconPath)
                    var usePlaceholderBorder by remember { mutableStateOf(false) }

                    if (iconResId == 0 && storeItem.githubUrl.isNotBlank()) {
                        val repoMipmapName = getRepoMipmapName(storeItem.githubUrl)
                        if (repoMipmapName.isNotBlank()) {
                            iconResId = context.resources.getIdentifier(
                                repoMipmapName, "mipmap", context.packageName
                            )
                            if (iconResId != 0) {
                                Log.d(
                                    "StoreItemCell",
                                    "Using mipmap '$repoMipmapName' (ID: $iconResId) from GitHub URL for ${storeItem.packageName}"
                                )
                            } else {
                                Log.w(
                                    "StoreItemCell",
                                    "Fallback Mipmap '$repoMipmapName' from GitHub URL not found for ${storeItem.packageName}. Trying placeholder."
                                )

                                iconResId = context.resources.getIdentifier(
                                    "placeholder", "mipmap", context.packageName
                                )
                                if (iconResId != 0) {
                                    usePlaceholderBorder =
                                        true
                                    Log.i(
                                        "StoreItemCell",
                                        "Using placeholder mipmap for ${storeItem.packageName}"
                                    )
                                } else {
                                    Log.e(
                                        "StoreItemCell",
                                        "Placeholder mipmap also not found for ${storeItem.packageName}."
                                    )
                                }
                            }
                        }
                    }

                    val iconModifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .then(
                            if (usePlaceholderBorder) Modifier.border(
                                0.5.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                RoundedCornerShape(16.dp)
                            ) else Modifier
                        )

                    if (iconResId != 0) {
                        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") AndroidView(factory = { ctx ->
                            ImageView(ctx).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                        }, modifier = iconModifier, update = {
                            try {
                                val originalDrawable = ResourcesCompat.getDrawable(
                                    context.resources, iconResId, null
                                )
                                if (originalDrawable is AdaptiveIconDrawable) {
                                    val iconSizePx = 48.dp.toPx(context).toInt()
                                    if (iconSizePx > 0) {
                                        val bitmap = createBitmap(iconSizePx, iconSizePx)
                                        val canvas = Canvas(bitmap)

                                        val scaleFactor = 1.5f
                                        val scaledWidth = iconSizePx * scaleFactor
                                        val scaledHeight = iconSizePx * scaleFactor

                                        val offsetWidth = (scaledWidth - iconSizePx) / 2f
                                        val offsetHeight = (scaledHeight - iconSizePx) / 2f

                                        val scaledLeft = (-offsetWidth).toInt()
                                        val scaledTop = (-offsetHeight).toInt()
                                        val scaledRight = (iconSizePx + offsetWidth).toInt()
                                        val scaledBottom = (iconSizePx + offsetHeight).toInt()

                                        originalDrawable.background?.let {
                                            it.setBounds(
                                                scaledLeft, scaledTop, scaledRight, scaledBottom
                                            )
                                            it.draw(canvas)
                                        }
                                        originalDrawable.foreground?.let {
                                            it.setBounds(
                                                scaledLeft, scaledTop, scaledRight, scaledBottom
                                            )
                                            it.draw(canvas)
                                        }
                                        it.setImageBitmap(bitmap)
                                        it.visibility = View.VISIBLE
                                    } else {
                                        Log.w(
                                            "StoreItemCell",
                                            "ImageView dimensions for ${storeItem.packageName} are invalid ($iconSizePx x $iconSizePx), falling back to direct drawable."
                                        )
                                        it.setImageDrawable(originalDrawable)
                                        it.visibility = View.VISIBLE
                                    }
                                } else {
                                    it.setImageDrawable(originalDrawable)
                                    it.visibility = View.VISIBLE
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    "StoreItemCell",
                                    "Error loading drawable ID: $iconResId for ${storeItem.packageName}",
                                    e
                                )
                                it.visibility = View.GONE
                            }
                        })
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                        Log.w(
                            "StoreItemCell",
                            "No valid icon resource found for ${storeItem.packageName}. Displaying Spacer."
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(
                            LargestPadding
                        )
                    )


                    Text(
                        text = storeItem.getName(language),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = QuicksandTitleVariable,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    val isUpdateAvailable =
                        storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED || (storeItem.state == AppEntryState.DOWNLOADING && storeItem.isOutdated()) || (storeItem.state == AppEntryState.INSTALLING && storeItem.isOutdated())

                    IconButton(
                        onClick = { isExpanded = !isExpanded }, colors = if (isUpdateAvailable) {
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        } else {
                            IconButtonDefaults.iconButtonColors()
                        }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "More info"
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isUpdateAvailable =
                            storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED || (storeItem.state == AppEntryState.DOWNLOADING && storeItem.isOutdated()) || (storeItem.state == AppEntryState.INSTALLING && storeItem.isOutdated())
                        if (storeItem.githubUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = getRepoOwner(storeItem.githubUrl),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isUpdateAvailable && storeItem.newVersion.isNotEmpty()) {
                                if (storeItem.installedVersion.isNotEmpty()) {
                                    Text(
                                        text = stringResource(
                                            R.string.version_with_prefix, storeItem.installedVersion
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        textDecoration = TextDecoration.LineThrough,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = ">>"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = stringResource(
                                        R.string.version_without_prefix, storeItem.newVersion
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (storeItem.installedVersion.isNotEmpty()) {
                                Text(
                                    text = stringResource(
                                        R.string.version_with_prefix, storeItem.installedVersion
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (storeItem.newVersion.isNotEmpty()) {
                                Text(
                                    text = stringResource(
                                        R.string.version_with_prefix, storeItem.newVersion
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val mainActionButtonVisible =
                    storeItem.state == AppEntryState.NOT_INSTALLED || storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED || storeItem.state == AppEntryState.DOWNLOADING || storeItem.state == AppEntryState.INSTALLING

                val openAndUninstallRowVisible =
                    storeItem.state == AppEntryState.INSTALLED || storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED || (storeItem.installedVersion.isNotEmpty() && (storeItem.state == AppEntryState.DOWNLOADING || storeItem.state == AppEntryState.INSTALLING))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mainActionButtonVisible) {
                        Button(
                            onClick = {
                                if (storeItem.state == AppEntryState.NOT_INSTALLED || storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED) {
                                    onInstall(storeItem)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(if (openAndUninstallRowVisible) 0.5f else 1f)
                                .height(40.dp),
                            enabled = storeItem.state == AppEntryState.NOT_INSTALLED || storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED,
                            contentPadding = if (storeItem.state == AppEntryState.DOWNLOADING) PaddingValues(
                                0.dp
                            ) else ButtonDefaults.ContentPadding
                        ) {
                            when (storeItem.state) {
                                AppEntryState.DOWNLOADING -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val progress = if (storeItem.fileSize > 0) {
                                            (storeItem.bytesDownloaded.toFloat() / storeItem.fileSize.toFloat()).coerceIn(
                                                0f, 1f
                                            )
                                        } else {
                                            0f
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        ) {
                                            val isDarkTheme = isSystemInDarkTheme()
                                            val tertiaryColor = MaterialTheme.colorScheme.tertiary
                                            val adjustedTertiaryColor = if (isDarkTheme) {
                                                tertiaryColor.copy(
                                                    red = (tertiaryColor.red + 0.25f).coerceAtMost(
                                                        1f
                                                    ),
                                                    green = (tertiaryColor.green + 0.25f).coerceAtMost(
                                                        1f
                                                    ),
                                                    blue = (tertiaryColor.blue + 0.25f).coerceAtMost(
                                                        1f
                                                    )
                                                )
                                            } else {
                                                tertiaryColor.copy(
                                                    red = tertiaryColor.red * 0.75f,
                                                    green = tertiaryColor.green * 0.75f,
                                                    blue = tertiaryColor.blue * 0.75f
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(fraction = progress)
                                                    .background(
                                                        brush = Brush.horizontalGradient(
                                                            colors = listOf(
                                                                tertiaryColor, adjustedTertiaryColor
                                                            )
                                                        ), shape = RoundedCornerShape(12.dp)
                                                    )
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    Text(text = installButtonText)
                                }
                            }
                        }
                    }

                    if (openAndUninstallRowVisible) {
                        Row(
                            modifier = Modifier.weight(if (mainActionButtonVisible) 0.5f else 1f),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Button(
                                onClick = { onOpen(storeItem) },
                                shape = RoundedCornerShape(
                                    bottomStart = 16.dp,
                                    topStart = 16.dp,
                                    topEnd = 4.dp,
                                    bottomEnd = 4.dp
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                enabled = storeItem.state == AppEntryState.INSTALLED || storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED
                            ) {
                                Text(text = stringResource(R.string.open))
                            }

                            Box(
                                modifier = Modifier.width(52.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = { onUninstall(storeItem) },
                                    modifier = Modifier
                                        .width(52.dp)
                                        .height(40.dp),
                                    enabled = storeItem.state == AppEntryState.INSTALLED || storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED,
                                    shape = RoundedCornerShape(
                                        bottomStart = 4.dp,
                                        topStart = 4.dp,
                                        topEnd = 16.dp,
                                        bottomEnd = 16.dp
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    border = outlinedButtonBorder.copy(
                                        brush = SolidColor(MaterialTheme.colorScheme.primary)
                                    )
                                ) {}
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.uninstall),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}


@Preview(showBackground = true, name = "Not Installed", widthDp = 380)
@Composable
private fun StoreItemCellPreviewNotInstalled() {
    MaterialTheme {
        StoreItemCell(
            storeItem = StoreItem(
            nameMap = hashMapOf("en" to "Amazing New Application"),
            packageName = "com.sample.app.notinstalled",
            githubUrl = "Dinico414/Xenon-App",
            iconPath = "@mipmap/ic_launcher"
        ).apply {
            state = AppEntryState.NOT_INSTALLED
            newVersion = "1.0.0"
            fileSize = 10 * 1024 * 1024
        }, onInstall = {}, onUninstall = {}, onOpen = {})
    }
}

@Preview(showBackground = true, name = "Downloading New", widthDp = 380)
@Composable
private fun StoreItemCellPreviewDownloadingNew() {
    MaterialTheme {
        StoreItemCell(
            storeItem = StoreItem(
            nameMap = hashMapOf("en" to "Super Downloader App"),
            packageName = "com.sample.app.downloadingnew",
            githubUrl = "Dinico414/downloader",
            iconPath = "@mipmap/ic_launcher_round"
        ).apply {
            state = AppEntryState.DOWNLOADING
            bytesDownloaded = 50 * 1024 * 1024
            fileSize = 100 * 1024 * 1024
            newVersion = "2.1.0"
        }, onInstall = {}, onUninstall = {}, onOpen = {})
    }
}

@Preview(showBackground = true, name = "Downloading Update", widthDp = 380)
@Composable
private fun StoreItemCellPreviewDownloadingUpdate() {
    MaterialTheme {
        StoreItemCell(
            storeItem = StoreItem(
            nameMap = hashMapOf("en" to "My Awesome App (Updating)"),
            packageName = "com.sample.app.downloadingupdate",
            githubUrl = "Dinico414/updater",
            iconPath = "@mipmap/ic_launcher_round"
        ).apply {
            state = AppEntryState.DOWNLOADING
            installedVersion = "1.0.0"
            newVersion = "1.1.0"
            bytesDownloaded = 30 * 1024 * 1024
            fileSize = 60 * 1024 * 1024
        }, onInstall = {}, onUninstall = {}, onOpen = {})
    }
}

@Preview(showBackground = true, name = "Installing New", widthDp = 380)
@Composable
private fun StoreItemCellPreviewInstallingNew() {
    MaterialTheme {
        StoreItemCell(
            storeItem = StoreItem(
            nameMap = hashMapOf("en" to "Fantastic Installer (New)"),
            packageName = "com.sample.app.installingnew",
            githubUrl = "Dinico414/installer",
            iconPath = "@mipmap/ic_launcher"
        ).apply {
            state = AppEntryState.INSTALLING
            newVersion = "1.0.0"
            fileSize = 25 * 1024 * 1024
        }, onInstall = {}, onUninstall = {}, onOpen = {})
    }
}

@Preview(showBackground = true, name = "Installing Update", widthDp = 380)
@Composable
private fun StoreItemCellPreviewInstallingUpdate() {
    MaterialTheme {
        StoreItemCell(
            storeItem = StoreItem(
            nameMap = hashMapOf("en" to "Fantastic Installer (Update)"),
            packageName = "com.sample.app.installingupdate",
            githubUrl = "Dinico414/installer",
            iconPath = "@mipmap/ic_launcher"
        ).apply {
            state = AppEntryState.INSTALLING
            installedVersion = "1.0.0"
            newVersion = "1.1.0"
            fileSize = 15 * 1024 * 1024
        }, onInstall = {}, onUninstall = {}, onOpen = {})
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
            iconPath = "@drawable/xenon_icon"
        ).apply {
            state = AppEntryState.INSTALLED
            installedVersion = "1.0.0"
            newVersion = "1.0.0"
            fileSize = 50 * 1024 * 1024
        }, onInstall = {}, onUninstall = {}, onOpen = {})
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
            fileSize = 20 * 1024 * 1024
        }, onInstall = {}, onUninstall = {}, onOpen = {})
    }
}
@file:Suppress("DEPRECATION")

package com.xenonware.store.ui.res

import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.outlinedButtonBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import coil.compose.AsyncImage
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.ExtraLargerCornerRadius
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenonware.store.R
import com.xenonware.store.util.Util
import com.xenonware.store.viewmodel.classes.AppEntryState
import com.xenonware.store.viewmodel.classes.StoreItem

/**
 * Converts a [Dp] value to raw pixels based on device display density.
 *
 * @param context The [android.content.Context] used to obtain display metrics.
 * @return The value in pixels as a [Float].
 */
private fun Dp.toPx(context: android.content.Context): Float {
    return this.value * context.resources.displayMetrics.density
}

/**
 * Extracts the repository owner/organization name from a GitHub repository URL.
 *
 * @param githubUrl The full or partial GitHub URL string.
 * @return The owner name, or an empty string if URL formatting is unparseable.
 */
private fun getRepoOwner(githubUrl: String): String {
    val urlParts = githubUrl.trimEnd('/').split('/')
    return urlParts.getOrNull(urlParts.size - 2) ?: ""
}

/**
 * Converts a GitHub repository URL into a normalized resource/mipmap name.
 * Hyphens are replaced with underscores, dots are removed, and characters are lowercased.
 *
 * @param githubUrl The GitHub URL string.
 * @return The formatted resource name suitable for resource lookup.
 */
private fun getRepoMipmapName(githubUrl: String): String {
    return githubUrl.substringAfterLast('/').replace("-", "_").replace(".", "").lowercase()
}

/**
 * Resolves a drawable or mipmap resource identifier from an explicit string path reference (e.g. "@mipmap/ic_launcher").
 *
 * @param context The [android.content.Context] used to resolve resources.
 * @param iconPath Resource reference string formatted as "@defType/defName".
 * @return Resolved resource ID integer, or 0 if resolution fails.
 */
private fun getDrawableIdFromPath(context: android.content.Context, iconPath: String?): Int {
    if (iconPath.isNullOrBlank()) return 0
    val iconRegex = "^@([^/]+)/([^/]+)".toRegex()
    val matchResult = iconRegex.find(iconPath)
    val iconDirectory = matchResult?.groups?.get(1)?.value
    val iconName = matchResult?.groups?.get(2)?.value
    if (iconDirectory == null || iconName == null) return 0
    return context.resources.getIdentifier(iconName, iconDirectory, context.packageName)
}

/**
 * AndroidView wrapper composable to render custom [Drawable] objects, including support for
 * [AdaptiveIconDrawable] instances with scaled background and foreground layer composition.
 *
 * @param drawable The [Drawable] instance to display.
 * @param modifier [Modifier] applied to this view.
 */
@Composable
private fun DrawableIconView(
    drawable: Drawable,
    modifier: Modifier
) {
    val context = LocalContext.current
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        modifier = modifier,
        update = { imageView ->
            try {
                if (drawable is AdaptiveIconDrawable) {
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

                        drawable.background?.let {
                            it.setBounds(
                                scaledLeft, scaledTop, scaledRight, scaledBottom
                            )
                            it.draw(canvas)
                        }
                        drawable.foreground?.let {
                            it.setBounds(
                                scaledLeft, scaledTop, scaledRight, scaledBottom
                            )
                            it.draw(canvas)
                        }
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                    } else {
                        imageView.setImageDrawable(drawable)
                        imageView.visibility = View.VISIBLE
                    }
                } else {
                    imageView.setImageDrawable(drawable)
                    imageView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("StoreItemCell", "Error rendering drawable", e)
                imageView.visibility = View.GONE
            }
        }
    )
}

/**
 * Card composable representing a single application item in the Xenon Store.
 *
 * Displays:
 * - Application icon (installed icon, HTTP image, or embedded resource)
 * - Application title and owner details
 * - Expandable version info comparison
 * - Action controls (Install / Update / Download Progress / Cancel Download / Open / Uninstall)
 *
 * During active downloading, a Cancel Download button matching the size of the uninstall button (52.dp width x 40.dp height)
 * is displayed next to the progress bar.
 *
 * @param storeItem The [StoreItem] data model.
 * @param isOnline Whether network connectivity is available.
 * @param onInstall Callback invoked to initiate app installation or update.
 * @param onUninstall Callback invoked to initiate app uninstallation.
 * @param onOpen Callback invoked to launch the installed application.
 * @param onCancelDownload Callback invoked to cancel an ongoing download.
 */
@Composable
fun StoreItemCell(
    storeItem: StoreItem,
    isOnline: Boolean = true,
    onInstall: (StoreItem) -> Unit,
    onUninstall: (StoreItem) -> Unit,
    onOpen: (StoreItem) -> Unit,
    onCancelDownload: (StoreItem) -> Unit = {},
) {
    val context = LocalContext.current
    val language = Util.getCurrentLanguage(context.resources)

    val installButtonText = when (storeItem.state) {
        AppEntryState.NOT_INSTALLED -> {
            if (storeItem.isDownloaded) stringResource(R.string.install)
            else stringResource(R.string.download)
        }
        AppEntryState.INSTALLED_AND_OUTDATED -> {
            if (storeItem.isDownloaded) stringResource(R.string.install)
            else stringResource(R.string.update)
        }
        AppEntryState.INSTALLING -> {
            if (storeItem.installedVersion.isNotEmpty()) stringResource(R.string.update)
            else stringResource(R.string.install)
        }
        else -> stringResource(R.string.install)
    }

    val installedAppIcon: Drawable? = remember(storeItem.packageName, storeItem.installedVersion) {
        if (storeItem.installedVersion.isNotEmpty()) {
            try {
                context.packageManager.getApplicationIcon(storeItem.packageName)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(ExtraLargerCornerRadius),
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
                    val isHttpIcon = storeItem.iconPath.startsWith("http://") || storeItem.iconPath.startsWith("https://")
                    var iconResId = if (!isHttpIcon) getDrawableIdFromPath(context, storeItem.iconPath) else 0
                    var usePlaceholderBorder by remember { mutableStateOf(false) }

                    if (!isHttpIcon && iconResId == 0 && storeItem.githubUrl.isNotBlank()) {
                        val repoMipmapName = getRepoMipmapName(storeItem.githubUrl)
                        if (repoMipmapName.isNotBlank()) {
                            iconResId = context.resources.getIdentifier(
                                repoMipmapName, "mipmap", context.packageName
                            )
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

                    if (installedAppIcon != null) {
                        DrawableIconView(
                            drawable = installedAppIcon,
                            modifier = iconModifier
                        )
                    } else if (isHttpIcon) {
                        AsyncImage(
                            model = storeItem.iconPath,
                            contentDescription = storeItem.getName(language),
                            modifier = iconModifier,
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.default_icon),
                            placeholder = painterResource(id = R.drawable.default_icon)
                        )
                    } else if (iconResId != 0) {
                        val resDrawable = remember(iconResId) {
                            try {
                                ResourcesCompat.getDrawable(context.resources, iconResId, null)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (resDrawable != null) {
                            DrawableIconView(
                                drawable = resDrawable,
                                modifier = iconModifier
                            )
                        } else {
                            AsyncImage(
                                model = R.drawable.default_icon,
                                contentDescription = storeItem.getName(language),
                                modifier = iconModifier,
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else if (storeItem.isCustom && storeItem.githubUrl.isNotBlank()) {
                        val owner = getRepoOwner(storeItem.githubUrl)
                        AsyncImage(
                            model = "https://github.com/$owner.png",
                            contentDescription = storeItem.getName(language),
                            modifier = iconModifier,
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.default_icon),
                            placeholder = painterResource(id = R.drawable.default_icon)
                        )
                    } else {
                        AsyncImage(
                            model = R.drawable.default_icon,
                            contentDescription = storeItem.getName(language),
                            modifier = iconModifier,
                            contentScale = ContentScale.Crop
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
                        onClick = { isExpanded = !isExpanded },
                        colors = if (isUpdateAvailable) {
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        } else if (storeItem.isCustom) {
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (storeItem.githubUrl.isNotBlank()) {
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

                            if (storeItem.isCustom && storeItem.githubUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledTonalIconButton(
                                    onClick = {
                                        val repoUrl = if (storeItem.githubUrl.startsWith("http://") || storeItem.githubUrl.startsWith("https://")) {
                                            storeItem.githubUrl
                                        } else {
                                            "https://github.com/${storeItem.githubUrl}"
                                        }
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.e("StoreItemCell", "Failed to open repository URL: $repoUrl", e)
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.OpenInNew,
                                        contentDescription = stringResource(R.string.open_repository),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val mainActionButtonVisible =
                    storeItem.state == AppEntryState.NOT_INSTALLED || storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED || storeItem.state == AppEntryState.DOWNLOADING || storeItem.state == AppEntryState.INSTALLING

                val openAndUninstallRowVisible =
                    storeItem.state != AppEntryState.DOWNLOADING && (storeItem.state == AppEntryState.INSTALLED || storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED || (storeItem.installedVersion.isNotEmpty() && storeItem.state == AppEntryState.INSTALLING))

                val canPerformMainAction = storeItem.isDownloaded || isOnline

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
                            enabled = canPerformMainAction && (storeItem.state == AppEntryState.NOT_INSTALLED || storeItem.state == AppEntryState.INSTALLED_AND_OUTDATED),
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

                    if (storeItem.state == AppEntryState.DOWNLOADING) {
                        OutlinedButton(
                            onClick = { onCancelDownload(storeItem) },
                            modifier = Modifier
                                .width(52.dp)
                                .height(40.dp),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(0.dp),
                            border = outlinedButtonBorder.copy(
                                brush = SolidColor(MaterialTheme.colorScheme.error)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cancel),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
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
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
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

/**
 * Preview for [StoreItemCell] in the NOT_INSTALLED state.
 */
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
            }, onInstall = {}, onUninstall = {}, onOpen = {}, onCancelDownload = {})
    }
}

/**
 * Preview for [StoreItemCell] in the DOWNLOADING state for a new installation,
 * displaying the progress bar and the Cancel Download button.
 */
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
            }, onInstall = {}, onUninstall = {}, onOpen = {}, onCancelDownload = {})
    }
}

/**
 * Preview for [StoreItemCell] in the DOWNLOADING state for an application update,
 * displaying the progress bar and the Cancel Download button.
 */
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
            }, onInstall = {}, onUninstall = {}, onOpen = {}, onCancelDownload = {})
    }
}

/**
 * Preview for [StoreItemCell] in the INSTALLING state for a new app.
 */
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
            }, onInstall = {}, onUninstall = {}, onOpen = {}, onCancelDownload = {})
    }
}

/**
 * Preview for [StoreItemCell] in the INSTALLING state for an update.
 */
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
            }, onInstall = {}, onUninstall = {}, onOpen = {}, onCancelDownload = {})
    }
}

/**
 * Preview for [StoreItemCell] in the INSTALLED state.
 */
@Preview(showBackground = true, name = "Installed", widthDp = 380)
@Composable
private fun StoreItemCellPreviewInstalled() {
    MaterialTheme {
        StoreItemCell(
            storeItem = StoreItem(
                nameMap = hashMapOf("en" to "My Favorite Installed App"),
                packageName = "com.sample.app.installed",
                githubUrl = "User/My-Favorite-App.Repo",
                iconPath = "@drawable/xenon_icon",
                isCustom = true
            ).apply {
                state = AppEntryState.INSTALLED
                installedVersion = "1.0.0"
                newVersion = "1.0.0"
                fileSize = 50 * 1024 * 1024
            }, onInstall = {}, onUninstall = {}, onOpen = {}, onCancelDownload = {})
    }
}

/**
 * Preview for [StoreItemCell] in the INSTALLED_AND_OUTDATED state.
 */
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
            }, onInstall = {}, onUninstall = {}, onOpen = {}, onCancelDownload = {})
    }
}

package com.xenon.store.ui.res

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.xenon.store.R
import com.xenon.store.ui.layouts.QuicksandTitleVariable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Composable
fun DialogShareSelector(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    XenonDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.share_store_title),
        properties = DialogProperties(usePlatformDefaultWidth = true),
        contentManagesScrolling = true,
        content = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    ShareOptionBox(
                        modifier = Modifier.weight(1f),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Link,
                                contentDescription = stringResource(R.string.share_via_link),
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = stringResource(R.string.share_via_link),
                        subtitle = "",
                        onClick = {
                            shareLink(context)
                            onDismissRequest()
                        }
                    )

                    ShareOptionBox(
                        modifier = Modifier.weight(1f),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.InsertDriveFile,
                                contentDescription = stringResource(R.string.share_via_file),
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = stringResource(R.string.share_via_file),
                        subtitle = stringResource(R.string.recommended),
                        onClick = {
                            shareFile(context)
                            onDismissRequest()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    )
}

@Composable
fun ShareOptionBox(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(150.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(4.dp)),
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontFamily = QuicksandTitleVariable,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = QuicksandTitleVariable,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Thin
                )
            }
        }
    }
}

private fun shareFile(context: Context) {
    try {
        val assetManager = context.assets
        val apkFileName = "XenonStoreInstaller.apk"
        val inputStream = assetManager.open(apkFileName)
        val file = File(context.cacheDir, apkFileName)
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.flush()
        outputStream.close()

        val uri = FileProvider.getUriForFile(
            context, context.packageName + ".provider", file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(
                shareIntent, context.getString(R.string.share_store_title)
            )
        )
    } catch (e: IOException) {
        Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }
}

private fun shareLink(context: Context) {
    val storeUrl = "https://xenonware.com"
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, storeUrl)
    }
    context.startActivity(
        Intent.createChooser(
            shareIntent, context.getString(R.string.share_store_title)
        )
    )
}


package com.xenon.store.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.xenon.store.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Composable
fun ShareStoreDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.share_store_title)) },
        text = { Text(text = stringResource(R.string.share_store_description)) },
        confirmButton = {
            TextButton(
                onClick = {
                    shareFile(context)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.share_via_file))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    shareLink(context)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.share_via_link))
            }
        }
    )
}

private fun shareFile(context: Context) {
    // Note: This assumes "XenonStoreInstaller.apk" is in your app's assets folder.
    // You might need to adjust this logic if the APK is stored elsewhere or generated.
    try {
        val assetManager = context.assets
        val inputStream = assetManager.open("XenonStoreInstaller.apk") // Ensure this file exists in assets
        val file = File(context.cacheDir, "XenonStoreInstaller.apk")
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.flush()
        outputStream.close()

        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_store_title)))
    } catch (e: IOException) {
        // Handle error, e.g., show a toast
        android.widget.Toast.makeText(context, "Error sharing file: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }
}

private fun shareLink(context: Context) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "https://xenonware.com") // Placeholder link
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_store_title)))
}

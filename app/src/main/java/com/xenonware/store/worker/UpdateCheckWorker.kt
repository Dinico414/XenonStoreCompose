package com.xenonware.store.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xenonware.store.MainActivity
import com.xenonware.store.R
import com.xenonware.store.data.SharedPreferenceManager
import com.xenonware.store.util.Util
import com.xenonware.store.viewmodel.classes.StoreResponse
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.platform.PlatformRegistry.applicationContext

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val client = OkHttpClient()
    private val jsonSerializer = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val sharedPrefs = SharedPreferenceManager(context)

    companion object {
        const val CHANNEL_ID = "update_notifications"
        const val APPS_JSON_URL = "https://storage.googleapis.com/xenon-store-bucket/apps.json"
    }

    override suspend fun doWork(): Result {
        try {
            val request = Request.Builder().url(APPS_JSON_URL).build()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) return Result.retry()
            
            val body = response.body?.string() ?: return Result.failure()
            val storeResponse = jsonSerializer.decodeFromString<StoreResponse>(body)
            
            val usePre = sharedPrefs.checkForPreReleases
            val updatesAvailable = mutableListOf<String>()

            for (item in storeResponse.appList) {
                val installedVersion = getInstalledVersion(item.packageName) ?: continue
                
                val cloudVersion = if (usePre) {
                    item.newVersion
                } else {
                    item.stableVersion ?: ""
                }

                if (cloudVersion.isNotEmpty() && Util.isNewerVersion(installedVersion, cloudVersion)) {
                    updatesAvailable.add(item.getName(Util.getCurrentLanguage(applicationContext.resources)))
                }
            }

            if (updatesAvailable.isNotEmpty()) {
                showNotification(updatesAvailable)
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun getInstalledVersion(pkg: String): String? {
        return try {
            applicationContext.packageManager.getPackageInfo(pkg, 0).versionName
        } catch (_: Exception) {
            null
        }
    }

    private fun showNotification(apps: List<String>) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for available app updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (apps.size == 1) {
            "Update available for ${apps[0]}"
        } else {
            "${apps.size} updates available"
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Adjust icon as needed
            .setContentTitle("Xenon Store")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}

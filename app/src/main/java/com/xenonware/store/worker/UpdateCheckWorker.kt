package com.xenonware.store.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xenonware.store.MainActivity
import com.xenonware.store.R
import com.xenonware.store.data.SharedPreferenceManager
import com.xenonware.store.util.Util
import com.xenonware.store.viewmodel.classes.GitHubRelease
import com.xenonware.store.viewmodel.classes.StoreItem
import com.xenonware.store.viewmodel.classes.StoreResponse
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

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
            val request = Request.Builder()
                .url(APPS_JSON_URL)
                .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
                .header("Cache-Control", "no-cache")
                .build()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) return Result.retry()
            
            val body = response.body.string()
            val storeResponse = jsonSerializer.decodeFromString<StoreResponse>(body)
            
            val usePre = sharedPrefs.checkForPreReleases
            val updatesAvailable = mutableListOf<String>()

            val customApps = sharedPrefs.loadCustomStoreItems()
            val updatedCustomApps = customApps.map { item ->
                if (item.githubUrl.isNotBlank()) {
                    val owner = item.owner
                    val repo = item.repo
                    if (owner.isNotBlank() && repo.isNotBlank()) {
                        fetchGitHubReleaseSync(item, owner, repo) ?: item
                    } else {
                        item
                    }
                } else {
                    item
                }
            }

            if (updatedCustomApps != customApps) {
                sharedPrefs.saveCustomStoreItems(updatedCustomApps)
            }

            val allApps = (storeResponse.appList + updatedCustomApps)
                .distinctBy { it.packageName }
                .map { item ->
                    if (item.githubUrl.isNotBlank() && item.owner.isNotBlank() && item.repo.isNotBlank()) {
                        fetchGitHubReleaseSync(item, item.owner, item.repo) ?: item
                    } else {
                        item
                    }
                }

            for (item in allApps) {
                val installedVersion = getInstalledVersion(item.packageName) ?: continue
                
                val targetVersion = if (usePre) {
                    val stableVer = item.stableVersion ?: ""
                    val preVer = item.preVersion ?: item.newVersion
                    if (stableVer.isNotEmpty() && preVer.isNotEmpty()) {
                        if (Util.isNewerVersion(preVer, stableVer)) stableVer else preVer
                    } else if (preVer.isNotEmpty()) {
                        preVer
                    } else {
                        stableVer.ifEmpty { item.newVersion }
                    }
                } else {
                    item.stableVersion ?: if (!item.isPrerelease) item.newVersion else ""
                }

                if (targetVersion.isNotEmpty() && Util.isNewerVersion(installedVersion, targetVersion)) {
                    updatesAvailable.add(item.getName(Util.getCurrentLanguage(applicationContext.resources)))
                }
            }

            if (updatesAvailable.isNotEmpty()) {
                showNotification(updatesAvailable)
            }

            return Result.success()
        } catch (_: Exception) {
            return Result.retry()
        }
    }

    private fun parseReleasesResponseBody(body: String): List<GitHubRelease> {
        return try {
            jsonSerializer.decodeFromString<List<GitHubRelease>>(body)
        } catch (_: Exception) {
            try {
                val single = jsonSerializer.decodeFromString<GitHubRelease>(body)
                listOf(single)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    private fun fetchGitHubReleaseSync(item: StoreItem, owner: String, repo: String): StoreItem? {
        val url = "https://api.github.com/repos/$owner/$repo/releases"
        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("Accept", "vnd.github+json")

        val token = sharedPrefs.githubToken
        if (token.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        return try {
            val response = client.newCall(reqBuilder.build()).execute()
            if (!response.isSuccessful) return null
            val body = response.body.string()
            val releases = parseReleasesResponseBody(body)
            if (releases.isEmpty()) return null

            val usePre = sharedPrefs.checkForPreReleases

            val stableRelease = releases.firstOrNull { !it.prerelease && !it.draft && it.assets.any { a -> a.name.endsWith(".apk", ignoreCase = true) } }
            val preRelease = releases.firstOrNull { it.prerelease && !it.draft && it.assets.any { a -> a.name.endsWith(".apk", ignoreCase = true) } }
            val latestAnyRelease = releases.firstOrNull { !it.draft && it.assets.any { a -> a.name.endsWith(".apk", ignoreCase = true) } }

            val stableVer = stableRelease?.tagName?.ifBlank { stableRelease.name ?: "" }?.removePrefix("v")?.removePrefix("V") ?: ""
            val preVer = preRelease?.tagName?.ifBlank { preRelease.name ?: "" }?.removePrefix("v")?.removePrefix("V") ?: ""

            val selectedRelease = if (usePre) {
                if (stableRelease != null && preRelease != null) {
                    if (Util.isNewerVersion(preVer, stableVer)) {
                        stableRelease
                    } else {
                        preRelease
                    }
                } else {
                    preRelease ?: stableRelease ?: latestAnyRelease
                }
            } else {
                stableRelease ?: latestAnyRelease
            } ?: return null

            val apkAsset = selectedRelease.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) } ?: return null
            val cleanVersion = selectedRelease.tagName.ifBlank { selectedRelease.name ?: "" }
                .removePrefix("v").removePrefix("V")

            val icon = if (item.iconPath.isBlank()) "https://github.com/$owner.png" else item.iconPath

            item.copy(
                newVersion = cleanVersion,
                downloadUrl = apkAsset.browserDownloadUrl,
                stableVersion = stableVer.ifBlank { null },
                stableDownloadUrl = stableRelease?.assets?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }?.browserDownloadUrl,
                preVersion = preVer.ifBlank { null },
                preDownloadUrl = preRelease?.assets?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }?.browserDownloadUrl,
                isPrerelease = selectedRelease.prerelease,
                iconPath = icon
            )
        } catch (_: Exception) {
            null
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

        val channel = NotificationChannel(
            CHANNEL_ID,
            "App Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for available app updates"
        }
        notificationManager.createNotificationChannel(channel)

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

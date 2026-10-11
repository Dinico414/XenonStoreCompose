package com.xenonware.store.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.xenonware.store.MainActivity
import com.xenonware.store.R
import com.xenonware.store.viewmodel.classes.StoreItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class DownloadStatus {
    DOWNLOADING,
    SUCCESS,
    FAILED,
    CANCELLED
}

data class DownloadProgress(
    val packageName: String,
    val appName: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val status: DownloadStatus,
    val downloadedFile: File? = null,
    val errorMessage: String? = null,
    val isCustom: Boolean = false
)

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient()

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val FOREGROUND_NOTIFICATION_ID = 2001
        const val COMPLETE_NOTIFICATION_ID = 2002

        const val ACTION_START_DOWNLOAD = "com.xenonware.store.action.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.xenonware.store.action.CANCEL_DOWNLOAD"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_DOWNLOAD_URL = "extra_download_url"
        const val EXTRA_DEST_PATH = "extra_dest_path"
        const val EXTRA_IS_CUSTOM = "extra_is_custom"

        private val _downloadProgressFlow = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
        val downloadProgressFlow: StateFlow<Map<String, DownloadProgress>> = _downloadProgressFlow.asStateFlow()

        private val activeTasks = ConcurrentHashMap<String, DownloadProgress>()
        private val activeJobs = ConcurrentHashMap<String, Job>()
        private val activeCalls = ConcurrentHashMap<String, Call>()

        fun isDownloading(packageName: String): Boolean {
            return activeTasks.containsKey(packageName) ||
                    _downloadProgressFlow.value[packageName]?.status == DownloadStatus.DOWNLOADING
        }

        fun getProgress(packageName: String): DownloadProgress? {
            return activeTasks[packageName] ?: _downloadProgressFlow.value[packageName]
        }

        fun hasActiveDownloads(): Boolean {
            return activeTasks.isNotEmpty() || _downloadProgressFlow.value.values.any { it.status == DownloadStatus.DOWNLOADING }
        }

        fun startDownload(
            context: Context,
            item: StoreItem,
            downloadUrl: String,
            destFile: File
        ) {
            if (isDownloading(item.packageName)) {
                return
            }
            val language = context.resources.configuration.locales.get(0).language
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_PACKAGE_NAME, item.packageName)
                putExtra(EXTRA_APP_NAME, item.getName(language))
                putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
                putExtra(EXTRA_DEST_PATH, destFile.absolutePath)
                putExtra(EXTRA_IS_CUSTOM, item.isCustom)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancelDownload(context: Context, packageName: String) {
            cancelDownloadTask(packageName)
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_PACKAGE_NAME, packageName)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
            }
        }

        fun cancelDownloadTask(pkg: String) {
            // Abort the network call first: this unblocks execute()/read() immediately.
            activeCalls.remove(pkg)?.cancel()
            activeJobs.remove(pkg)?.cancel()
            activeTasks.remove(pkg)
            _downloadProgressFlow.update { it - pkg }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL_DOWNLOAD) {
            val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME)
            if (pkg != null) {
                cancelDownloadTask(pkg)
            }
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_START_DOWNLOAD) {
            val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return START_NOT_STICKY
            if (isDownloading(pkg)) {
                return START_STICKY
            }
            val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: pkg
            val url = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return START_NOT_STICKY
            val destPath = intent.getStringExtra(EXTRA_DEST_PATH) ?: return START_NOT_STICKY
            val isCustom = intent.getBooleanExtra(EXTRA_IS_CUSTOM, false)

            val destFile = File(destPath)
            startDownloadTask(pkg, appName, url, destFile, isCustom)
        }
        return START_STICKY
    }

    private fun startDownloadTask(
        pkg: String,
        appName: String,
        url: String,
        destFile: File,
        isCustom: Boolean
    ) {
        if (activeTasks.containsKey(pkg)) {
            return
        }
        val initialProgress = DownloadProgress(
            packageName = pkg,
            appName = appName,
            bytesDownloaded = 0L,
            totalBytes = 0L,
            status = DownloadStatus.DOWNLOADING,
            isCustom = isCustom
        )
        activeTasks[pkg] = initialProgress
        updateProgressFlow()
        updateForegroundNotification()

        // LAZY so the job is registered in activeJobs before it can run/finish.
        val job = serviceScope.launch(start = CoroutineStart.LAZY) {
            val call = client.newCall(Request.Builder().url(url).build())
            activeCalls[pkg] = call
            try {
                ensureActive()
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        onDownloadFailed(pkg, appName, "HTTP Code ${response.code}", destFile)
                        return@launch
                    }

                    val body = response.body
                    val total = body.contentLength()
                    destFile.outputStream().use { out ->
                        body.byteStream().use { inp ->
                            val buf = ByteArray(8192)
                            var bytes = inp.read(buf)
                            var current = 0L
                            var lastNotifyTime = 0L

                            while (bytes >= 0) {
                                ensureActive()
                                out.write(buf, 0, bytes)
                                current += bytes

                                val now = System.currentTimeMillis()
                                if (now - lastNotifyTime > 250L || current == total) {
                                    lastNotifyTime = now
                                    val progress = DownloadProgress(
                                        packageName = pkg,
                                        appName = appName,
                                        bytesDownloaded = current,
                                        totalBytes = total,
                                        status = DownloadStatus.DOWNLOADING,
                                        isCustom = isCustom
                                    )
                                    // Only update if the task wasn't cancelled meanwhile,
                                    // otherwise we'd re-add it to the UI.
                                    if (activeTasks.computeIfPresent(pkg) { _, _ -> progress } != null) {
                                        updateProgressFlow()
                                        updateForegroundNotification()
                                    }
                                }

                                bytes = inp.read(buf)
                            }
                        }
                    }
                }

                ensureActive()
                onDownloadSuccess(pkg, appName, destFile, isCustom)

            } catch (e: Exception) {
                // call.cancel() surfaces as an IOException ("Canceled" / "Socket closed"),
                // not as a CancellationException, so check all cancel signals.
                if (e is CancellationException || call.isCanceled() || !isActive) {
                    onDownloadCancelled(pkg, destFile)
                } else {
                    onDownloadFailed(pkg, appName, e.message ?: "Download error", destFile)
                }
            } finally {
                activeCalls.remove(pkg, call)
                activeJobs.remove(pkg, coroutineContext.job)
            }
        }
        activeJobs[pkg] = job
        job.start()
    }

    private fun cancelDownloadTask(pkg: String) {
        Companion.cancelDownloadTask(pkg)
        checkStopService()
    }

    private fun onDownloadCancelled(pkg: String, destFile: File) {
        if (destFile.exists()) {
            try {
                destFile.delete()
            } catch (_: Exception) {}
        }
        activeTasks.remove(pkg)
        Companion.cancelDownloadTask(pkg)
        checkStopService()
    }

    private fun onDownloadSuccess(pkg: String, appName: String, destFile: File, isCustom: Boolean) {
        val successProgress = DownloadProgress(
            packageName = pkg,
            appName = appName,
            bytesDownloaded = destFile.length(),
            totalBytes = destFile.length(),
            status = DownloadStatus.SUCCESS,
            downloadedFile = destFile,
            isCustom = isCustom
        )
        activeTasks.remove(pkg)
        updateProgressFlowWithCompleted(successProgress)

        showCompletedNotification(appName, "Download complete")
        checkStopService()
    }

    private fun onDownloadFailed(pkg: String, appName: String, error: String, destFile: File) {
        if (destFile.exists()) destFile.delete()

        val failedProgress = DownloadProgress(
            packageName = pkg,
            appName = appName,
            bytesDownloaded = 0L,
            totalBytes = 0L,
            status = DownloadStatus.FAILED,
            errorMessage = error
        )
        activeTasks.remove(pkg)
        updateProgressFlowWithCompleted(failedProgress)

        showCompletedNotification(appName, "Download failed: $error")
        checkStopService()
    }

    private fun updateProgressFlow() {
        _downloadProgressFlow.value = activeTasks.toMap()
    }

    private fun updateProgressFlowWithCompleted(progress: DownloadProgress) {
        _downloadProgressFlow.update { it + (progress.packageName to progress) }
    }

    private fun updateForegroundNotification() {
        if (activeTasks.isEmpty()) return

        val activeList = activeTasks.values.toList()
        val primary = activeList.first()

        val title = if (activeList.size == 1) {
            "Downloading ${primary.appName}"
        } else {
            "Downloading ${activeList.size} apps"
        }

        val percent = if (primary.totalBytes > 0) {
            ((primary.bytesDownloaded * 100) / primary.totalBytes).toInt()
        } else -1

        val currentStr = Formatter.formatShortFileSize(this, primary.bytesDownloaded)
        val totalStr = if (primary.totalBytes > 0) Formatter.formatShortFileSize(this, primary.totalBytes) else "..."

        val contentText = if (percent >= 0) {
            "$percent% • $currentStr / $totalStr"
        } else {
            "$currentStr / $totalStr"
        }

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (percent >= 0) {
            builder.setProgress(100, percent, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        val notification = builder.build()

        startForeground(
            FOREGROUND_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun showCompletedNotification(appName: String, text: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(appName)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(COMPLETE_NOTIFICATION_ID, notification)
    }

    private fun checkStopService() {
        if (activeTasks.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            updateForegroundNotification()
        }
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "App Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows live progress for app downloads"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
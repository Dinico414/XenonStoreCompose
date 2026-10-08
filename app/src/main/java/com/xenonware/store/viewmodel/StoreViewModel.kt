package com.xenonware.store.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.store.data.InstallMethod
import com.xenonware.store.data.SharedPreferenceManager
import com.xenonware.store.util.Util.Companion.getCurrentLanguage
import com.xenonware.store.viewmodel.classes.AppEntryState
import com.xenonware.store.viewmodel.classes.GitHubRelease
import com.xenonware.store.viewmodel.classes.StoreItem
import com.xenonware.store.viewmodel.classes.StoreResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rikka.shizuku.Shizuku
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val _cloudStoreItems = MutableStateFlow<List<StoreItem>>(emptyList())
    private val _customStoreItems = MutableStateFlow<List<StoreItem>>(emptyList())
    private val _storeItems = MutableStateFlow<List<StoreItem>>(emptyList())
    val storeItems: StateFlow<List<StoreItem>> = _storeItems.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentActionInfo = MutableStateFlow<String?>(null)
    val currentActionInfo: StateFlow<String?> = _currentActionInfo.asStateFlow()

    private val _xenonStoreUpdateInfo = MutableStateFlow<GithubReleaseInfo?>(null)
    val xenonStoreUpdateInfo: StateFlow<GithubReleaseInfo?> = _xenonStoreUpdateInfo.asStateFlow()

    private val _xenonStoreDownloadProgress = MutableStateFlow(0f)
    val xenonStoreDownloadProgress: StateFlow<Float> = _xenonStoreDownloadProgress.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _toastMessage = MutableStateFlow<String?>(null)

    private val client: OkHttpClient = OkHttpClient.Builder().build()
    private val sharedPreferenceManager = SharedPreferenceManager(application)
    private val packageInstallReceiver = PackageInstallReceiver()
    private val jsonSerializer = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    
    private var isPackageReceiverRegistered = false
    private var originalCloudItems: List<StoreItem> = emptyList()
    private val lastUpdateMap = mutableMapOf<String, Long>()

    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeDownloads = ConcurrentHashMap<String, Boolean>()
    private val activeInstallations = ConcurrentHashMap<String, Boolean>()

    private companion object {
        const val TAG = "XenonStoreVM"
        const val XENON_STORE_PACKAGE = "com.xenonware.store"
        const val BASE_CLOUD_URL = "https://storage.googleapis.com/xenon-store-bucket"
        const val APPS_JSON_URL = "$BASE_CLOUD_URL/apps.json"
    }

    init {
        cleanupOldApks(application.applicationContext)
        loadCustomStoreItems()
        fetchAndRefreshAppList()
        checkForXenonStoreUpdate()
        registerPackageReceiver()
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        try {
            val context = getApplication<Application>().applicationContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(packageInstallReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(packageInstallReceiver, filter)
            }
            isPackageReceiverRegistered = true
        } catch (_: Exception) {}
    }

    private fun loadCustomStoreItems() {
        viewModelScope.launch {
            _customStoreItems.value = sharedPreferenceManager.loadCustomStoreItems()
            refreshItemsState(isCustom = true)
            fetchReleaseInfoForCustomApps()
        }
    }

    fun fetchAndRefreshAppList() {
        fetchReleaseInfoForCustomApps()
        viewModelScope.launch {
            _currentActionInfo.value = "Fetching app list..."
            downloadToString(APPS_JSON_URL) { result ->
                if (result != null) {
                    try {
                        // The Cloud Function has already prepared these apps.json in GCS
                        val response = jsonSerializer.decodeFromString<StoreResponse>(result)
                        val items = response.appList

                        originalCloudItems = items
                        _cloudStoreItems.value = items.filter { cloud ->
                            _customStoreItems.value.none { it.packageName == cloud.packageName }
                        }
                        refreshItemsState(isCustom = false)
                        _currentActionInfo.value = null
                    } catch (e: Exception) {
                        _error.value = "Metadata error. Syncing with Cloud..."
                        Log.e(TAG, "Parse error", e)
                    }
                } else {
                    _error.value = "Cannot reach Xenon Cloud Storage."
                }
            }
        }
    }

    private fun refreshItemsState(isCustom: Boolean) {
        val usePre = sharedPreferenceManager.checkForPreReleases
        val context = getApplication<Application>().applicationContext
        cleanupOldApks(context)
        viewModelScope.launch {
            // Use originalCloudItems as the source for cloud apps to ensure 
            // we always have the absolute latest version available to restore
            val sourceList = if (isCustom) {
                _customStoreItems.value
            } else {
                originalCloudItems.filter { cloud ->
                    _customStoreItems.value.none { it.packageName == cloud.packageName }
                }
            }

            val updated = sourceList.map { item ->
                val newItem = item.copy()
                newItem.installedVersion = getInstalledVersion(newItem.packageName) ?: ""

                // Selection Logic for both Cloud and Custom Apps:
                val candidateVersion: String
                val candidateUrl: String

                if (usePre) {
                    // When pre-releases are enabled, take whichever is newer: the stable release or the pre-release!
                    val stableVer = newItem.stableVersion ?: ""
                    val preVer = newItem.preVersion ?: newItem.newVersion

                    if (stableVer.isNotEmpty() && preVer.isNotEmpty()) {
                        if (com.xenonware.store.util.Util.isNewerVersion(preVer, stableVer)) {
                            // Stable version is newer than pre-release!
                            candidateVersion = stableVer
                            candidateUrl = newItem.stableDownloadUrl ?: newItem.downloadUrl
                        } else {
                            // Pre-release is newer or equal
                            candidateVersion = preVer
                            candidateUrl = newItem.preDownloadUrl ?: newItem.downloadUrl
                        }
                    } else if (stableVer.isNotEmpty()) {
                        candidateVersion = stableVer
                        candidateUrl = newItem.stableDownloadUrl ?: newItem.downloadUrl
                    } else {
                        candidateVersion = preVer
                        candidateUrl = newItem.preDownloadUrl ?: newItem.downloadUrl
                    }
                } else {
                    // If stable only, force the version to the stable one.
                    candidateVersion = newItem.stableVersion ?: if (!newItem.isPrerelease) newItem.newVersion else ""
                    candidateUrl = newItem.stableDownloadUrl ?: if (!newItem.isPrerelease) newItem.downloadUrl else ""
                }

                if (candidateVersion.isNotEmpty()) {
                    newItem.newVersion = candidateVersion
                    newItem.downloadUrl = candidateUrl
                }

                // Check if APK is downloaded locally
                val downloadedApk = getDownloadedApkFile(context, newItem)
                newItem.isDownloaded = downloadedApk != null

                if (activeDownloads[newItem.packageName] == true) {
                    newItem.state = AppEntryState.DOWNLOADING
                } else if (activeInstallations[newItem.packageName] == true) {
                    newItem.state = AppEntryState.INSTALLING
                } else if (newItem.installedVersion.isNotEmpty()) {
                    newItem.state = if (newItem.isOutdated()) AppEntryState.INSTALLED_AND_OUTDATED else AppEntryState.INSTALLED
                } else {
                    newItem.state = AppEntryState.NOT_INSTALLED
                }
                newItem
            }

            if (isCustom) {
                _customStoreItems.value = updated
            } else {
                _cloudStoreItems.value = updated
                checkForXenonStoreUpdate()
            }
            filterItems()
        }
    }

    private fun cleanupOldApks(context: Context) {
        try {
            val installDir = File(context.filesDir, "apks")
            if (!installDir.exists() || !installDir.isDirectory) return

            val now = System.currentTimeMillis()
            val maxAgeMillis = 12 * 60 * 60 * 1000L // 12 hours

            installDir.listFiles()?.forEach { file ->
                if (file.isFile && (file.extension.equals("apk", ignoreCase = true) || file.name.startsWith("download_"))) {
                    val age = now - file.lastModified()
                    if (age >= maxAgeMillis) {
                        try {
                            file.delete()
                            Log.d(TAG, "Deleted old downloaded APK: ${file.name}")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to delete old APK: ${file.name}", e)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun getDownloadedApkFile(context: Context, item: StoreItem): File? {
        val installDir = File(context.filesDir, "apks")
        if (!installDir.exists()) return null

        val cleanPkg = item.packageName
        val cleanVer = item.newVersion.replace(Regex("[^a-zA-Z0-9._-]"), "_")

        // 1. Check versioned file
        if (cleanVer.isNotEmpty()) {
            val versionedFile = File(installDir, "${cleanPkg}_${cleanVer}.apk")
            if (versionedFile.exists() && versionedFile.length() > 0) {
                val pkgInfo = getArchiveInfo(context, versionedFile)
                if (pkgInfo != null) return versionedFile
                else versionedFile.delete()
            }
        }

        // 2. Check unversioned file
        val unversionedFile = File(installDir, "$cleanPkg.apk")
        if (unversionedFile.exists() && unversionedFile.length() > 0) {
            val pkgInfo = getArchiveInfo(context, unversionedFile)
            if (pkgInfo != null) {
                if (item.isOutdated()) {
                    val archiveVer = pkgInfo.versionName?.removePrefix("v")?.removePrefix("V") ?: ""
                    if (archiveVer == item.newVersion.removePrefix("v").removePrefix("V")) {
                        return unversionedFile
                    }
                } else if (item.installedVersion.isEmpty()) {
                    return unversionedFile
                }
            } else {
                unversionedFile.delete()
            }
        }

        return null
    }

    private fun getArchiveInfo(context: Context, file: File): android.content.pm.PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(
                    file.absolutePath,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getInstalledVersion(pkg: String): String? {
        return try {
            getApplication<Application>().packageManager.getPackageInfo(pkg, 0).versionName
        } catch (_: Exception) { null }
    }

    fun installApp(item: StoreItem, context: Context) {
        val existingApk = getDownloadedApkFile(context, item)
        if (existingApk != null) {
            viewModelScope.launch {
                _currentActionInfo.value = "Installing ${item.getName(getCurrentLanguage(context.resources))}..."
                updateItemInternalState(item.packageName, AppEntryState.INSTALLING)
                activeInstallations[item.packageName] = true

                val extractedPkg = getArchiveInfo(context, existingApk)?.packageName ?: item.packageName
                performInstallation(existingApk, extractedPkg, context)

                activeInstallations.remove(item.packageName)
                _currentActionInfo.value = null
            }
            return
        }

        // Not downloaded yet -> Download in background-capable scope
        downloadScope.launch {
            withContext(Dispatchers.Main) {
                _currentActionInfo.value = "Downloading ${item.getName(getCurrentLanguage(context.resources))}..."
                activeDownloads[item.packageName] = true
                updateItemInternalState(item.packageName, AppEntryState.DOWNLOADING)
            }

            val installDir = File(context.filesDir, "apks")
            if (!installDir.exists()) installDir.mkdirs()
            cleanupOldApks(context)

            val tempApk = File(installDir, "download_${System.currentTimeMillis()}.apk")

            downloadFile(item.downloadUrl, tempApk,
                onProgress = { current, total ->
                    updateItemProgress(item.packageName, current, total)
                },
                onSuccess = {
                    downloadScope.launch {
                        activeDownloads.remove(item.packageName)
                        val pkgInfo = getArchiveInfo(context, tempApk)
                        val actualPkg = pkgInfo?.packageName?.takeIf { it.isNotBlank() } ?: item.packageName
                        val cleanVer = (pkgInfo?.versionName ?: item.newVersion).replace(Regex("[^a-zA-Z0-9._-]"), "_")

                        // Update custom app package name if it differed
                        if (item.isCustom && actualPkg != item.packageName) {
                            val currentApps = sharedPreferenceManager.loadCustomStoreItems().toMutableList()
                            val idx = currentApps.indexOfFirst {
                                it.packageName == item.packageName || (it.owner.equals(item.owner, ignoreCase = true) && it.repo.equals(item.repo, ignoreCase = true))
                            }
                            val updatedItem = item.copy(packageName = actualPkg)
                            if (idx != -1) {
                                currentApps[idx] = updatedItem
                            } else {
                                currentApps.add(updatedItem)
                            }
                            sharedPreferenceManager.saveCustomStoreItems(currentApps)
                            withContext(Dispatchers.Main) {
                                _customStoreItems.value = currentApps
                            }
                        }

                        // Save versioned APK
                        val dest = File(installDir, "${actualPkg}_${cleanVer}.apk")
                        if (dest.exists()) dest.delete()
                        tempApk.renameTo(dest)

                        val method = sharedPreferenceManager.installMethod
                        if (method == InstallMethod.SHIZUKU || method == InstallMethod.ROOT) {
                            withContext(Dispatchers.Main) {
                                _currentActionInfo.value = "Installing $actualPkg..."
                                activeInstallations[actualPkg] = true
                                updateItemInternalState(actualPkg, AppEntryState.INSTALLING)
                            }
                            performInstallation(dest, actualPkg, context)
                            activeInstallations.remove(actualPkg)
                            withContext(Dispatchers.Main) {
                                _currentActionInfo.value = null
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                _currentActionInfo.value = null
                                refreshItemsState(item.isCustom)
                            }
                        }
                    }
                },
                onFailure = {
                    downloadScope.launch {
                        activeDownloads.remove(item.packageName)
                        if (tempApk.exists()) tempApk.delete()
                        withContext(Dispatchers.Main) {
                            _error.value = "Download failed."
                            _currentActionInfo.value = null
                            refreshItemsState(item.isCustom)
                        }
                    }
                }
            )
        }
    }

    private suspend fun performInstallation(apk: File, pkg: String, context: Context) {
        val method = sharedPreferenceManager.installMethod
        when (method) {
            InstallMethod.SHIZUKU -> executeShizukuInstall(apk, pkg)
            InstallMethod.ROOT -> {
                // To improve root reliability, we copy to /data/local/tmp first
                val tmpApk = File("/data/local/tmp/${pkg}.apk")
                val copyCmd = "cp ${apk.absolutePath} ${tmpApk.absolutePath} && chmod 666 ${tmpApk.absolutePath}"
                val installCmd = "pm install -r ${tmpApk.absolutePath}"
                val cleanupCmd = "rm ${tmpApk.absolutePath}"
                
                if (executeRootCommand("$copyCmd && $installCmd && $cleanupCmd")) {
                    handlePackageChanged(pkg)
                } else {
                    _error.value = "Root installation failed."
                    refreshItemsState(false)
                }
            }
            InstallMethod.DEFAULT -> {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apk)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    private suspend fun executeRootCommand(cmd: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            process.waitFor() == 0
        } catch (_: Exception) { false }
    }

    private fun executeShizukuInstall(apk: File, pkg: String) {
        if (!Shizuku.pingBinder()) {
            _error.value = "Shizuku not running."
            return
        }

        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(0)
            _error.value = "Requesting Shizuku permission..."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
                val newProcessMethod = shizukuClass.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                newProcessMethod.isAccessible = true
                
                val tmpPath = "/data/local/tmp/${pkg}_temp.apk"
                
                // 1. Copy APK to /data/local/tmp using sh and cat
                // We use this because shell (Shizuku) often can't access app-private or even external-cache dirs on modern Android
                val catProcess = newProcessMethod.invoke(null, arrayOf("sh", "-c", "cat > $tmpPath"), null, null) as Process
                catProcess.outputStream.use { out ->
                    apk.inputStream().use { inp ->
                        inp.copyTo(out)
                    }
                }
                catProcess.waitFor()
                
                // 2. Perform installation
                // -r: replace, -t: allow test APKs, -d: allow downgrade, -g: grant all permissions
                val installProcess = newProcessMethod.invoke(null, arrayOf("pm", "install", "-r", "-t", "-d", "-g", tmpPath), null, null) as Process
                
                // Read output streams before/during wait to prevent buffer blocking
                val outText = installProcess.inputStream.bufferedReader().readText()
                val errText = installProcess.errorStream.bufferedReader().readText()
                val exitCode = installProcess.waitFor()
                
                Log.d(TAG, "Shizuku install exit: $exitCode")
                Log.d(TAG, "Shizuku install stdout: $outText")
                Log.d(TAG, "Shizuku install stderr: $errText")
                
                // 3. Cleanup
                val rmProcess = newProcessMethod.invoke(null, arrayOf("rm", tmpPath), null, null) as Process
                rmProcess.waitFor()

                if (exitCode == 0) {
                    handlePackageChanged(pkg)
                } else {
                    val errorMsg = errText.ifEmpty { outText }.ifEmpty { "Exit code $exitCode" }
                    withContext(Dispatchers.Main) { 
                        _error.value = "Shizuku install failed: ${errorMsg.trim().take(100)}" 
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Shizuku install error", e)
                withContext(Dispatchers.Main) { _error.value = "Shizuku error: ${e.message}" }
            }
        }
    }

    private fun filterItems() {
        val query = _searchQuery.value.lowercase()
        val usePre = sharedPreferenceManager.checkForPreReleases
        
        val all = (_customStoreItems.value + _cloudStoreItems.value).distinctBy { it.packageName }
            .filter { item ->
                // Hide Xenon Store itself from the main app list
                if (item.packageName == XENON_STORE_PACKAGE) return@filter false

                // Visibility Logic:
                item.isCustom || 
                item.installedVersion.isNotEmpty() || 
                usePre || 
                item.stableVersion != null || 
                !item.isPrerelease
            }

        _storeItems.value = if (query.isEmpty()) all else all.filter { 
            it.packageName.lowercase().contains(query) || it.nameMap.values.any { n -> n.lowercase().contains(query) }
        }
    }

    private fun updateItemInternalState(pkg: String, state: AppEntryState) {
        val update = { list: List<StoreItem> -> list.map { if (it.packageName == pkg) it.copy(state = state) else it } }
        _cloudStoreItems.value = update(_cloudStoreItems.value)
        _customStoreItems.value = update(_customStoreItems.value)
        filterItems()
    }

    private fun updateItemProgress(pkg: String, bytes: Long, total: Long) {
        val now = System.currentTimeMillis()
        if (now - (lastUpdateMap[pkg] ?: 0L) < 100 && bytes < total) return
        lastUpdateMap[pkg] = now

        viewModelScope.launch {
            val update = { list: List<StoreItem> ->
                list.map {
                    if (it.packageName == pkg) {
                        it.copy(bytesDownloaded = bytes, fileSize = total)
                    } else it
                }
            }
            _cloudStoreItems.value = update(_cloudStoreItems.value)
            _customStoreItems.value = update(_customStoreItems.value)
            filterItems()
        }
    }

    fun handlePackageChanged(pkg: String) {
        viewModelScope.launch {
            refreshItemsState(isCustom = true)
            refreshItemsState(isCustom = false)
            _currentActionInfo.value = null
            if (pkg == XENON_STORE_PACKAGE) checkForXenonStoreUpdate()
        }
    }

    private fun downloadToString(url: String, callback: (String?) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
            .header("Cache-Control", "no-cache")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(null)
            override fun onResponse(call: Call, response: Response) = callback(response.body.string())
        })
    }

    private fun downloadFile(url: String, dest: File, onProgress: (Long, Long) -> Unit, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        client.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { viewModelScope.launch { onFailure(e.message ?: "Net error") } }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) { viewModelScope.launch { onFailure("Code ${response.code}") }; return }
                try {
                    val body = response.body
                    dest.outputStream().use { out ->
                        body.byteStream().use { inp ->
                            val buf = ByteArray(8192)
                            var bytes = inp.read(buf)
                            var total = 0L
                            while (bytes >= 0) {
                                out.write(buf, 0, bytes)
                                total += bytes
                                onProgress(total, body.contentLength())
                                bytes = inp.read(buf)
                            }
                        }
                    }
                    viewModelScope.launch { onSuccess() }
                } catch (e: Exception) { viewModelScope.launch { onFailure(e.message ?: "Write error") } }
            }
        })
    }

    private fun checkForXenonStoreUpdate() {
        viewModelScope.launch {
            // Use the processed cloud items which already respect the pre-release setting
            val xenonStoreItem = _cloudStoreItems.value.find { it.packageName == XENON_STORE_PACKAGE } ?: return@launch
            if (xenonStoreItem.isOutdated()) {
                _xenonStoreUpdateInfo.value = GithubReleaseInfo(
                    version = xenonStoreItem.newVersion,
                    downloadUrl = xenonStoreItem.downloadUrl
                )
            } else {
                _xenonStoreUpdateInfo.value = null
            }
        }
    }

    private inner class PackageInstallReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pkg = intent?.data?.schemeSpecificPart ?: return
            handlePackageChanged(pkg)
        }
    }
    
    fun onCustomAppsUpdated() {
        loadCustomStoreItems()
    }

    fun onSignedIn() {
        fetchAndRefreshAppList()
    }

    fun verifyAndRefreshPendingInstallations() {
        viewModelScope.launch {
            refreshItemsState(isCustom = true)
            refreshItemsState(isCustom = false)
        }
    }

    fun uninstallApp(item: StoreItem, context: Context) {
        viewModelScope.launch {
            val method = sharedPreferenceManager.installMethod
            when (method) {
                InstallMethod.SHIZUKU -> executeShizukuUninstall(item.packageName)
                InstallMethod.ROOT -> {
                    if (executeRootCommand("pm uninstall ${item.packageName}")) {
                        handlePackageChanged(item.packageName)
                    } else {
                        _error.value = "Root uninstall failed."
                    }
                }
                InstallMethod.DEFAULT -> {
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = "package:${item.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    private fun executeShizukuUninstall(pkg: String) {
        if (!Shizuku.pingBinder()) {
            _error.value = "Shizuku not running."
            return
        }

        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(0)
            _error.value = "Requesting Shizuku permission..."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
                val newProcessMethod = shizukuClass.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                newProcessMethod.isAccessible = true
                
                val process = newProcessMethod.invoke(null, arrayOf("pm", "uninstall", pkg), null, null) as Process
                
                val outText = process.inputStream.bufferedReader().readText()
                val errText = process.errorStream.bufferedReader().readText()
                val exitCode = process.waitFor()
                
                Log.d(TAG, "Shizuku uninstall exit: $exitCode")

                if (exitCode == 0) {
                    handlePackageChanged(pkg)
                } else {
                    val errorMsg = errText.ifEmpty { outText }.ifEmpty { "Exit code $exitCode" }
                    withContext(Dispatchers.Main) { 
                        _error.value = "Shizuku uninstall failed: ${errorMsg.trim().take(100)}" 
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Shizuku uninstall error", e)
                withContext(Dispatchers.Main) { _error.value = "Shizuku error: ${e.message}" }
            }
        }
    }

    fun openApp(item: StoreItem, context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(item.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                _error.value = "Cannot open app."
            }
        } catch (e: Exception) {
            _error.value = "Error opening app: ${e.message}"
        }
    }

    fun downloadAndInstallXenonStoreUpdate(context: Context) {
        val info = _xenonStoreUpdateInfo.value ?: return
        viewModelScope.launch {
            _currentActionInfo.value = "Updating Xenon Store..."
            val installDir = File(context.filesDir, "apks")
            if (!installDir.exists()) installDir.mkdirs()
            val dest = File(installDir, "xenon_store_update.apk")
            downloadFile(info.downloadUrl, dest,
                onProgress = { current, total ->
                    _xenonStoreDownloadProgress.value = current.toFloat() / total
                },
                onSuccess = {
                    viewModelScope.launch {
                        _xenonStoreDownloadProgress.value = 1f
                        performInstallation(dest, XENON_STORE_PACKAGE, context)
                    }
                },
                onFailure = { err ->
                    _error.value = "Update download failed: $err"
                    _xenonStoreDownloadProgress.value = 0f
                }
            )
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

    private fun fetchReleaseInfoForCustomApps() {
        val currentCustom = _customStoreItems.value
        if (currentCustom.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val deferredList = currentCustom.map { item ->
                async {
                    if (item.isCustom && item.githubUrl.isNotBlank()) {
                        val owner = item.owner
                        val repo = item.repo
                        if (owner.isNotBlank() && repo.isNotBlank()) {
                            val fetchedItem = fetchGitHubReleaseSync(item, owner, repo)
                            if (fetchedItem != null) {
                                return@async fetchedItem
                            }
                        }
                    }
                    item
                }
            }
            val updatedList = deferredList.awaitAll()
            if (updatedList != currentCustom) {
                sharedPreferenceManager.saveCustomStoreItems(updatedList)
                withContext(Dispatchers.Main) {
                    _customStoreItems.value = updatedList
                    refreshItemsState(isCustom = true)
                }
            }
        }
    }

    private fun fetchGitHubReleaseSync(item: StoreItem, owner: String, repo: String): StoreItem? {
        val url = "https://api.github.com/repos/$owner/$repo/releases"
        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("Accept", "vnd.github+json")

        val token = sharedPreferenceManager.githubToken
        if (token.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        return try {
            val response = client.newCall(reqBuilder.build()).execute()
            if (!response.isSuccessful) return null
            val body = response.body.string()
            val releases = parseReleasesResponseBody(body)
            if (releases.isEmpty()) return null

            val usePre = sharedPreferenceManager.checkForPreReleases

            val stableRelease = releases.firstOrNull { !it.prerelease && !it.draft && it.assets.any { a -> a.name.endsWith(".apk", ignoreCase = true) } }
            val preRelease = releases.firstOrNull { it.prerelease && !it.draft && it.assets.any { a -> a.name.endsWith(".apk", ignoreCase = true) } }
            val latestAnyRelease = releases.firstOrNull { !it.draft && it.assets.any { a -> a.name.endsWith(".apk", ignoreCase = true) } }

            val stableVer = stableRelease?.tagName?.ifBlank { stableRelease.name ?: "" }?.removePrefix("v")?.removePrefix("V") ?: ""
            val preVer = preRelease?.tagName?.ifBlank { preRelease.name ?: "" }?.removePrefix("v")?.removePrefix("V") ?: ""

            val selectedRelease = if (usePre) {
                // If pre-releases are enabled, select whichever is genuinely newer between stable and pre-release!
                if (stableRelease != null && preRelease != null) {
                    if (com.xenonware.store.util.Util.isNewerVersion(preVer, stableVer)) {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching GitHub release for $owner/$repo", e)
            null
        }
    }

    fun addGitHubRepoConfig(
        owner: String,
        repo: String,
        packageName: String,
        isUpdate: Boolean,
    ) {
        viewModelScope.launch {
            val resolvedPkg = if (packageName.startsWith("com.${owner.lowercase().replace("-", "_")}") || packageName.isBlank()) {
                val real = com.xenonware.store.util.GitHubPackageResolver.resolvePackageName(owner, repo, sharedPreferenceManager.githubToken)
                real.ifBlank { packageName }
            } else {
                packageName
            }

            val currentApps = sharedPreferenceManager.loadCustomStoreItems().toMutableList()
            val newApp = StoreItem(
                nameMap = hashMapOf("en" to repo),
                iconPath = "https://github.com/$owner.png",
                githubUrl = "https://github.com/$owner/$repo",
                packageName = resolvedPkg,
                isCustom = true
            )
            val existingIndex = currentApps.indexOfFirst { it.packageName == resolvedPkg || (it.owner.equals(owner, ignoreCase = true) && it.repo.equals(repo, ignoreCase = true)) }
            if (existingIndex != -1) {
                currentApps[existingIndex] = newApp
            } else {
                currentApps.add(newApp)
            }
            sharedPreferenceManager.saveCustomStoreItems(currentApps)
            loadCustomStoreItems()
        }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q; filterItems() }
    fun showToast(m: String) { _toastMessage.value = m }
    fun clearError() { _error.value = null }

    override fun onCleared() {
        if (isPackageReceiverRegistered) {
            try { getApplication<Application>().unregisterReceiver(packageInstallReceiver) } catch (_: Exception) {}
        }
    }

    data class GithubReleaseInfo(val version: String, val downloadUrl: String)
}

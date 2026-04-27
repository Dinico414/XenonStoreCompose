package com.xenonware.store.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.store.data.InstallMethod
import com.xenonware.store.data.SharedPreferenceManager
import com.xenonware.store.util.Util.Companion.getCurrentLanguage
import com.xenonware.store.viewmodel.classes.AppEntryState
import com.xenonware.store.viewmodel.classes.StoreItem
import com.xenonware.store.viewmodel.classes.StoreResponse
import kotlinx.coroutines.Dispatchers
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
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val client: OkHttpClient = OkHttpClient.Builder().build()
    private val sharedPreferenceManager = SharedPreferenceManager(application)
    private val packageInstallReceiver = PackageInstallReceiver()
    private val jsonSerializer = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    
    private var isPackageReceiverRegistered = false
    private var originalCloudItems: List<StoreItem> = emptyList()
    private val lastUpdateMap = mutableMapOf<String, Long>()

    private companion object {
        const val TAG = "XenonStoreVM"
        const val XENON_STORE_PACKAGE = "com.xenonware.store"
        const val BASE_CLOUD_URL = "https://storage.googleapis.com/xenon-store-bucket"
        const val APPS_JSON_URL = "$BASE_CLOUD_URL/apps.json"
    }

    init {
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
                context.registerReceiver(packageInstallReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
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
        }
    }

    fun fetchAndRefreshAppList(useCache: Boolean = true) {
        viewModelScope.launch {
            _currentActionInfo.value = "Fetching app list..."
            downloadToString(APPS_JSON_URL) { result ->
                if (result != null) {
                    try {
                        // The Cloud Function has already prepared this apps.json in GCS
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
        viewModelScope.launch {
            val targetList = if (isCustom) _customStoreItems else _cloudStoreItems
            val updated = targetList.value.map { item ->
                val newItem = item.copy()
                newItem.installedVersion = getInstalledVersion(newItem.packageName) ?: ""
                
                if (!newItem.isCustom && usePre && !newItem.preVersion.isNullOrEmpty()) {
                    newItem.newVersion = newItem.preVersion!!
                    newItem.downloadUrl = newItem.preDownloadUrl!!
                }

                if (newItem.installedVersion.isNotEmpty()) {
                    newItem.state = if (newItem.isOutdated()) AppEntryState.INSTALLED_AND_OUTDATED else AppEntryState.INSTALLED
                } else {
                    newItem.state = AppEntryState.NOT_INSTALLED
                }
                newItem
            }
            if (isCustom) _customStoreItems.value = updated else _cloudStoreItems.value = updated
            filterItems()
        }
    }

    private fun getInstalledVersion(pkg: String): String? {
        return try {
            getApplication<Application>().packageManager.getPackageInfo(pkg, 0).versionName
        } catch (_: Exception) { null }
    }

    fun installApp(item: StoreItem, context: Context) {
        viewModelScope.launch {
            _currentActionInfo.value = "Downloading ${item.getName(getCurrentLanguage(context.resources))}..."
            val installDir = File(context.filesDir, "apks")
            if (!installDir.exists()) installDir.mkdirs()
            
            val dest = File(installDir, "${item.packageName}.apk")
            
            updateItemInternalState(item.packageName, AppEntryState.DOWNLOADING)
            
            downloadFile(item.downloadUrl, dest, 
                onProgress = { current, total ->
                    updateItemProgress(item.packageName, current, total)
                },
                onSuccess = {
                    viewModelScope.launch {
                        _currentActionInfo.value = "Installing..."
                        updateItemInternalState(item.packageName, AppEntryState.INSTALLING)
                        performInstallation(dest, item.packageName, context)
                    }
                },
                onFailure = { err ->
                    _error.value = "Download failed."
                    refreshItemsState(item.isCustom)
                }
            )
        }
    }

    private suspend fun performInstallation(apk: File, pkg: String, context: Context) {
        val method = sharedPreferenceManager.installMethod
        when (method) {
            InstallMethod.SHIZUKU -> executeShizukuInstall(apk, pkg, context)
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

    private fun executeShizukuInstall(apk: File, pkg: String, context: Context) {
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
        val all = (_customStoreItems.value + _cloudStoreItems.value).distinctBy { it.packageName }
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
        client.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(null)
            override fun onResponse(call: Call, response: Response) = callback(response.body?.string())
        })
    }

    private fun downloadFile(url: String, dest: File, onProgress: (Long, Long) -> Unit, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        client.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { viewModelScope.launch { onFailure(e.message ?: "Net error") } }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) { viewModelScope.launch { onFailure("Code ${response.code}") }; return }
                try {
                    val body = response.body ?: throw IOException("Empty body")
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
            // Find XenonStore in the cloud list
            val xenonStoreItem = originalCloudItems.find { it.packageName == XENON_STORE_PACKAGE } ?: return@launch
            if (xenonStoreItem.isOutdated()) {
                _xenonStoreUpdateInfo.value = GithubReleaseInfo(
                    version = xenonStoreItem.newVersion,
                    downloadUrl = xenonStoreItem.downloadUrl
                )
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
        fetchAndRefreshAppList(useCache = false)
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
                        data = Uri.parse("package:${item.packageName}")
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

    fun addGitHubRepoConfig(
        owner: String,
        repo: String,
        packageName: String,
        gitHubPAT: String?,
        isUpdate: Boolean
    ) {
        viewModelScope.launch {
            val currentApps = sharedPreferenceManager.loadCustomStoreItems().toMutableList()
            val newApp = StoreItem(
                nameMap = hashMapOf("en" to repo),
                iconPath = "",
                githubUrl = "https://github.com/$owner/$repo",
                packageName = packageName,
                isCustom = true
            )
            val existingIndex = currentApps.indexOfFirst { it.packageName == packageName }
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
    fun clearToast() { _toastMessage.value = null }
    fun clearError() { _error.value = null }

    override fun onCleared() {
        super.onCleared()
        if (isPackageReceiverRegistered) {
            try { getApplication<Application>().unregisterReceiver(packageInstallReceiver) } catch (_: Exception) {}
        }
    }

    data class GithubReleaseInfo(val version: String, val downloadUrl: String)
}

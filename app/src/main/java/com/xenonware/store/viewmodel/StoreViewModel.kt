package com.xenonware.store.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.store.InstallMethod
import com.xenonware.store.SharedPreferenceManager
import com.xenonware.store.ShizukuManager
import com.xenonware.store.utils.Utils.Companion.getCurrentLanguage
import com.xenonware.store.utils.Utils.Companion.isNewerVersion
import com.xenonware.store.viewmodel.classes.AppEntryState
import com.xenonware.store.viewmodel.classes.StoreItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val _fullStoreItems = MutableStateFlow<List<StoreItem>>(emptyList())
    private val _storeItems = MutableStateFlow<List<StoreItem>>(emptyList())
    val storeItems: StateFlow<List<StoreItem>> = _storeItems.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentActionInfo = MutableStateFlow<String?>(null)
    val currentActionInfo: StateFlow<String?> = _currentActionInfo.asStateFlow()

    private val _xenonStoreUpdateInfo = MutableStateFlow<GithubReleaseInfo?>(null)
    val xenonStoreUpdateInfo: StateFlow<GithubReleaseInfo?> = _xenonStoreUpdateInfo.asStateFlow()

    private val _xenonStoreDownloadProgress = MutableStateFlow(0f)
    val xenonStoreDownloadProgress: StateFlow<Float> = _xenonStoreDownloadProgress.asStateFlow()

    private val client: OkHttpClient = OkHttpClient.Builder().build()
    private val sharedPreferenceManager = SharedPreferenceManager(application)
    private val packageInstallReceiver: PackageInstallReceiver

    // --- TOAST MESSAGE STATE ---
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }
    // --- END TOAST MESSAGE STATE ---

    private companion object {
        const val APP_LIST_PROTOCOL_VERSION = "v0.1"
        const val TAG = "StoreViewModel"
        const val XENON_STORE_PACKAGE_NAME = "com.xenonware.store"
        const val XENON_STORE_OWNER = "Dinico414"
        const val XENON_STORE_REPO = "XenonStoreCompose"
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001

        const val CLOUD_RUN_API_URL = "https://appstore-cache-8027822132.europe-west1.run.app"
        const val CLOUD_API_ENDPOINT = "/api/apps"
    }


    private var cachedJsonHash: Int = 0

    init {
        fetchAndRefreshAppList()
        checkForXenonStoreUpdate()

        packageInstallReceiver = PackageInstallReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        getApplication<Application>().registerReceiver(packageInstallReceiver, intentFilter)
    }

    private inner class PackageInstallReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.data?.schemeSpecificPart ?: return
            Log.d(TAG, "Package event: ${intent.action} for $packageName")
            handlePackageChanged(packageName)
        }
    }

    fun handlePackageChanged(packageName: String) {
        viewModelScope.launch {
            val currentList = _fullStoreItems.value
            val itemIndex = currentList.indexOfFirst { it.packageName == packageName }
            if (itemIndex != -1) {
                val itemToRefresh = currentList[itemIndex]
                val refreshedItem = refreshAppItemBlocking(
                    itemToRefresh.copy(),
                    githubInfoUseCache = true,
                    forceStateReEvaluation = true
                )
                val newList = currentList.toMutableList()
                newList[itemIndex] = refreshedItem
                _fullStoreItems.value = newList.toList()
                filterStoreItems(_searchQuery.value)
                Log.d(TAG, "Refreshed item $packageName (state: ${refreshedItem.state}) due to package change.")
            } else if (packageName == XENON_STORE_PACKAGE_NAME) {
                checkForXenonStoreUpdate()
            }
        }
    }

    fun verifyAndRefreshPendingInstallations() {
        viewModelScope.launch {
            val itemsToCheck = _fullStoreItems.value.filter { it.state == AppEntryState.INSTALLING }
            if (itemsToCheck.isEmpty()) return@launch

            Log.d(TAG, "Verifying ${itemsToCheck.size} items in INSTALLING state.")
            var listChanged = false
            val currentFullList = _fullStoreItems.value.toMutableList()

            itemsToCheck.forEach { item ->
                val installedVersion = getInstalledAppVersion(item.packageName)
                if (installedVersion == null || (item.newVersion.isNotEmpty() && installedVersion != item.newVersion)) {
                    Log.d(TAG, "Installation for ${item.packageName} likely cancelled or failed. Current installed: $installedVersion, Target: ${item.newVersion}. Reverting state.")
                    val refreshedItem = refreshAppItemBlocking(item.copy(), githubInfoUseCache = true, forceStateReEvaluation = true)
                    val itemIndex = currentFullList.indexOfFirst { it.packageName == item.packageName }
                    if (itemIndex != -1 && currentFullList[itemIndex].state != refreshedItem.state) {
                        currentFullList[itemIndex] = refreshedItem
                        listChanged = true
                    }
                } else {
                    Log.d(TAG, "Item ${item.packageName} is in INSTALLING state, but version check ($installedVersion vs ${item.newVersion}) suggests it might be installed. Refreshing.")
                    val refreshedItem = refreshAppItemBlocking(item.copy(), githubInfoUseCache = true, forceStateReEvaluation = true)
                     val itemIndex = currentFullList.indexOfFirst { it.packageName == item.packageName }
                    if (itemIndex != -1 && currentFullList[itemIndex].state != refreshedItem.state) {
                        currentFullList[itemIndex] = refreshedItem
                        listChanged = true
                    }
                }
            }
            if (listChanged) {
                _fullStoreItems.value = currentFullList.toList()
                filterStoreItems(_searchQuery.value)
            }
        }
    }


    fun fetchAndRefreshAppList(useCache: Boolean = true) {
        viewModelScope.launch {
            _currentActionInfo.value = "Refreshing app list..."
            val urlString = CLOUD_RUN_API_URL + CLOUD_API_ENDPOINT
            downloadToString(urlString, object : DownloadListener<String> {
                override fun onCompleted(result: String) {
                    val hash = result.hashCode()
                    if (useCache && cachedJsonHash != 0 && _fullStoreItems.value.isNotEmpty() && cachedJsonHash == hash) {
                        Log.d(TAG, "App list JSON is unchanged, refreshing existing items states with useCache = true.")
                        refreshAllAppItemsStates(true)
                        _currentActionInfo.value = "App list refreshed (cached)."
                        showToast("App list refreshed!") // Show toast on completion
                        return
                    }
                    cachedJsonHash = hash
                    Log.d(TAG, "Parsing new app list JSON or cache miss/invalidated.")
                    val appList = parseAppListJson(result)
                    _fullStoreItems.value = appList // Store the full list
                    refreshAllAppItemsStates(false)
                    filterStoreItems(_searchQuery.value) // Apply current search query
                    _currentActionInfo.value = "App list updated."
                    showToast("App list refreshed!") // Show toast on completion
                }

                override fun onFailure(error: String) {
                    _error.value = "Failed to fetch app list from Cloud Run: $error"
                    _currentActionInfo.value = null
                    showToast("Failed to refresh app list.") // Show toast on failure
                }
            }, useCache)
        }
    }

    private fun refreshAllAppItemsStates(useCache: Boolean) {
        viewModelScope.launch {
            val updatedList = _fullStoreItems.value.map { item ->
                refreshAppItemBlocking(item.copy(), githubInfoUseCache = useCache, forceStateReEvaluation = false)
            }
            _fullStoreItems.value = updatedList
            filterStoreItems(_searchQuery.value)
        }
    }

    private suspend fun refreshAppItemBlocking(
        appItem: StoreItem,
        githubInfoUseCache: Boolean,
        forceStateReEvaluation: Boolean = false,
    ): StoreItem {
        return withContext(Dispatchers.IO) {
            val currentAppItem = appItem.copy()
            currentAppItem.installedVersion = getInstalledAppVersion(currentAppItem.packageName) ?: ""

            val shouldFetchFromGitHub = currentAppItem.newVersion.isEmpty() ||
                    currentAppItem.downloadUrl.isEmpty() ||
                    !githubInfoUseCache

            if (shouldFetchFromGitHub) {
                try {
                    Log.d(TAG, "Fetching GitHub release for ${currentAppItem.packageName}. Conditions: newVersionEmpty=${currentAppItem.newVersion.isEmpty()}, downloadUrlEmpty=${currentAppItem.downloadUrl.isEmpty()}, githubInfoUseCache=$githubInfoUseCache")
                    val releaseInfo = getNewReleaseVersionGithubBlocking(currentAppItem.owner, currentAppItem.repo)
                    currentAppItem.downloadUrl = releaseInfo.downloadUrl
                    currentAppItem.newVersion = releaseInfo.version
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get release info for ${currentAppItem.packageName}: ${e.message}")
                }
            }

            val previousState = currentAppItem.state
            if (forceStateReEvaluation || (previousState != AppEntryState.DOWNLOADING && previousState != AppEntryState.INSTALLING)) {
                if (currentAppItem.installedVersion.isNotEmpty()) {
                    if (currentAppItem.isOutdated()) {
                        currentAppItem.state = AppEntryState.INSTALLED_AND_OUTDATED
                    } else {
                        currentAppItem.state = AppEntryState.INSTALLED
                    }
                } else {
                    currentAppItem.state = AppEntryState.NOT_INSTALLED
                }
            }

            if (currentAppItem.state != AppEntryState.DOWNLOADING) {
                currentAppItem.bytesDownloaded = 0
                currentAppItem.fileSize = 0
            }
            currentAppItem
        }
    }

    private fun getInstalledAppVersion(packageName: String): String? {
        return try {
            val context = getApplication<Application>().applicationContext
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting installed app version for $packageName: ${e.message}")
            null
        }
    }

    private fun parseAppListJson(jsonString: String): List<StoreItem> {
        return try {
            val json = JSONObject(jsonString)
            val appList = ArrayList<StoreItem>()
            val version = json.optString("protocolVersion", "v0.0")
            if (isNewerVersion(APP_LIST_PROTOCOL_VERSION, version)) {
                _error.value = "App store client is outdated. Please update XenonStore."
                return emptyList()
            }
            val list = if (jsonString.startsWith("[")) {
                 JSONArray(jsonString)
            } else {
                json.optJSONArray("data") ?: json.getJSONArray("appList")
            }
            for (i in 0 until list.length()) {
                val el = list.getJSONObject(i)
                val nameMap = HashMap<String, String>()
                val defaultName = el.optString("name")
                if (defaultName.isNotEmpty()) nameMap["en"] = defaultName
                el.optJSONObject("names")?.let { namesObj ->
                    namesObj.keys().forEach { langKey ->
                        nameMap[langKey] = namesObj.getString(langKey)
                    }
                }
                val appItem = StoreItem(
                    nameMap = nameMap,
                    iconPath = el.getString("icon"),
                    githubUrl = el.getString("githubUrl"),
                    packageName = el.getString("packageName")
                )
                appList.add(appItem)
            }
            appList
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing app list JSON: ${e.message}")
            _error.value = "Error parsing app data."
            emptyList()
        }
    }

    private suspend fun getNewReleaseVersionGithubBlocking(owner: String, repo: String): GithubReleaseInfo {
        val checkForPreReleases = sharedPreferenceManager.checkForPreReleases
        val url = if (checkForPreReleases) {
            "https://api.github.com/repos/$owner/$repo/releases"
        } else {
            "https://api.github.com/repos/$owner/$repo/releases/latest"
        }
        val request = Request.Builder().url(url).build()
        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw IOException("Unexpected code $response for $url")
                val responseBody = response.body.string()
                if (responseBody.isEmpty()) throw IOException("Empty response body from $url")
                if (checkForPreReleases) {
                    val releasesArray = JSONArray(responseBody)
                    if (releasesArray.length() == 0) throw IOException("No releases found (pre-releases enabled) for $owner/$repo")
                    for (i in 0 until releasesArray.length()) {
                        val releaseNode = releasesArray.getJSONObject(i)
                        val isDraft = releaseNode.optBoolean("draft", false)
                        if (!isDraft) {
                            val assets = releaseNode.getJSONArray("assets")
                            if (assets.length() > 0) {
                                val asset = assets.getJSONObject(0)
                                val newVersion = releaseNode.getString("tag_name")
                                return@withContext GithubReleaseInfo(newVersion, asset.getString("browser_download_url"))
                            }
                        }
                    }
                    throw IOException("No suitable non-draft release with assets found in /releases endpoint for $owner/$repo")
                } else {
                    val latestRelease = JSONObject(responseBody)
                    val newVersion = latestRelease.getString("tag_name")
                    val assets = latestRelease.getJSONArray("assets")
                    if (assets.length() > 0) {
                        val asset = assets.getJSONObject(0)
                        GithubReleaseInfo(newVersion, asset.getString("browser_download_url"))
                    } else {
                        throw IOException("No assets found in the latest release for $owner/$repo")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "GitHub API error for $owner/$repo: ${e.message}")
                throw e
            }
        }
    }

    data class GithubReleaseInfo(val version: String, val downloadUrl: String)

    fun checkForXenonStoreUpdate() {
        viewModelScope.launch {
            try {
                val installedVersion = getInstalledAppVersion(XENON_STORE_PACKAGE_NAME)
                if (installedVersion == null) {
                    Log.d(TAG, "XenonStore not installed, skipping update check.")
                    _xenonStoreUpdateInfo.value = null
                    return@launch
                }
                val latestReleaseInfo = getNewReleaseVersionGithubBlocking(XENON_STORE_OWNER, XENON_STORE_REPO)
                if (isNewerVersion(installedVersion, latestReleaseInfo.version)) {
                    _xenonStoreUpdateInfo.value = latestReleaseInfo
                } else {
                    _xenonStoreUpdateInfo.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check for XenonStore update: ${e.message}")
                _xenonStoreUpdateInfo.value = null
            }
        }
    }

    fun downloadAndInstallXenonStoreUpdate(context: Context) {
        val updateInfo = _xenonStoreUpdateInfo.value ?: return
        viewModelScope.launch {
            _currentActionInfo.value = "Downloading XenonStore update ${updateInfo.version}..."
            _xenonStoreDownloadProgress.value = 0.01f
            val fileName = "XenonStore_${updateInfo.version}.apk"
            val apksDir = File(context.filesDir, "apks")
            if (!apksDir.exists()) {
                apksDir.mkdirs()
            }
            val destinationFile = File(apksDir, fileName)
            downloadFile(updateInfo.downloadUrl, destinationFile,
                onProgress = { bytesDownloaded, fileSize ->
                    val progress = if (fileSize > 0) (bytesDownloaded.toFloat() / fileSize.toFloat()).coerceIn(0f, 1f) else 0.01f
                    _xenonStoreDownloadProgress.value = progress
                },
                onCompleted = {
                    _currentActionInfo.value = "XenonStore download complete. Starting installation..."
                    _xenonStoreDownloadProgress.value = 1f
                    initiateInstall(destinationFile, context, XENON_STORE_PACKAGE_NAME, true) // Pass true for isXenonStoreUpdate
                },
                onFailure = { errorMsg ->
                    _error.value = "XenonStore download failed: $errorMsg"
                    _currentActionInfo.value = null
                    _xenonStoreDownloadProgress.value = 0f
                }
            )
        }
    }

    fun installApp(storeItem: StoreItem, context: Context) {
        viewModelScope.launch {
            val currentItemState = _fullStoreItems.value.firstOrNull { it.packageName == storeItem.packageName } // Use full list
            val itemToInstall = currentItemState?.copy() ?: storeItem.copy()

            _currentActionInfo.value = "Preparing to install ${itemToInstall.getName(
                getCurrentLanguage(context.resources))}..."
            Log.d(TAG, "Install/Update clicked for: ${itemToInstall.packageName}")

            if (itemToInstall.downloadUrl.isEmpty() || itemToInstall.newVersion.isEmpty()) {
                 Log.d(TAG, "Missing downloadUrl or newVersion for ${itemToInstall.packageName}. Attempting refresh.")
                val refreshedItem = refreshAppItemBlocking(itemToInstall, githubInfoUseCache = false, forceStateReEvaluation = false)
                updateItemInList(refreshedItem)
                if (refreshedItem.downloadUrl.isEmpty() || refreshedItem.newVersion.isEmpty()) {
                    _error.value = "No download URL or version for ${refreshedItem.getName(
                      getCurrentLanguage(context.resources))} after refresh."
                    _currentActionInfo.value = null
                    handlePackageChanged(refreshedItem.packageName)
                    return@launch
                }
                installApp(refreshedItem, context) // Recursive call with the refreshed item
                return@launch
            }

            val fileName = "${itemToInstall.packageName}_${itemToInstall.newVersion}.apk"
            val apksDir = File(context.filesDir, "apks")
            if (!apksDir.exists()) {
                apksDir.mkdirs()
            }
            val destinationFile = File(apksDir, fileName)

            updateItemState(itemToInstall.packageName, AppEntryState.DOWNLOADING, bytesDownloaded = 0, fileSize = 1)
            downloadFile(itemToInstall.downloadUrl, destinationFile,
                onProgress = { bytesDownloaded, fileSize ->
                    updateItemState(itemToInstall.packageName, AppEntryState.DOWNLOADING, bytesDownloaded, fileSize)
                },
                onCompleted = {
                    _currentActionInfo.value = "Download complete for ${itemToInstall.getName(
                       getCurrentLanguage(context.resources))}. Starting installation..."
                    updateItemState(itemToInstall.packageName, AppEntryState.INSTALLING, 0, 0)
                    initiateInstall(destinationFile, context, itemToInstall.packageName)
                },
                onFailure = { errorMsg ->
                    _error.value = "Download failed for ${itemToInstall.getName(getCurrentLanguage(context.resources))}: $errorMsg"
                    handlePackageChanged(itemToInstall.packageName)
                    _currentActionInfo.value = null
                }
            )
        }
    }

    private fun updateItemInList(updatedItem: StoreItem) {
        viewModelScope.launch {
            val currentList = _fullStoreItems.value.toMutableList() // Use full list
            val itemIndex = currentList.indexOfFirst { it.packageName == updatedItem.packageName }
            if (itemIndex != -1) {
                currentList[itemIndex] = updatedItem
                _fullStoreItems.value = currentList.toList() // Update the full list
                filterStoreItems(_searchQuery.value) // Re-filter to update visible list
            }
        }
    }

    private suspend fun executeRootCommand(command: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            var output = ""
            var errorOutput = ""
            val exitCode: Int
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))

                val job = viewModelScope.launch(Dispatchers.IO) {
                    launch {
                        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                            output = reader.readText()
                        }
                    }
                    launch {
                        BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                            errorOutput = reader.readText()
                        }
                    }
                }
                job.join()
                exitCode = process.waitFor()

                val logMessage = "Root command '$command'\\nExit code: $exitCode\\nStdout:\\n$output\\nStderr:\\n$errorOutput"
                if (exitCode == 0) {
                    Log.d(TAG, logMessage)
                } else {
                    Log.e(TAG, logMessage)
                }
                Pair(exitCode == 0, if (exitCode == 0) output else errorOutput.ifEmpty { output })
            } catch (e: Exception) {
                Log.e(TAG, "Root command failed: $command", e)
                Pair(false, e.message ?: "Exception occurred")
            }
        }
    }

    private fun initiateInstall(apkFile: File, context: Context, packageName: String, isXenonStoreUpdate: Boolean = false) {
        viewModelScope.launch {
            val installMethod = sharedPreferenceManager.installMethod
            _currentActionInfo.value = "Initiating install for $packageName using $installMethod method..."
            Log.d(TAG, "Initiating install for ${apkFile.name} ($packageName) using $installMethod. APK Path: ${apkFile.absolutePath}")

            try {
                when (installMethod) {
                    InstallMethod.SHIZUKU -> {
                        _currentActionInfo.value = "Waiting for Shizuku service..."
                        val isReady = withTimeoutOrNull(5000L) {
                         ShizukuManager.isAvailable.first { it }
                        } ?: false

                        if (!isReady) {
                            _error.value = "Shizuku service not available. Please start Shizuku and try again."
                            Log.e(TAG, "Timed out waiting for Shizuku binder.")
                            if (!isXenonStoreUpdate) handlePackageChanged(packageName) else resetXenonStoreUpdateState()
                            return@launch
                        }

                        Log.d(TAG, "Shizuku service is ready. Proceeding with installation.")
                        if (Shizuku.isPreV11()) {
                            _error.value = "Shizuku version too old."
                            Log.e(TAG, "Shizuku is pre-v11, not supported by this request method.")
                            if (!isXenonStoreUpdate) handlePackageChanged(packageName) else resetXenonStoreUpdateState()
                            return@launch
                        }
                        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                            Log.i(TAG, "Shizuku permission not granted. Requesting...")
                            _currentActionInfo.value = "Requesting Shizuku permission..."
                            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                            _error.value = "Shizuku permission required. Please grant it and try again."
                            // TODO: IMPORTANT! The app (Activity) needs to handle the Shizuku permission result.
                            if (!isXenonStoreUpdate) handlePackageChanged(packageName) else resetXenonStoreUpdateState()
                            return@launch
                        } else {
                            _currentActionInfo.value = "Using Shizuku to install $packageName..."
                            Log.d(TAG, "Shizuku permission granted. Proceeding with Shizuku installation logic.")
                            // TODO: Implement actual Shizuku installation logic here.
                            _error.value = "Shizuku installation not fully implemented. Install failed."
                            Log.e(TAG, "Shizuku installation API call is a TODO.")
                            if (!isXenonStoreUpdate) handlePackageChanged(packageName) else resetXenonStoreUpdateState()
                        }
                    }
                    InstallMethod.ROOT -> {
                        _currentActionInfo.value = "Attempting Root installation for $packageName..."
                        val tempApkName = "install_${apkFile.name}" // Ensure unique temp name
                        val tempApkPath = "/data/local/tmp/$tempApkName"

                        Log.d(TAG, "Root: Original APK at ${apkFile.absolutePath}")
                        Log.d(TAG, "Root: Will copy to $tempApkPath")

                        // 1. Copy APK to /data/local/tmp/
                        val (copySuccess, copyMessage) = executeRootCommand("cp \"${apkFile.absolutePath}\" \"$tempApkPath\"")
                        if (!copySuccess) {
                            _error.value = "Root: Failed to copy APK to temp: $copyMessage"
                            Log.e(TAG, "Root: Failed to copy APK to $tempApkPath. Details: $copyMessage")
                            if (!isXenonStoreUpdate) handlePackageChanged(packageName) else resetXenonStoreUpdateState()
                            return@launch
                        }
                        Log.d(TAG, "Root: APK copied to $tempApkPath. Output: $copyMessage")

                        // 2. Set permissions (optional but good practice)
                        executeRootCommand("chmod 644 \"$tempApkPath\"") // Best effort

                        // 3. Install from /data/local/tmp/
                        val (installSuccess, installMessage) = executeRootCommand("pm install -g -r \"$tempApkPath\"")

                        // 4. Delete the temp APK
                        val (deleteSuccess, deleteMessage) = executeRootCommand("rm \"$tempApkPath\"")
                        if (!deleteSuccess) {
                            Log.w(TAG, "Root: Failed to delete temp APK $tempApkPath. Details: $deleteMessage")
                        } else {
                            Log.d(TAG, "Root: Temp APK $tempApkPath deleted. Output: $deleteMessage")
                        }

                        if (installSuccess) {
                            _currentActionInfo.value = "Root install command for $packageName succeeded. Waiting for system update..."
                            Log.d(TAG, "Root install for $packageName from $tempApkPath succeeded. Output: $installMessage")
                        } else {
                            _error.value = "Root installation failed for $packageName: $installMessage"
                            Log.e(TAG, "Root installation from $tempApkPath failed. Details: $installMessage")
                            if (!isXenonStoreUpdate) handlePackageChanged(packageName) else resetXenonStoreUpdateState()
                        }
                    }
                  InstallMethod.DEFAULT -> {
                      if (!context.packageManager.canRequestPackageInstalls()) {
                          _error.value = "Permission needed to install apps. Please enable in settings."
                          val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                              data = "package:${context.packageName}".toUri()
                              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                          }
                          context.startActivity(settingsIntent)
                          _currentActionInfo.value = null
                          if (!isXenonStoreUpdate) handlePackageChanged(packageName) else resetXenonStoreUpdateState()
                          return@launch
                      }
                      val intent = Intent(Intent.ACTION_VIEW)
                        val fileUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
                        intent.setDataAndType(fileUri, "application/vnd.android.package-archive")
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try {
                            context.startActivity(intent)
                            Log.d(TAG, "Default installation intent started for ${apkFile.name}. Item state: INSTALLING.")
                            _currentActionInfo.value = "Installation for $packageName started by system (Default)."
                        } catch (e: Exception) {
                             _error.value = "Failed to start default install intent: ${e.message}"
                            Log.e(TAG, "Could not start ACTION_VIEW intent for ${apkFile.absolutePath}", e)
                            if (!isXenonStoreUpdate) handlePackageChanged(packageName) else resetXenonStoreUpdateState()
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to start installation for $packageName: ${e.message}"
                Log.e(TAG, "Error initiating install for $packageName with $installMethod: ", e)
                _currentActionInfo.value = null
                if (!isXenonStoreUpdate) handlePackageChanged(packageName) else resetXenonStoreUpdateState()
            }
        }
    }

    private fun resetXenonStoreUpdateState() {
        _xenonStoreDownloadProgress.value = 0f
        _currentActionInfo.value = null
        checkForXenonStoreUpdate() // Refresh update info
    }


    fun uninstallApp(storeItem: StoreItem, context: Context) {
        Log.d(TAG, "Uninstall clicked for: ${storeItem.packageName}")
        _currentActionInfo.value = "Uninstalling ${storeItem.getName(getCurrentLanguage(context.resources))}..."
        try {
            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                data = "package:${storeItem.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _currentActionInfo.value = "Uninstallation for ${storeItem.getName(getCurrentLanguage(context.resources))} started by system."
        } catch (e: Exception) {
            _error.value = "Failed to start uninstall for ${storeItem.getName(getCurrentLanguage(context.resources))}: ${e.message}"
            Log.e(TAG, "Error starting uninstall intent for ${storeItem.packageName}", e)
            _currentActionInfo.value = null
        }
    }

    fun openApp(storeItem: StoreItem, context: Context) {
        Log.d(TAG, "Open clicked for: ${storeItem.packageName}")
        _currentActionInfo.value = "Opening ${storeItem.getName(getCurrentLanguage(context.resources))}..."
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(storeItem.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                _currentActionInfo.value = null
            } else {
                _error.value = "Could not open app: ${storeItem.getName(getCurrentLanguage(context.resources))}. App not found or no launch intent."
                Log.w(TAG, "No launch intent found for package: ${storeItem.packageName}")
                _currentActionInfo.value = null
            }
        } catch (e: Exception) {
            _error.value = "Failed to open app: ${e.message}"
            Log.e(TAG, "Error opening app ${storeItem.packageName}", e)
            _currentActionInfo.value = null
        }
    }

    private fun updateItemState(packageName: String, newState: AppEntryState, bytesDownloaded: Long = 0, fileSize: Long = 0) {
        viewModelScope.launch {
            val currentList = _fullStoreItems.value.toMutableList() // Use full list
            val itemIndex = currentList.indexOfFirst { it.packageName == packageName }
            if (itemIndex != -1) {
                val currentItem = currentList[itemIndex]
                val updatedItem = currentItem.copy(
                    state = newState,
                    bytesDownloaded = if (newState == AppEntryState.DOWNLOADING) bytesDownloaded else 0,
                    fileSize = when (newState) {
                        AppEntryState.DOWNLOADING if fileSize > 0 -> fileSize
                        AppEntryState.DOWNLOADING if currentItem.fileSize > 0 -> currentItem.fileSize
                        else -> 0
                    }
                )
                if (currentList[itemIndex] != updatedItem) {
                    currentList[itemIndex] = updatedItem
                    _fullStoreItems.value = currentList.toList() // Update the full list
                    filterStoreItems(_searchQuery.value) // Re-filter to update visible list
                }
            }
        }
    }

    private interface DownloadListener<T> {
        fun onCompleted(result: T)
        fun onFailure(error: String)
    }

    private fun downloadToString(url: String, listener: DownloadListener<String>, useCache: Boolean = true) {
        val request = Request.Builder().url(url).build()
        val currentClient = if (useCache) client else OkHttpClient.Builder().cache(null).build()
        currentClient.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    listener.onFailure("Response error code: ${response.code} from $url")
                    return
                }
                response.body.string().let { listener.onCompleted(it) }
            }
            override fun onFailure(call: Call, e: IOException) { listener.onFailure("Download failed for $url: ${e.message}") }
        })
    }

    private fun downloadFile(
        url: String, destinationFile: File,
        onProgress: (bytesDownloaded: Long, fileSize: Long) -> Unit,
        onCompleted: () -> Unit, onFailure: (errorMsg: String) -> Unit,
    ) {
        val request = Request.Builder().url(url).build()
        val freshClient = OkHttpClient.Builder().cache(null).build() // Use a fresh client to bypass OkHttp caching for downloads
        freshClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { viewModelScope.launch { onFailure("Network error: ${e.message}") } }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) { viewModelScope.launch { onFailure("Server error: ${response.code} for $url") }; return }
                val body = response.body
                val fileSize = body.contentLength()
                var bytesCopied: Long = 0
                try {
                    destinationFile.outputStream().use { outputStream ->
                        body.byteStream().use { inputStream ->
                            val buffer = ByteArray(8 * 1024)
                            var bytes = inputStream.read(buffer)
                            while (bytes >= 0) {
                                outputStream.write(buffer, 0, bytes)
                                bytesCopied += bytes
                                viewModelScope.launch { onProgress(bytesCopied, fileSize) }
                                bytes = inputStream.read(buffer)
                            }
                        }
                    }
                    if (bytesCopied > 0 || fileSize == 0L) { // Allow 0 byte files if server reports 0L
                        viewModelScope.launch { onCompleted() }
                    } else {
                        viewModelScope.launch { onFailure("Zero bytes downloaded from $url.") }
                    }
                } catch (e: IOException) { viewModelScope.launch { onFailure("File I/O error for $url: ${e.message}") } }
                finally { body.close() }
            }
        })
    }

    fun clearError() { _error.value = null }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterStoreItems(query)
    }

    private fun filterStoreItems(query: String) {
        if (query.isBlank()) {
            _storeItems.value = _fullStoreItems.value
            return
        }
        val lowerCaseQuery = query.lowercase()
        val filteredList = _fullStoreItems.value.filter { storeItem ->
            // Check app name (defaulting to English for search if no other locale matches)
            val appNameMatches = storeItem.nameMap.any { (_, name) ->
                name.lowercase().contains(lowerCaseQuery)
            } || storeItem.packageName.lowercase().contains(lowerCaseQuery) // Check package name

            appNameMatches
        }
        _storeItems.value = filteredList
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(packageInstallReceiver)
        client.dispatcher.executorService.shutdown() // Gracefully shutdown OkHttp
        client.connectionPool.evictAll()
    }
}

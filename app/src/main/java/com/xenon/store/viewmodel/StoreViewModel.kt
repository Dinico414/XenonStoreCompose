package com.xenon.store.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenon.store.SharedPreferenceManager
import com.xenon.store.util.Util
import com.xenon.store.viewmodel.classes.AppEntryState
import com.xenon.store.viewmodel.classes.StoreItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

class StoreViewModel(application: Application) : AndroidViewModel(application) {

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

    private val client: OkHttpClient
    private val sharedPreferenceManager = SharedPreferenceManager(application)
    private val packageInstallReceiver: PackageInstallReceiver

    private companion object {
        const val APP_LIST_PROTOCOL_VERSION = "v0.1"
        const val TAG = "StoreViewModel"
        const val XENON_STORE_PACKAGE_NAME = "com.xenon.store"
        const val XENON_STORE_OWNER = "Dinico414"
        const val XENON_STORE_REPO = "XenonStore"
    }

    private var cachedJsonHash: Int = 0

    init {
        client = OkHttpClient.Builder().build()
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

    private inner class PackageInstallReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.data?.schemeSpecificPart ?: return
            Log.d(TAG, "Package event: ${intent.action} for $packageName")
            handlePackageChanged(packageName)
        }
    }

    fun handlePackageChanged(packageName: String) {
        viewModelScope.launch {
            val currentList = _storeItems.value
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
                _storeItems.value = newList.toList()
                Log.d(TAG, "Refreshed item $packageName (state: ${refreshedItem.state}) due to package change.")
            } else if (packageName == XENON_STORE_PACKAGE_NAME) {
                checkForXenonStoreUpdate()
            }
        }
    }
    
    fun verifyAndRefreshPendingInstallations() {
        viewModelScope.launch {
            val itemsToCheck = _storeItems.value.filter { it.state == AppEntryState.INSTALLING }
            if (itemsToCheck.isEmpty()) return@launch

            Log.d(TAG, "Verifying ${itemsToCheck.size} items in INSTALLING state.")
            var listChanged = false
            val currentList = _storeItems.value.toMutableList()

            itemsToCheck.forEach { item ->
                val installedVersion = getInstalledAppVersion(item.packageName)
                // If it was a new install, newVersion would be the target. If an update, also newVersion.
                // If installedVersion is null (not installed) OR installedVersion is not the newVersion we aimed for.
                if (installedVersion == null || (item.newVersion.isNotEmpty() && installedVersion != item.newVersion)) {
                    Log.d(TAG, "Installation for ${item.packageName} likely cancelled or failed. Current installed: $installedVersion, Target: ${item.newVersion}. Reverting state.")
                    // Re-evaluate its state fully, which will set it to NOT_INSTALLED or INSTALLED_AND_OUTDATED (if old version exists)
                    val refreshedItem = refreshAppItemBlocking(item.copy(), githubInfoUseCache = true, forceStateReEvaluation = true)
                    val itemIndex = currentList.indexOfFirst { it.packageName == item.packageName }
                    if (itemIndex != -1 && currentList[itemIndex].state != refreshedItem.state) {
                        currentList[itemIndex] = refreshedItem
                        listChanged = true
                    }
                } else {
                    // It seems the app was installed/updated correctly, but the broadcast might have been missed or is pending.
                    // Let's force a refresh just in case to get it to INSTALLED state.
                    Log.d(TAG, "Item ${item.packageName} is in INSTALLING state, but version check ($installedVersion vs ${item.newVersion}) suggests it might be installed. Refreshing.")
                    val refreshedItem = refreshAppItemBlocking(item.copy(), githubInfoUseCache = true, forceStateReEvaluation = true)
                     val itemIndex = currentList.indexOfFirst { it.packageName == item.packageName }
                    if (itemIndex != -1 && currentList[itemIndex].state != refreshedItem.state) {
                        currentList[itemIndex] = refreshedItem
                        listChanged = true
                    }
                }
            }
            if (listChanged) {
                _storeItems.value = currentList.toList()
            }
        }
    }


    fun fetchAndRefreshAppList(useCache: Boolean = true) {
        viewModelScope.launch {
            _currentActionInfo.value = "Refreshing app list..."
            val urlString =
                "https://raw.githubusercontent.com/Dinico414/Xenon-Commons/master/accesspoint/src/main/java/com/xenon/commons/accesspoint/app_list.json"
            downloadToString(urlString, object : DownloadListener<String> {
                override fun onCompleted(result: String) {
                    val hash = result.hashCode()
                    if (useCache && cachedJsonHash != 0 && _storeItems.value.isNotEmpty() && cachedJsonHash == hash) {
                        Log.d(TAG, "App list JSON is unchanged, refreshing existing items states with useCache = true.")
                        refreshAllAppItemsStates(true)
                        _currentActionInfo.value = "App list refreshed (cached)."
                        return
                    }
                    cachedJsonHash = hash
                    Log.d(TAG, "Parsing new app list JSON or cache miss/invalidated.")
                    val appList = parseAppListJson(result)
                    _storeItems.value = appList
                    refreshAllAppItemsStates(false) 
                    _currentActionInfo.value = "App list updated."
                }

                override fun onFailure(errorMessage: String) {
                    _error.value = "Failed to fetch app list: $errorMessage"
                    _currentActionInfo.value = null
                }
            }, useCache)
        }
    }

    private fun refreshAllAppItemsStates(useCache: Boolean) {
        viewModelScope.launch {
            val updatedList = _storeItems.value.map { item ->
                refreshAppItemBlocking(item.copy(), githubInfoUseCache = useCache, forceStateReEvaluation = false)
            }
            _storeItems.value = updatedList
        }
    }

    private suspend fun refreshAppItemBlocking(
        appItem: StoreItem,
        githubInfoUseCache: Boolean,
        forceStateReEvaluation: Boolean = false 
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
        } catch (e: PackageManager.NameNotFoundException) {
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
            if (Util.isNewerVersion(APP_LIST_PROTOCOL_VERSION, version)) {
                _error.value = "App store client is outdated. Please update XenonStore."
                return emptyList()
            }
            val list = json.getJSONArray("appList")
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
                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) throw IOException("Empty response body from $url")
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
                if (Util.isNewerVersion(installedVersion, latestReleaseInfo.version)) {
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
            val destinationFile = File(context.getExternalFilesDir(null), fileName)
            downloadFile(updateInfo.downloadUrl, destinationFile,
                onProgress = { bytesDownloaded, fileSize ->
                    val progress = if (fileSize > 0) (bytesDownloaded.toFloat() / fileSize.toFloat()).coerceIn(0f, 1f) else 0.01f
                    _xenonStoreDownloadProgress.value = progress
                },
                onCompleted = {
                    _currentActionInfo.value = "XenonStore download complete. Starting installation..."
                    _xenonStoreDownloadProgress.value = 1f
                    initiateInstall(destinationFile, context, XENON_STORE_PACKAGE_NAME)
                    // Don't reset progress immediately, let UI show 100% briefly if needed
                    // The verifyAndRefreshPendingInstallations or package receiver will handle final state.
                    // _xenonStoreDownloadProgress.value = 0f 
                    // _xenonStoreUpdateInfo.value = null 
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
            val currentItemState = _storeItems.value.firstOrNull { it.packageName == storeItem.packageName }
            val itemToInstall = currentItemState?.copy() ?: storeItem.copy()

            _currentActionInfo.value = "Preparing to install ${itemToInstall.getName(Util.getCurrentLanguage(context.resources))}..."
            Log.d(TAG, "Install/Update clicked for: ${itemToInstall.packageName}")

            // Ensure we have the latest download URL, especially if it was missing before
            if (itemToInstall.downloadUrl.isEmpty() || itemToInstall.newVersion.isEmpty()) {
                 Log.d(TAG, "Missing downloadUrl or newVersion for ${itemToInstall.packageName}. Attempting refresh.")
                val refreshedItem = refreshAppItemBlocking(itemToInstall, githubInfoUseCache = false, forceStateReEvaluation = false)
                updateItemInList(refreshedItem)
                if (refreshedItem.downloadUrl.isEmpty() || refreshedItem.newVersion.isEmpty()) {
                    _error.value = "No download URL or version for ${refreshedItem.getName(Util.getCurrentLanguage(context.resources))} after refresh."
                    _currentActionInfo.value = null
                     // Make sure to reset to a definitive state if we can't proceed
                    handlePackageChanged(refreshedItem.packageName)
                    return@launch
                }
                // Retry with the refreshed item
                installApp(refreshedItem, context)
                return@launch
            }

            val fileName = "${itemToInstall.packageName}_${itemToInstall.newVersion}.apk"
            val destinationFile = File(context.getExternalFilesDir(null), fileName)

            updateItemState(itemToInstall.packageName, AppEntryState.DOWNLOADING, bytesDownloaded = 0, fileSize = 1) 
            downloadFile(itemToInstall.downloadUrl, destinationFile,
                onProgress = { bytesDownloaded, fileSize ->
                    updateItemState(itemToInstall.packageName, AppEntryState.DOWNLOADING, bytesDownloaded, fileSize)
                },
                onCompleted = {
                    _currentActionInfo.value = "Download complete for ${itemToInstall.getName(Util.getCurrentLanguage(context.resources))}. Starting installation..."
                    updateItemState(itemToInstall.packageName, AppEntryState.INSTALLING, 0, 0)
                    initiateInstall(destinationFile, context, itemToInstall.packageName)
                },
                onFailure = { errorMsg ->
                    _error.value = "Download failed for ${itemToInstall.getName(Util.getCurrentLanguage(context.resources))}: $errorMsg"
                    handlePackageChanged(itemToInstall.packageName) 
                    _currentActionInfo.value = null
                }
            )
        }
    }
    
    private fun updateItemInList(updatedItem: StoreItem) {
        viewModelScope.launch {
            val currentList = _storeItems.value.toMutableList()
            val itemIndex = currentList.indexOfFirst { it.packageName == updatedItem.packageName }
            if (itemIndex != -1) {
                currentList[itemIndex] = updatedItem
                _storeItems.value = currentList.toList()
            } else {
                // Should not happen if called after a refresh of an existing item
                // but as a safeguard, could add it if it's truly new, though refresh logic implies it exists.
                 Log.w(TAG, "updateItemInList called for an item not in the list: ${updatedItem.packageName}")
            }
        }
    }

    private fun initiateInstall(apkFile: File, context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val fileUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
            intent.setDataAndType(fileUri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    _error.value = "Permission needed to install apps. Please enable in settings."
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    _currentActionInfo.value = null
                    if (packageName != XENON_STORE_PACKAGE_NAME) {
                         handlePackageChanged(packageName) 
                    } else {
                         _xenonStoreDownloadProgress.value = 0f
                         // User needs to grant permission, then try again. Update info should remain.
                    }
                    return
                }
            }
            context.startActivity(intent)
            Log.d(TAG, "Installation intent started for ${apkFile.name}. Item state: INSTALLING.")
            _currentActionInfo.value = "Installation for $packageName started by system."
            // Item state is already INSTALLING. Broadcast receiver or verifyAndRefreshPendingInstallations will handle next state.
        } catch (e: Exception) {
            _error.value = "Failed to start installation for $packageName: ${e.message}"
            Log.e(TAG, "Error initiating install: ", e)
            _currentActionInfo.value = null
            if (packageName != XENON_STORE_PACKAGE_NAME) {
                handlePackageChanged(packageName) 
            } else {
                _xenonStoreDownloadProgress.value = 0f
                // If XenonStore install failed to even start, re-check for its update to restore button
                checkForXenonStoreUpdate() 
            }
        }
    }

    fun uninstallApp(storeItem: StoreItem, context: Context) {
        Log.d(TAG, "Uninstall clicked for: ${storeItem.packageName}")
        _currentActionInfo.value = "Uninstalling ${storeItem.getName(Util.getCurrentLanguage(context.resources))}..."
        try {
            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                data = Uri.parse("package:${storeItem.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _currentActionInfo.value = "Uninstallation for ${storeItem.getName(Util.getCurrentLanguage(context.resources))} started by system."
        } catch (e: Exception) {
            _error.value = "Failed to start uninstall for ${storeItem.getName(Util.getCurrentLanguage(context.resources))}: ${e.message}"
            Log.e(TAG, "Error starting uninstall intent for ${storeItem.packageName}", e)
            _currentActionInfo.value = null
        }
    }

    fun openApp(storeItem: StoreItem, context: Context) {
        Log.d(TAG, "Open clicked for: ${storeItem.packageName}")
        _currentActionInfo.value = "Opening ${storeItem.getName(Util.getCurrentLanguage(context.resources))}..."
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(storeItem.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                _currentActionInfo.value = null
            } else {
                _error.value = "Could not open app: ${storeItem.getName(Util.getCurrentLanguage(context.resources))}. App not found or no launch intent."
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
            val currentList = _storeItems.value.toMutableList()
            val itemIndex = currentList.indexOfFirst { it.packageName == packageName }
            if (itemIndex != -1) {
                val currentItem = currentList[itemIndex]
                val updatedItem = currentItem.copy(
                    state = newState,
                    bytesDownloaded = if (newState == AppEntryState.DOWNLOADING) bytesDownloaded else 0,
                    fileSize = if (newState == AppEntryState.DOWNLOADING && fileSize > 0) fileSize 
                               else if (newState == AppEntryState.DOWNLOADING && currentItem.fileSize > 0) currentItem.fileSize // Preserve old if new is 0
                               else 0
                )
                if (currentList[itemIndex] != updatedItem) { // Only update flow if actual change
                    currentList[itemIndex] = updatedItem
                    _storeItems.value = currentList.toList()
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
                response.body?.string()?.let { listener.onCompleted(it) } ?: listener.onFailure("Empty body from $url")
            }
            override fun onFailure(call: Call, e: IOException) { listener.onFailure("Download failed for $url: ${e.message}") }
        })
    }

    private fun downloadFile(
        url: String, destinationFile: File,
        onProgress: (bytesDownloaded: Long, fileSize: Long) -> Unit,
        onCompleted: () -> Unit, onFailure: (errorMsg: String) -> Unit
    ) {
        val request = Request.Builder().url(url).build()
        val freshClient = OkHttpClient.Builder().cache(null).build()
        freshClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { viewModelScope.launch { onFailure("Network error: ${e.message}") } }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) { viewModelScope.launch { onFailure("Server error: ${response.code} for $url") }; return }
                val body = response.body
                if (body == null) { viewModelScope.launch { onFailure("Empty response body for $url") }; return }
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
                    if (bytesCopied > 0 || fileSize == 0L) { viewModelScope.launch { onCompleted() } }
                    else { viewModelScope.launch { onFailure("Zero bytes downloaded from $url.") } }
                } catch (e: IOException) { viewModelScope.launch { onFailure("File I/O error for $url: ${e.message}") } }
                finally { body.close() }
            }
        })
    }

    fun clearError() { _error.value = null }
    fun clearActionInfo() { _currentActionInfo.value = null }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(packageInstallReceiver)
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}

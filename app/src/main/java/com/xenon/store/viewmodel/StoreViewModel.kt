package com.xenon.store.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
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

// You'll need to provide the Application context via ViewModelFactory or Hilt
class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val _storeItems = MutableStateFlow<List<StoreItem>>(emptyList())
    val storeItems: StateFlow<List<StoreItem>> = _storeItems.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentActionInfo = MutableStateFlow<String?>(null)
    val currentActionInfo: StateFlow<String?> = _currentActionInfo.asStateFlow()

    private val _xenonStoreUpdateInfo = MutableStateFlow<GithubReleaseInfo?>(null)
    val xenonStoreUpdateInfo: StateFlow<GithubReleaseInfo?> = _xenonStoreUpdateInfo.asStateFlow()

    private val client: OkHttpClient
    private val sharedPreferenceManager = SharedPreferenceManager(application)

    private companion object {
        const val APP_LIST_PROTOCOL_VERSION = "v0.1"
        const val TAG = "StoreViewModel"
        const val XENON_STORE_PACKAGE_NAME = "com.xenon.store" // Your app's package name
        const val XENON_STORE_OWNER = "Dinico414" // GitHub owner
        const val XENON_STORE_REPO = "XenonStoreCompose" // GitHub repo
    }

    private var cachedJsonHash: Int = 0

    init {
        val clientBuilder = OkHttpClient.Builder()
        client = clientBuilder.build()
        fetchAndRefreshAppList()
        checkForXenonStoreUpdate()
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
                refreshAppItemBlocking(item.copy(), useCache)
            }
            _storeItems.value = updatedList
        }
    }

    private suspend fun refreshAppItemBlocking(appItem: StoreItem, githubInfoUseCache: Boolean): StoreItem {
        return withContext(Dispatchers.IO) {
            val currentAppItem = appItem
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
            } else {
                Log.d(TAG, "Skipping GitHub release fetch for ${currentAppItem.packageName} due to cache preference and existing data.")
            }

            if (currentAppItem.state != AppEntryState.DOWNLOADING) {
                if (currentAppItem.isOutdated()) {
                    currentAppItem.state = AppEntryState.INSTALLED_AND_OUTDATED
                } else if (currentAppItem.installedVersion.isNotEmpty()) {
                    currentAppItem.state = AppEntryState.INSTALLED
                } else {
                    currentAppItem.state = AppEntryState.NOT_INSTALLED
                }
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
                // Potentially trigger XenonStore update check here as well or show a more prominent error
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
                    // Iterate to find the first non-draft release with assets
                    for (i in 0 until releasesArray.length()) {
                        val releaseNode = releasesArray.getJSONObject(i)
                        val isDraft = releaseNode.optBoolean("draft", false)
                        if (!isDraft) {
                            val assets = releaseNode.getJSONArray("assets")
                            if (assets.length() > 0) {
                                val asset = assets.getJSONObject(0) // Assuming the first asset is the APK
                                val newVersion = releaseNode.getString("tag_name")
                                return@withContext GithubReleaseInfo(newVersion, asset.getString("browser_download_url"))
                            }
                        }
                    }
                    throw IOException("No suitable non-draft release with assets found in /releases endpoint for $owner/$repo")
                } else { // Not checking for pre-releases, so /latest endpoint was used
                    val latestRelease = JSONObject(responseBody)
                    val newVersion = latestRelease.getString("tag_name")
                    val assets = latestRelease.getJSONArray("assets")
                    if (assets.length() > 0) {
                        val asset = assets.getJSONObject(0) // Assuming the first asset is the APK
                        GithubReleaseInfo(newVersion, asset.getString("browser_download_url"))
                    } else {
                        throw IOException("No assets found in the latest release for $owner/$repo")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get GitHub release for $owner/$repo (URL: $url): ${e.message}")
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
                    Log.d(TAG, "XenonStore not found or version couldn't be determined.")
                    _xenonStoreUpdateInfo.value = null // Or fetch latest if it's a first install scenario
                    return@launch
                }

                val latestReleaseInfo = getNewReleaseVersionGithubBlocking(XENON_STORE_OWNER, XENON_STORE_REPO)

                if (Util.isNewerVersion(installedVersion, latestReleaseInfo.version)) {
                    _xenonStoreUpdateInfo.value = latestReleaseInfo
                    Log.d(TAG, "XenonStore update available: ${latestReleaseInfo.version}")
                } else {
                    _xenonStoreUpdateInfo.value = null
                    Log.d(TAG, "XenonStore is up to date (Installed: $installedVersion, Latest: ${latestReleaseInfo.version})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check for XenonStore update: ${e.message}")
                _xenonStoreUpdateInfo.value = null
                // Optionally set _error.value if this check is critical
            }
        }
    }

    fun downloadAndInstallXenonStoreUpdate(context: Context) {
        val updateInfo = _xenonStoreUpdateInfo.value ?: return
        viewModelScope.launch {
            _currentActionInfo.value = "Downloading XenonStore update ${updateInfo.version}..."
            Log.d(TAG, "Downloading XenonStore update from: ${updateInfo.downloadUrl}")

            val fileName = "XenonStore_${updateInfo.version}.apk"
            val destinationFile = File(context.getExternalFilesDir(null), fileName)

            // No specific progress update for self-update in UI for now, just action info
            downloadFile(updateInfo.downloadUrl, destinationFile,
                onProgress = { bytesDownloaded, fileSize ->
                    // Log progress for now, could potentially update a specific state for XenonStore download
                    Log.d(TAG, "XenonStore Update Download Progress: $bytesDownloaded / $fileSize")
                },
                onCompleted = {
                    _currentActionInfo.value = "XenonStore download complete. Starting installation..."
                    initiateInstall(destinationFile, context, XENON_STORE_PACKAGE_NAME)
                    // After starting install, clear the update info as user will handle it via system UI
                    _xenonStoreUpdateInfo.value = null
                },
                onFailure = { errorMsg ->
                    _error.value = "XenonStore download failed: $errorMsg"
                    _currentActionInfo.value = null
                    // Keep _xenonStoreUpdateInfo so user can try again
                }
            )
        }
    }


    fun installApp(storeItem: StoreItem, context: Context) {
        viewModelScope.launch {
            _currentActionInfo.value = "Preparing to install ${storeItem.getName(Util.getCurrentLanguage(context.resources))}..."
            Log.d(TAG, "Install/Update clicked for: ${storeItem.packageName}")

            if (storeItem.downloadUrl.isEmpty()) {
                _error.value = "No download URL available for ${storeItem.getName(Util.getCurrentLanguage(context.resources))}."
                _currentActionInfo.value = null
                return@launch
            }

            val fileName = "${storeItem.packageName}_${storeItem.newVersion}.apk"
            val destinationFile = File(context.getExternalFilesDir(null), fileName)

            updateItemState(storeItem.packageName, AppEntryState.DOWNLOADING, bytesDownloaded = 0, fileSize = 100) // Placeholder fileSize

            downloadFile(storeItem.downloadUrl, destinationFile,
                onProgress = { bytesDownloaded, fileSize ->
                    updateItemState(storeItem.packageName, AppEntryState.DOWNLOADING, bytesDownloaded, fileSize)
                },
                onCompleted = {
                    _currentActionInfo.value = "Download complete. Starting installation..."
                    initiateInstall(destinationFile, context, storeItem.packageName)
                },
                onFailure = { errorMsg ->
                    _error.value = "Download failed: $errorMsg"
                    updateItemState(storeItem.packageName, AppEntryState.NOT_INSTALLED) // Or previous state
                    _currentActionInfo.value = null
                }
            )
        }
    }

    private fun initiateInstall(apkFile: File, context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // Ensure this matches your FileProvider authority
                apkFile
            )

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
                    // If this was a regular app install, we'd update its state.
                    // For XenonStore self-update, the app might restart or close, so state update here is less critical.
                    // If it was another app, you'd do: updateItemState(packageName, AppEntryState.NOT_INSTALLED)
                    _currentActionInfo.value = null
                    return
                }
            }
            context.startActivity(intent)
            Log.d(TAG, "Installation intent started for ${apkFile.name}")
            _currentActionInfo.value = "Installation process started by system."
             // If it was a regular app and not XenonStore itself, we might refresh its state after a delay.
            // For self-update, this is usually not necessary as the app will likely restart.
        } catch (e: Exception) {
            _error.value = "Failed to start installation: ${e.message}"
            Log.e(TAG, "Error initiating install: ", e)
            // if (packageName != XENON_STORE_PACKAGE_NAME) { // Only update state for other apps
            // updateItemState(packageName, AppEntryState.NOT_INSTALLED)
            // }
            _currentActionInfo.value = null
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
            _currentActionInfo.value = "Uninstallation process started by system."
        } catch (e: Exception) {
            _error.value = "Failed to start uninstall: ${e.message}"
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
            val currentList = _storeItems.value
            val itemIndex = currentList.indexOfFirst { it.packageName == packageName }
            if (itemIndex != -1) {
                val updatedItem = currentList[itemIndex].copy(
                    state = newState,
                    bytesDownloaded = bytesDownloaded,
                    fileSize = if (fileSize > 0) fileSize else currentList[itemIndex].fileSize
                )
                val newList = currentList.toMutableList()
                newList[itemIndex] = updatedItem
                _storeItems.value = newList.toList()
            }
        }
    }


    private fun downloadToString(
        url: String,
        listener: DownloadListener<String>,
        useCache: Boolean = true,
    ) {
        val request = Request.Builder().url(url).build()
        val currentClient = if (useCache) client else OkHttpClient()

        currentClient.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    listener.onFailure("Response error code: ${response.code} from $url")
                    return
                }
                response.body?.string()?.let {
                    listener.onCompleted(it)
                } ?: listener.onFailure("Empty body from $url")
            }

            override fun onFailure(call: Call, e: IOException) {
                listener.onFailure("Download failed for $url: ${e.message}")
            }
        })
    }

    private fun downloadFile(
        url: String,
        destinationFile: File,
        onProgress: (bytesDownloaded: Long, fileSize: Long) -> Unit,
        onCompleted: () -> Unit,
        onFailure: (errorMsg: String) -> Unit
    ) {
        val request = Request.Builder().url(url).build()
        // For self-update, always fetch fresh, do not rely on OkHttp default cache for the APK itself.
        val freshClient = OkHttpClient.Builder().cache(null).build() // Ensures no HTTP caching for this specific call

        freshClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                viewModelScope.launch { onFailure("Network error: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    viewModelScope.launch { onFailure("Server error: ${response.code}") }
                    return
                }
                val body = response.body
                if (body == null) {
                    viewModelScope.launch { onFailure("Empty response body") }
                    return
                }

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
                                if (fileSize > 0) {
                                    viewModelScope.launch { onProgress(bytesCopied, fileSize) }
                                }
                                bytes = inputStream.read(buffer)
                            }
                        }
                    }
                    if (bytesCopied > 0) {
                        viewModelScope.launch { onCompleted() }
                    } else {
                        viewModelScope.launch { onFailure("Zero bytes downloaded.") }
                    }
                } catch (e: IOException) {
                    viewModelScope.launch { onFailure("File I/O error: ${e.message}") }
                } finally {
                    body.close()
                }
            }
        })
    }


    interface DownloadListener<T> {
        fun onCompleted(result: T)
        fun onFailure(error: String)
    }

    fun clearError() {
        _error.value = null
    }

    fun clearActionInfo() {
        _currentActionInfo.value = null
    }

    override fun onCleared() {
        super.onCleared()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}

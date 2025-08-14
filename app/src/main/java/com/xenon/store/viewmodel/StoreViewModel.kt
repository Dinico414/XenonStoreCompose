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
import com.xenon.store.util.Util // Assuming Util.getCurrentLanguage and other utils
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
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// You'll need to provide the Application context via ViewModelFactory or Hilt
class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val _storeItems = MutableStateFlow<List<StoreItem>>(emptyList())
    val storeItems: StateFlow<List<StoreItem>> = _storeItems.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentActionInfo = MutableStateFlow<String?>(null)
    val currentActionInfo: StateFlow<String?> = _currentActionInfo.asStateFlow()

    private val client: OkHttpClient

    private companion object {
        const val APP_LIST_PROTOCOL_VERSION = "v0.1"
        const val TAG = "StoreViewModel"
    }

    private var cachedJsonHash: Int = 0

    init {
        val clientBuilder = OkHttpClient.Builder()
        client = clientBuilder.build()
        fetchAndRefreshAppList()
    }

    fun fetchAndRefreshAppList(useCache: Boolean = true) {
        viewModelScope.launch {
            _currentActionInfo.value = "Refreshing app list..."
            val urlString =
                "https://raw.githubusercontent.com/Dinico414/Xenon-Commons/master/accesspoint/src/main/java/com/xenon/commons/accesspoint/app_list.json" // Replace with your actual URL
            downloadToString(urlString, object : DownloadListener<String> {
                override fun onCompleted(result: String) {
                    val hash = result.hashCode()
                    if (useCache && cachedJsonHash != 0 && _storeItems.value.isNotEmpty() && cachedJsonHash == hash) {
                        Log.d(TAG, "App list JSON is unchanged, refreshing existing items.")
                        refreshAllAppItemsStates(useCache)
                        _currentActionInfo.value = "App list refreshed (cached)."
                        return
                    }
                    cachedJsonHash = hash
                    Log.d(TAG, "Parsing new app list JSON.")
                    val appList = parseAppListJson(result)
                    _storeItems.value = appList
                    refreshAllAppItemsStates(false) // Refresh states with new list data
                    _currentActionInfo.value = "App list updated."
                }

                override fun onFailure(errorMessage: String) {
                    _error.value = "Failed to fetch app list: $errorMessage"
                    _currentActionInfo.value = null
                }
            }, useCache)
        }
    }

    private fun refreshAllAppItemsStates(useCache: Boolean = true) {
        viewModelScope.launch {
            val updatedList = _storeItems.value.map { item ->
                refreshAppItemBlocking(item.copy(), useCache) // Use copy to ensure new object for StateFlow
            }
            _storeItems.value = updatedList
        }
    }

    private suspend fun refreshAppItemBlocking(appItem: StoreItem, useCache: Boolean = true): StoreItem {
        return withContext(Dispatchers.IO) {
            appItem.installedVersion = getInstalledAppVersion(appItem.packageName) ?: ""

            if (appItem.state != AppEntryState.DOWNLOADING) {
                if (appItem.isOutdated()) {
                    appItem.state = AppEntryState.INSTALLED_AND_OUTDATED
                } else if (appItem.installedVersion.isNotEmpty()) {
                    appItem.state = AppEntryState.INSTALLED
                } else {
                    appItem.state = AppEntryState.NOT_INSTALLED
                }
            }

            if (appItem.downloadUrl.isEmpty() || !useCache || appItem.newVersion.isEmpty()) {
                try {
                    val releaseInfo = getNewReleaseVersionGithubBlocking(appItem.owner, appItem.repo)
                    appItem.downloadUrl = releaseInfo.downloadUrl
                    if (appItem.isNewerVersion(releaseInfo.version)) {
                        appItem.newVersion = releaseInfo.version
                        if (appItem.state == AppEntryState.INSTALLED && appItem.isOutdated()) {
                            appItem.state = AppEntryState.INSTALLED_AND_OUTDATED
                        }
                    } else if (appItem.newVersion.isEmpty()) {
                        // If no newer version and newVersion was empty, set it to installed if available
                        appItem.newVersion = appItem.installedVersion.ifEmpty { releaseInfo.version }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get release info for ${appItem.packageName}: ${e.message}")
                    // Keep existing versions or mark as error if critical
                }
            }
            appItem // Return the modified item
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
        val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val request = Request.Builder().url(url).build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) throw IOException("Empty response body")

                val latestRelease = JSONObject(responseBody)
                val newVersion = latestRelease.getString("tag_name")
                val assets = latestRelease.getJSONArray("assets")
                if (assets.length() > 0) {
                    val asset = assets.getJSONObject(0) // Assuming the first asset is the APK
                    GithubReleaseInfo(newVersion, asset.getString("browser_download_url"))
                } else {
                    throw IOException("No assets found in the latest release")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get GitHub release for $owner/$repo: ${e.message}")
                throw e // Re-throw to be caught by caller
            }
        }
    }
    data class GithubReleaseInfo(val version: String, val downloadUrl: String)


    fun installApp(storeItem: StoreItem, context: Context) {
        viewModelScope.launch {
            _currentActionInfo.value = "Preparing to install ${storeItem.getName(Util.getCurrentLanguage(context.resources))}..."
            Log.d(TAG, "Install/Update clicked for: ${storeItem.packageName}")

            if (storeItem.downloadUrl.isEmpty()) {
                _error.value = "No download URL available for ${storeItem.getName(Util.getCurrentLanguage(context.resources))}."
                _currentActionInfo.value = null
                return@launch
            }

            // --- Basic Download and Install ---
            // THIS IS A SIMPLIFIED VERSION. YOU NEED A ROBUST DOWNLOADER AND FILEPROVIDER.
            val fileName = "${storeItem.packageName}_${storeItem.newVersion}.apk"
            val destinationFile = File(context.getExternalFilesDir(null), fileName)

            // Update item state to DOWNLOADING
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
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Important if calling from non-Activity context

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    _error.value = "Permission needed to install apps. Please enable in settings."
                    // Guide user to settings
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    updateItemState(packageName, AppEntryState.NOT_INSTALLED) // Reset state
                    _currentActionInfo.value = null
                    return
                }
            }
            context.startActivity(intent)
            // After this, the system takes over. You'll need to refresh the list
            // later to see if the installation was successful (e.g., in onResume or via broadcast receiver).
            // For now, we'll just log.
            Log.d(TAG, "Installation intent started for ${apkFile.name}")
            // Optimistically, you might set state to INSTALLED here, but it's better to verify.
            // For simplicity, we wait for a manual refresh or app restart.
            _currentActionInfo.value = "Installation process started by system."
            // Consider calling fetchAndRefreshAppList() after a short delay or when app resumes.
        } catch (e: Exception) {
            _error.value = "Failed to start installation: ${e.message}"
            Log.e(TAG, "Error initiating install: ", e)
            updateItemState(packageName, AppEntryState.NOT_INSTALLED) // Reset state
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
            // The system handles the uninstall dialog.
            // Refresh list later (e.g., onResume or via broadcast).
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
                _currentActionInfo.value = null // Clear info after opening
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
                    fileSize = if (fileSize > 0) fileSize else currentList[itemIndex].fileSize // Keep old if new is 0
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
        // Cache control can be more fine-grained with OkHttpClient if needed
        val currentClient = if (useCache) client else OkHttpClient() // Simplified cache handling

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
        client.newCall(request).enqueue(object : Callback {
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
                                if (fileSize > 0) { // Only report progress if total size is known
                                    viewModelScope.launch { onProgress(bytesCopied, fileSize) }
                                }
                                bytes = inputStream.read(buffer)
                            }
                        }
                    }
                    if (bytesCopied > 0) { // Ensure something was actually downloaded
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

    // Clear error message
    fun clearError() {
        _error.value = null
    }

    fun clearActionInfo() {
        _currentActionInfo.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel any ongoing operations if necessary
    }
}

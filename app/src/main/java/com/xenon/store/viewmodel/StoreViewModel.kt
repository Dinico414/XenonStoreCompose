package com.xenon.store.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xenon.store.util.Util
import com.xenon.store.viewmodel.classes.AppEntryState
import com.xenon.store.viewmodel.classes.StoreItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.EventListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class StoreViewModel : ViewModel() {

    private val _storeItems = MutableStateFlow<List<StoreItem>>(emptyList())
    val storeItems: StateFlow<List<StoreItem>> = _storeItems

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val client: OkHttpClient

    private companion object {
        const val appListProtocolVersion = "v0.1"
        const val tag = "StoreViewModel"
    }
    private var cachedJsonHash: Int = 0

    init {
        val clientBuilder = OkHttpClient.Builder()
        client = clientBuilder.build()
        fetchAndRefreshAppList()
    }

    fun fetchAndRefreshAppList(useCache: Boolean = true) {
        viewModelScope.launch {
            val urlString =
                "https://raw.githubusercontent.com/Dinico414/Xenon-Commons/master/accesspoint/src/main/java/com/xenon/commons/accesspoint/app_list.json"
            downloadToString(urlString, object : DownloadListener<String> {
                override fun onCompleted(result: String) {
                    val hash = result.hashCode()
                    if (cachedJsonHash != 0 && _storeItems.value.isNotEmpty() && cachedJsonHash == hash) {
                        refreshAppList(useCache)
                        return
                    }
                    cachedJsonHash = hash
                    val appList = parseAppListJson(result)
                    _storeItems.value = appList
                    refreshAppList()
                }

                override fun onFailure(error: String) {
                    _error.value = error
                }
            }, useCache)
        }
    }

    private fun refreshAppList(useCache: Boolean = true) {
        val currentList = _storeItems.value
        currentList.forEach {
            refreshAppItem(it, useCache)
        }
        _storeItems.value = currentList.toList()
    }

    private fun refreshAppItem(appItem: StoreItem, useCache: Boolean = true) {
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

        if (appItem.downloadUrl.isEmpty() || !useCache) {
            getNewReleaseVersionGithub(appItem.owner, appItem.repo, useCache,
                object : GithubReleaseAPICallback {
                    override fun onCompleted(version: String, downloadUrl: String) {
                        appItem.downloadUrl = downloadUrl
                        if (appItem.isNewerVersion(version)) {
                            appItem.newVersion = version
                            if (appItem.state == AppEntryState.INSTALLED) {
                                appItem.state = AppEntryState.INSTALLED_AND_OUTDATED
                            }
                        }
                    }

                    override fun onFailure(error: String) {
                        // Handle error
                    }
                })
        }
    }

    private fun getInstalledAppVersion(packageName: String): String? {
        // This needs context, should be passed or handled differently
        return null
    }


    private fun parseAppListJson(jsonString: String): ArrayList<StoreItem> {
        val json = JSONObject(jsonString)
        val appList = ArrayList<StoreItem>()

        val version = json.optString("protocolVersion") ?: "v0.0"
        if (Util.isNewerVersion(appListProtocolVersion, version)) {
            _error.value = "App is outdated"
            return appList
        }

        val list = json.getJSONArray("appList")
        for (i in 0 until list.length()) {
            val el = list.getJSONObject(i)

            val nameMap = HashMap<String, String>()
            val name = el.optString("name")
            if (name != null) nameMap["en"] = name
            val names = el.optJSONObject("names")
            if (names != null) {
                for (lang in names.keys()) {
                    nameMap[lang] = names.getString(lang)
                }
            }

            val appItem = StoreItem(
                nameMap,
                el.getString("icon"),
                el.getString("githubUrl"),
                el.getString("packageName")
            )
            appList.add(appItem)
        }
        return appList
    }

    private fun getNewReleaseVersionGithub(
        owner: String,
        repo: String,
        useCache: Boolean,
        callback: GithubReleaseAPICallback
    ) {
        val url = "https://api.github.com/repos/$owner/$repo/releases/latest"

        downloadToString(url, object : DownloadListener<String> {
            override fun onCompleted(result: String) {
                val latestRelease = JSONObject(result)
                val assets = latestRelease.getJSONArray("assets")
                val newVersion = latestRelease.getString("tag_name")
                if (assets.length() > 0) {
                    val asset = assets.getJSONObject(0)
                    callback.onCompleted(newVersion, asset.getString("browser_download_url"))
                } else {
                    callback.onFailure("No assets")
                }
            }

            override fun onFailure(error: String) {
                callback.onFailure(error)
            }
        }, useCache)
    }


    private fun downloadToString(
        url: String,
        progressListener: DownloadListener<String>,
        useCache: Boolean = true,
    ) {
        val request = Request.Builder()
            .url(url)
            .build()
        val currentClient = if (useCache) client else OkHttpClient()

        currentClient.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    progressListener.onFailure("Response error code: ${response.code}")
                    return
                }

                val responseBody = response.body?.string()
                if (responseBody == null) {
                    progressListener.onFailure("Empty body")
                } else {
                    progressListener.onCompleted(responseBody)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                progressListener.onFailure("Download failed: ${e.message}")
            }
        })
    }

    interface DownloadListener<T> {
        fun onCompleted(result: T)
        fun onFailure(error: String)
    }

    interface GithubReleaseAPICallback {
        fun onCompleted(version: String, downloadUrl: String)
        fun onFailure(error: String)
    }
}
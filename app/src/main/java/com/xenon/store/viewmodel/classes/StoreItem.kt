package com.xenon.store.viewmodel.classes

import android.content.Context
import com.xenon.store.util.Util

enum class AppEntryState {
    NOT_INSTALLED,
    DOWNLOADING,
    INSTALLED,
    INSTALLED_AND_OUTDATED,
}

data class StoreItem(
    val nameMap: HashMap<String, String>,
    val iconPath: String,
    val githubUrl: String,
    val packageName: String,
) {
    var id: Int = -1
    var state: AppEntryState = AppEntryState.NOT_INSTALLED
    var installedVersion: String = ""
    var newVersion: String = ""
    var installedIsPreRelease = false
    var newIsPreRelease = false

    fun isOutdated(): Boolean {
        return installedVersion.isNotEmpty() && isNewerVersion(newVersion)
    }

    // Download progressbar variables
    var bytesDownloaded: Long = 0
    var fileSize: Long = 0
    var downloadUrl: String = ""

    private val ownerRepoRegex = "^https://[^/]*github\\.com/([^/]+)/([^/]+)".toRegex()
    val owner = ownerRepoRegex.find(githubUrl)?.groups?.get(1)?.value ?: ""
    val repo = ownerRepoRegex.find(githubUrl)?.groups?.get(2)?.value ?: ""

    private val iconRegex = "^@([^/]+)/([^/]+)".toRegex()
    private val iconDirectory = iconRegex.find(iconPath)?.groups?.get(1)?.value
    private val iconName = iconRegex.find(iconPath)?.groups?.get(2)?.value

    fun getName(langCode: String): String {
        return nameMap[langCode] ?: nameMap["en"] ?: "App"
    }

    fun getDrawableId(context: Context): Int {
        if (iconDirectory == null || iconName == null) return 0
        return context.resources.getIdentifier(iconName, iconDirectory, context.packageName)
    }

    fun isNewerVersion(latestVersion: String): Boolean {
        return Util.isNewerVersion(installedVersion, latestVersion)
    }
}
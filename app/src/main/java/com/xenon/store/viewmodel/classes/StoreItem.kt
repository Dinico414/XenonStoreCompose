package com.xenon.store.viewmodel.classes

import android.content.Context
import android.content.res.Resources // For Util.getCurrentLanguage
import com.xenon.store.util.Util // Assuming Util.getCurrentLanguage

enum class AppEntryState {
    NOT_INSTALLED,
    DOWNLOADING,
    INSTALLED,
    INSTALLED_AND_OUTDATED
}

data class StoreItem(
    val nameMap: Map<String, String>,
    val iconPath: String,
    val githubUrl: String,
    val packageName: String,

    var state: AppEntryState = AppEntryState.NOT_INSTALLED,
    var installedVersion: String = "",
    var newVersion: String = "",
    var downloadUrl: String = "",
    var bytesDownloaded: Long = 0L,
    var fileSize: Long = 0L
) {
    val owner: String
        get() = githubUrl.split("/").getOrNull(3) ?: ""
    val repo: String
        get() = githubUrl.split("/").getOrNull(4) ?: ""

    fun getName(language: String): String {
        return nameMap[language] ?: nameMap["en"] ?: packageName
    }

    fun isOutdated(): Boolean {
        if (installedVersion.isEmpty() || newVersion.isEmpty()) return false
        return Util.isNewerVersion(installedVersion, newVersion)
    }

    fun isNewerVersion(remoteVersion: String): Boolean {
        if (newVersion.isEmpty() && remoteVersion.isNotEmpty()) return true
        return Util.isNewerVersion(newVersion, remoteVersion)
    }

    fun getDrawableId(context: Context): Int {
        return try {
            context.resources.getIdentifier(iconPath, "drawable", context.packageName)
        } catch (e: Exception) {
            0
        }
    }
}

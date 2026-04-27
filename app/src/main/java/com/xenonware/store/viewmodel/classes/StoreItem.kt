package com.xenonware.store.viewmodel.classes

import android.content.Context
import com.xenonware.store.util.Util
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class AppEntryState {
    NOT_INSTALLED,
    DOWNLOADING,
    INSTALLING,
    INSTALLED,
    INSTALLED_AND_OUTDATED
}

@Serializable
data class StoreResponse(
    val protocolVersion: String,
    val appList: List<StoreItem>
)

@Serializable
data class StoreItem(
    @SerialName("name") val name: String = "",
    @SerialName("names") val nameMap: Map<String, String> = emptyMap(),
    @SerialName("icon") val iconPath: String,
    @SerialName("githubUrl") val githubUrl: String,
    @SerialName("packageName") val packageName: String,
    val isCustom: Boolean = false,

    @Transient var state: AppEntryState = AppEntryState.NOT_INSTALLED,
    @Transient var installedVersion: String = "",
    @SerialName("version") var newVersion: String = "",
    @SerialName("downloadUrl") var downloadUrl: String = "",
    @Transient var bytesDownloaded: Long = 0L,
    @Transient var fileSize: Long = 0L,

    @SerialName("preVersion") var preVersion: String? = null,
    @SerialName("preDownloadUrl") var preDownloadUrl: String? = null
) {
    val owner: String
        get() = githubUrl.split("/").getOrNull(3) ?: ""

    val repo: String
        get() = githubUrl.split("/").getOrNull(4) ?: ""

    fun getName(language: String): String {
        return nameMap[language] ?: nameMap["en"] ?: name.ifEmpty { packageName }
    }

    fun isOutdated(): Boolean {
        if (installedVersion.isEmpty() || newVersion.isEmpty()) return false
        return Util.Companion.isNewerVersion(installedVersion, newVersion)
    }

    fun getDrawableId(context: Context): Int {
        return try {
            // Handles full resource strings like "@mipmap/calculator"
            context.resources.getIdentifier(iconPath, null, null)
        } catch (e: Exception) {
            0
        }
    }
}

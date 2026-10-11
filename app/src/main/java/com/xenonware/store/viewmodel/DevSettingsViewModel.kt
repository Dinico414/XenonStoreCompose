package com.xenonware.store.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.store.data.InstallMethod
import com.xenonware.store.data.SharedPreferenceManager
import com.xenonware.store.presentation.sign_in.GitHubAuthUiClient
import com.xenonware.store.viewmodel.classes.GitHubRepoItem
import com.xenonware.store.viewmodel.classes.GitHubSearchResponse
import com.xenonware.store.viewmodel.classes.StoreItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class DevSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferenceManager = SharedPreferenceManager(application)
    private val gitHubAuthUiClient = GitHubAuthUiClient(application)

    private val _devModeToggleState = MutableStateFlow(sharedPreferenceManager.developerModeEnabled)
    val devModeToggleState: StateFlow<Boolean> = _devModeToggleState

    private val _addButtonState =
        MutableStateFlow(sharedPreferenceManager.addButtonEnabled)
    val addButtonState: StateFlow<Boolean> = _addButtonState

    private val _installMethodState = MutableStateFlow(sharedPreferenceManager.installMethod)
    val installMethodState: StateFlow<InstallMethod> = _installMethodState

    private val _githubApps = MutableStateFlow<List<StoreItem>>(emptyList())
    val githubApps: StateFlow<List<StoreItem>> = _githubApps.asStateFlow()

    private val _editingApp = MutableStateFlow<StoreItem?>(null)
    val editingApp: StateFlow<StoreItem?> = _editingApp.asStateFlow()

    private val _customAppsUpdated = MutableSharedFlow<Unit>(replay = 1)
    val customAppsUpdated = _customAppsUpdated.asSharedFlow()

    // GitHub Login and Search state
    private val _isGitHubLoggedIn = MutableStateFlow(sharedPreferenceManager.isGitHubLoggedIn)
    val isGitHubLoggedIn: StateFlow<Boolean> = _isGitHubLoggedIn.asStateFlow()

    private val _githubUsername = MutableStateFlow(sharedPreferenceManager.githubUsername)
    val githubUsername: StateFlow<String> = _githubUsername.asStateFlow()

    private val _githubAvatarUrl = MutableStateFlow(sharedPreferenceManager.githubAvatarUrl)
    val githubAvatarUrl: StateFlow<String> = _githubAvatarUrl.asStateFlow()

    private val _githubRepoSearchResults = MutableStateFlow<List<GitHubRepoItem>>(emptyList())
    val githubRepoSearchResults: StateFlow<List<GitHubRepoItem>> = _githubRepoSearchResults.asStateFlow()

    private val httpClient = OkHttpClient.Builder().build()
    private val jsonSerializer = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            SharedPreferenceManager.KEY_DEVELOPER_MODE -> {
                _devModeToggleState.value = sharedPreferenceManager.developerModeEnabled
            }
            SharedPreferenceManager.KEY_ADD_BUTTON_STATE -> {
                _addButtonState.value = sharedPreferenceManager.addButtonEnabled
            }
            SharedPreferenceManager.KEY_INSTALL_METHOD -> {
                _installMethodState.value = sharedPreferenceManager.installMethod
            }
            SharedPreferenceManager.KEY_CUSTOM_STORE_ITEMS -> {
                loadGithubApps()
            }
            SharedPreferenceManager.KEY_IS_GITHUB_LOGGED_IN -> {
                _isGitHubLoggedIn.value = sharedPreferenceManager.isGitHubLoggedIn
            }
            SharedPreferenceManager.KEY_GITHUB_USERNAME -> {
                _githubUsername.value = sharedPreferenceManager.githubUsername
            }
            SharedPreferenceManager.KEY_GITHUB_AVATAR_URL -> {
                _githubAvatarUrl.value = sharedPreferenceManager.githubAvatarUrl
            }
        }
    }

    init {
        sharedPreferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        loadGithubApps()
        val user = gitHubAuthUiClient.getSignedInUser()
        if (user != null) {
            _isGitHubLoggedIn.value = true
            _githubUsername.value = user.username ?: ""
            _githubAvatarUrl.value = user.profilePictureUrl ?: ""
        }
    }

    private fun loadGithubApps() {
        viewModelScope.launch {
            _githubApps.value = sharedPreferenceManager.loadCustomStoreItems()
        }
    }

    fun loginWithGitHub(activity: Activity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = gitHubAuthUiClient.signIn(activity)
            if (result.data != null) {
                _isGitHubLoggedIn.value = true
                _githubUsername.value = result.data.username ?: ""
                _githubAvatarUrl.value = result.data.profilePictureUrl ?: ""
                Toast.makeText(getApplication(), "Authenticated as ${result.data.username}", Toast.LENGTH_SHORT).show()
                onSuccess()
            } else {
                onError(result.errorMessage ?: "GitHub authentication failed")
            }
        }
    }

    fun logoutGitHub() {
        gitHubAuthUiClient.signOut()
        _isGitHubLoggedIn.value = false
        _githubUsername.value = ""
        _githubAvatarUrl.value = ""
        _githubRepoSearchResults.value = emptyList()
        Toast.makeText(getApplication(), "Logged out of GitHub", Toast.LENGTH_SHORT).show()
    }

    fun searchGitHubRepos(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _githubRepoSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            // First attempt: search dedicated Komi Store / Android app repository catalog
            try {
                val komiUrl = "https://api.github-store.org/v1/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}&platform=android"
                val komiReq = Request.Builder().url(komiUrl).build()
                httpClient.newCall(komiReq).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        val root = jsonSerializer.parseToJsonElement(body)
                        val itemsArray = (root as? kotlinx.serialization.json.JsonObject)?.get("items") as? kotlinx.serialization.json.JsonArray
                        if (itemsArray != null && itemsArray.isNotEmpty()) {
                            val items = itemsArray.mapNotNull { element ->
                                val obj = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                                val name = obj["name"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: ""
                                val fullName = obj["fullName"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: ""
                                val desc = obj["description"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                                val ownerObj = obj["owner"] as? kotlinx.serialization.json.JsonObject
                                val ownerLogin = ownerObj?.get("login")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: ""
                                val ownerAvatar = ownerObj?.get("avatarUrl")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                                val hasAndroid = obj["hasInstallersAndroid"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBoolean() } ?: true
                                if (hasAndroid && name.isNotBlank() && ownerLogin.isNotBlank()) {
                                    GitHubRepoItem(
                                        name = name,
                                        fullName = fullName.ifBlank { "$ownerLogin/$name" },
                                        owner = com.xenonware.store.viewmodel.classes.GitHubRepoOwner(
                                            login = ownerLogin,
                                            avatarUrl = ownerAvatar
                                        ),
                                        description = desc
                                    )
                                } else null
                            }
                            if (items.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    _githubRepoSearchResults.value = items
                                }
                                return@launch
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            // Second attempt: Search GitHub specifically for Android repositories (topic:android, language:Kotlin/Java)
            try {
                val token = sharedPreferenceManager.githubToken
                val androidQuery = "${trimmed} topic:android"
                val encodedQuery = java.net.URLEncoder.encode(androidQuery, "UTF-8")
                val reqBuilder = Request.Builder()
                    .url("https://api.github.com/search/repositories?q=$encodedQuery&sort=stars&order=desc")
                    .addHeader("Accept", "vnd.github+json")

                if (token.isNotBlank()) {
                    reqBuilder.addHeader("Authorization", "Bearer $token")
                }

                httpClient.newCall(reqBuilder.build()).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        val searchResponse = jsonSerializer.decodeFromString<GitHubSearchResponse>(body)
                        if (searchResponse.items.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                _githubRepoSearchResults.value = searchResponse.items
                            }
                            return@launch
                        }
                    }
                }

                // If topic:android had 0 results, fallback to search with language or raw query
                val fallbackReq = Request.Builder()
                    .url("https://api.github.com/search/repositories?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}&sort=stars&order=desc")
                    .addHeader("Accept", "vnd.github+json")
                if (token.isNotBlank()) {
                    fallbackReq.addHeader("Authorization", "Bearer $token")
                }
                httpClient.newCall(fallbackReq.build()).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        val searchResponse = jsonSerializer.decodeFromString<GitHubSearchResponse>(body)
                        withContext(Dispatchers.Main) {
                            _githubRepoSearchResults.value = searchResponse.items
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun clearGitHubSearchResults() {
        _githubRepoSearchResults.value = emptyList()
    }

    fun onEditGitHubApp(app: StoreItem) {
        _editingApp.value = app
    }

    fun onSaveGitHubApp(owner: String, repo: String, packageName: String) {
        viewModelScope.launch {
            val resolvedPkg = if (packageName.startsWith("com.${owner.lowercase().replace("-", "_")}") || packageName.isBlank()) {
                val real = com.xenonware.store.util.GitHubPackageResolver.resolvePackageName(owner, repo, sharedPreferenceManager.githubToken)
                real.ifBlank { packageName }
            } else {
                packageName
            }

            val currentApps = _githubApps.value.toMutableList()
            val editingApp = _editingApp.value

            if (editingApp != null) {
                // Update existing app
                val index = currentApps.indexOfFirst { it.packageName == editingApp.packageName }
                if (index != -1) {
                    val updatedApp = editingApp.copy(
                        nameMap = hashMapOf("en" to repo),
                        githubUrl = "https://github.com/$owner/$repo",
                        packageName = resolvedPkg
                    )
                    currentApps[index] = updatedApp
                }
            } else {
                // Add new app
                val newApp = StoreItem(
                    nameMap = hashMapOf("en" to repo),
                    iconPath = "https://github.com/$owner.png",
                    githubUrl = "https://github.com/$owner/$repo",
                    packageName = resolvedPkg,
                    isCustom = true
                )
                if (currentApps.none { it.packageName == newApp.packageName }) {
                    currentApps.add(newApp)
                }
            }

            sharedPreferenceManager.saveCustomStoreItems(currentApps)
            _githubApps.value = currentApps
            _editingApp.value = null
            _customAppsUpdated.tryEmit(Unit)
        }
    }

    fun onDialogDismiss() {
        _editingApp.value = null
        clearGitHubSearchResults()
    }

    fun onDeleteGitHubApp(app: StoreItem) {
        viewModelScope.launch {
            val currentApps = _githubApps.value.toMutableList()
            if (currentApps.remove(app)) {
                sharedPreferenceManager.saveCustomStoreItems(currentApps)
                _githubApps.value = currentApps
                _customAppsUpdated.tryEmit(Unit)
            }
        }
    }

    fun deleteAllDownloadedFiles(context: Context): Int {
        var count = 0
        try {
            val installDir = File(context.filesDir, "apks")
            if (installDir.exists() && installDir.isDirectory) {
                installDir.listFiles()?.forEach { file ->
                    if (file.isFile && (file.extension.equals("apk", ignoreCase = true) || file.name.startsWith("download_"))) {
                        if (file.delete()) {
                            count++
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        sharedPreferenceManager.sharedPreferences.edit {
            putLong(SharedPreferenceManager.KEY_DOWNLOADED_FILES_UPDATED, System.currentTimeMillis())
        }
        viewModelScope.launch {
            _customAppsUpdated.emit(Unit)
        }
        return count
    }

    fun setDeveloperModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sharedPreferenceManager.developerModeEnabled = enabled
            _devModeToggleState.value = enabled
            _customAppsUpdated.tryEmit(Unit)

            if (!enabled) {
                setAddButtonEnabled(false)
            }
        }
    }

    fun setAddButtonEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (sharedPreferenceManager.addButtonEnabled != enabled) {
                sharedPreferenceManager.addButtonEnabled = enabled
                _addButtonState.value = enabled
                _customAppsUpdated.tryEmit(Unit)
            }
        }
    }

    fun setInstallMethod(installMethod: InstallMethod) {
        viewModelScope.launch {
            if (sharedPreferenceManager.installMethod != installMethod) {
                sharedPreferenceManager.installMethod = installMethod
                _installMethodState.value = installMethod
                Toast.makeText(
                    getApplication(),
                    "Install method updated to ${installMethod.name}.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCleared() {
        sharedPreferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }
}

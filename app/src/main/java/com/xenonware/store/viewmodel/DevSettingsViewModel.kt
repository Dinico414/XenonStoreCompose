package com.xenonware.store.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.store.InstallMethod
import com.xenonware.store.SharedPreferenceManager
import com.xenonware.store.ShizukuManager
import com.xenonware.store.viewmodel.classes.StoreItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DevSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferenceManager = SharedPreferenceManager(application)

    private val _devModeToggleState = MutableStateFlow(sharedPreferenceManager.developerModeEnabled)
    val devModeToggleState: StateFlow<Boolean> = _devModeToggleState

    private val _showDummyProfileState =
        MutableStateFlow(sharedPreferenceManager.showDummyProfileEnabled)
    val showDummyProfileState: StateFlow<Boolean> = _showDummyProfileState

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

    val isShizukuAvailable: StateFlow<Boolean> = ShizukuManager.isAvailable.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    init {
        loadGithubApps()
    }

    private fun loadGithubApps() {
        viewModelScope.launch {
            _githubApps.value = sharedPreferenceManager.loadCustomStoreItems()
        }
    }

    fun onEditGitHubApp(app: StoreItem) {
        _editingApp.value = app
    }

    fun onSaveGitHubApp(owner: String, repo: String, packageName: String) {
        viewModelScope.launch {
            val currentApps = _githubApps.value.toMutableList()
            val editingApp = _editingApp.value

            if (editingApp != null) {
                // Update existing app
                val index = currentApps.indexOfFirst { it.packageName == editingApp.packageName }
                if (index != -1) {
                    val updatedApp = editingApp.copy(
                        nameMap = hashMapOf("en" to repo),
                        githubUrl = "https://github.com/$owner/$repo",
                        packageName = packageName
                    )
                    currentApps[index] = updatedApp
                }
            } else {
                // Add new app
                val newApp = StoreItem(
                    nameMap = hashMapOf("en" to repo),
                    iconPath = "",
                    githubUrl = "https://github.com/$owner/$repo",
                    packageName = packageName,
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

    fun setDeveloperModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sharedPreferenceManager.developerModeEnabled = enabled
            _devModeToggleState.value = enabled

            if (!enabled) {
                setShowDummyProfileEnabled(false)
                setAddButtonEnabled(false)
            }
        }
    }

    fun setShowDummyProfileEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (sharedPreferenceManager.showDummyProfileEnabled != enabled) {
                sharedPreferenceManager.showDummyProfileEnabled = enabled
                _showDummyProfileState.value = enabled

                triggerExampleDevActionThatRequiresRestart()
            }
        }
    }

    fun setAddButtonEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (sharedPreferenceManager.addButtonEnabled != enabled) {
                sharedPreferenceManager.addButtonEnabled = enabled
                _addButtonState.value = enabled

                triggerExampleDevActionThatRequiresRestart()
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

    fun triggerExampleDevActionThatRequiresRestart() {
        viewModelScope.launch {
            Toast.makeText(
                getApplication(), "To apply changes, restart the app.", Toast.LENGTH_LONG
            ).show()
        }
    }
}

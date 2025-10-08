package com.xenonware.store.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.store.InstallMethod
import com.xenonware.store.SharedPreferenceManager
import com.xenonware.store.ShizukuManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DevSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferenceManager =
        _root_ide_package_.com.xenonware.store.SharedPreferenceManager(application)

    private val _devModeToggleState = MutableStateFlow(sharedPreferenceManager.developerModeEnabled)
    val devModeToggleState: StateFlow<Boolean> = _devModeToggleState

    private val _showDummyProfileState = MutableStateFlow(sharedPreferenceManager.showDummyProfileEnabled)
    val showDummyProfileState: StateFlow<Boolean> = _showDummyProfileState

    private val _installMethodState = MutableStateFlow(sharedPreferenceManager.installMethod)
    val installMethodState: StateFlow<com.xenonware.store.InstallMethod> = _installMethodState

    val isShizukuAvailable: StateFlow<Boolean> = _root_ide_package_.com.xenonware.store.ShizukuManager.isAvailable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDeveloperModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sharedPreferenceManager.developerModeEnabled = enabled
            _devModeToggleState.value = enabled

            if (!enabled) {
                setShowDummyProfileEnabled(false)
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

    fun setInstallMethod(installMethod: com.xenonware.store.InstallMethod) {
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
                getApplication(),
                "To apply changes, restart the app.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

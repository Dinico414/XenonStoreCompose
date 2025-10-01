package com.xenon.store.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenon.store.InstallMethod
import com.xenon.store.SharedPreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DevSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferenceManager = SharedPreferenceManager(application)

    private val _devModeToggleState = MutableStateFlow(sharedPreferenceManager.developerModeEnabled)
    val devModeToggleState: StateFlow<Boolean> = _devModeToggleState.asStateFlow()

    private val _showDummyProfileState = MutableStateFlow(sharedPreferenceManager.showDummyProfileEnabled)
    val showDummyProfileState: StateFlow<Boolean> = _showDummyProfileState.asStateFlow()

    private val _installMethodState = MutableStateFlow(sharedPreferenceManager.installMethod)
    val installMethodState: StateFlow<InstallMethod> = _installMethodState.asStateFlow()

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

    fun setInstallMethod(installMethod: InstallMethod) {
        viewModelScope.launch {
            if (sharedPreferenceManager.installMethod != installMethod) {
                // If Shizuku was selected but is no longer an option, revert to Default
                val newMethod = if (installMethod == InstallMethod.SHIZUKU) InstallMethod.DEFAULT else installMethod
                sharedPreferenceManager.installMethod = newMethod
                _installMethodState.value = newMethod
                Toast.makeText(
                    getApplication(),
                    "Install method updated to ${newMethod.name}.",
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

package com.xenonware.store

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

object ShizukuManager {

    private const val TAG = "ShizukuManager"

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable = _isAvailable.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.i(TAG, "Shizuku binder received, service is available.")
        _isAvailable.value = true
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.w(TAG, "Shizuku binder dead, service is unavailable.")
        _isAvailable.value = false
    }

    fun register() {
        Log.d(TAG, "Registering Shizuku listeners.")
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        _isAvailable.value = Shizuku.pingBinder()
        Log.i(TAG, "Initial Shizuku check: ${_isAvailable.value}")
    }

    fun unregister() {
        Log.d(TAG, "Unregistering Shizuku listeners.")
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Failed to unregister listeners, Shizuku service is not running.")
        }
    }
}

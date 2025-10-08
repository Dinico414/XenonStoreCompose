package com.xenonware.store

import android.app.Application

class MainApplication : android.app.Application() {

    override fun onCreate() {
        super.onCreate()
        ShizukuManager.register()
    }

    override fun onTerminate() {
        super.onTerminate()
        ShizukuManager.unregister()
    }
}

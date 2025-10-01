package com.xenon.store

import android.app.Application

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ShizukuManager.register()
    }

    override fun onTerminate() {
        super.onTerminate()
        ShizukuManager.unregister()
    }
}

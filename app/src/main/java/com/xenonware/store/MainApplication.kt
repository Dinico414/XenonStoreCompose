package com.xenonware.store

import android.app.Application
import com.xenonware.store.util.ShizukuManager

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

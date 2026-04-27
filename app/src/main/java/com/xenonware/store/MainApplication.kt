package com.xenonware.store

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.xenonware.store.util.ShizukuManager
import com.xenonware.store.worker.UpdateCheckWorker
import java.util.concurrent.TimeUnit

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ShizukuManager.register()
        scheduleUpdateChecks()
    }

    private fun scheduleUpdateChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val updateRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "UpdateCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            updateRequest
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        ShizukuManager.unregister()
    }
}

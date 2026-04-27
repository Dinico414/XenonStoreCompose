package com.xenonware.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import rikka.shizuku.Shizuku

class TransparentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Shizuku.requestPermission(0)
        } catch (e: Exception) {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // If we got here, we might have just returned from the permission dialog
        if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            finish()
        }
    }
}

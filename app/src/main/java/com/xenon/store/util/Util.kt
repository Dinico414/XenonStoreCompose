package com.xenon.store.util

import android.content.res.Resources

class Util {
    companion object {
        fun getCurrentLanguage(resources: Resources): String {
            return resources.configuration.locales.get(0).language
        }

        fun isNewerVersion(installedVersion: String, latestVersion: String): Boolean {
            if (installedVersion.isEmpty()) return true

            val latestParts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val installedParts = installedVersion.split(".").map { it.toIntOrNull() ?: 0 }

            for (i in 0 until maxOf(latestParts.size, installedParts.size)) {
                val latestPart = latestParts.getOrElse(i) { 0 }
                val installedPart = installedParts.getOrElse(i) { 0 }

                if (latestPart > installedPart) {
                    return true
                } else if (latestPart < installedPart) {
                    return false
                }
            }
            return false
        }
    }
}
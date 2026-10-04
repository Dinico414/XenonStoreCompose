package com.xenonware.store.util

import android.content.res.Resources

class Util {
    companion object {
        fun getCurrentLanguage(resources: Resources): String {
            return resources.configuration.locales.get(0).language
        }

        fun isNewerVersion(installedVersion: String, latestVersion: String): Boolean {
            if (installedVersion.isEmpty()) return true
            if (latestVersion.isEmpty()) return false

            val cleanInstalled = installedVersion.trim().removePrefix("v").removePrefix("V")
            val cleanLatest = latestVersion.trim().removePrefix("v").removePrefix("V")

            // Remove any trailing commit hash or metadata (e.g., -beta.1, -rc, -d)
            val installedBase = cleanInstalled.substringBefore("-").substringBefore("+")
            val latestBase = cleanLatest.substringBefore("-").substringBefore("+")

            val latestParts = latestBase.split(".").map { it.toIntOrNull() ?: 0 }
            val installedParts = installedBase.split(".").map { it.toIntOrNull() ?: 0 }

            for (i in 0 until maxOf(latestParts.size, installedParts.size)) {
                val latestPart = latestParts.getOrElse(i) { 0 }
                val installedPart = installedParts.getOrElse(i) { 0 }

                if (latestPart > installedPart) {
                    return true
                } else if (latestPart < installedPart) {
                    return false
                }
            }

            // If base numbers are equal (e.g. 2.0.0 vs 2.0.0-beta.1), a stable release without suffix is newer than a pre-release with suffix
            val installedHasSuffix = cleanInstalled.contains("-")
            val latestHasSuffix = cleanLatest.contains("-")
            if (installedHasSuffix && !latestHasSuffix) {
                return true
            }

            return false
        }
    }
}
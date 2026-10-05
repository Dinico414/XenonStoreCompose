package com.xenonware.store.util

import android.content.res.Resources

class Util {
    companion object {
        fun getCurrentLanguage(resources: Resources): String {
            return resources.configuration.locales.get(0).language
        }

        fun isNewerVersion(installedVersion: String, latestVersion: String): Boolean {
            if (installedVersion.isBlank()) return latestVersion.isNotBlank()
            if (latestVersion.isBlank()) return false

            return compareVersions(latestVersion, installedVersion) > 0
        }

        fun compareVersions(v1: String, v2: String): Int {
            if (v1.isBlank() && v2.isBlank()) return 0
            if (v1.isBlank()) return -1
            if (v2.isBlank()) return 1

            val clean1 = v1.trim().removePrefix("v").removePrefix("V")
            val clean2 = v2.trim().removePrefix("v").removePrefix("V")

            if (clean1.equals(clean2, ignoreCase = true)) return 0

            val base1 = clean1.substringBefore("-").substringBefore("+")
            val base2 = clean2.substringBefore("-").substringBefore("+")

            val parts1 = base1.split(".").map { it.toIntOrNull() ?: 0 }
            val parts2 = base2.split(".").map { it.toIntOrNull() ?: 0 }

            for (i in 0 until maxOf(parts1.size, parts2.size)) {
                val p1 = parts1.getOrElse(i) { 0 }
                val p2 = parts2.getOrElse(i) { 0 }
                if (p1 != p2) {
                    return p1.compareTo(p2)
                }
            }

            // Base version numbers are equal. Compare pre-release suffixes (after '-')
            val hasSuffix1 = clean1.contains("-")
            val hasSuffix2 = clean2.contains("-")

            if (!hasSuffix1 && hasSuffix2) {
                // v1 has no suffix (stable), v2 has suffix (pre-release) -> stable is newer
                return 1
            }
            if (hasSuffix1 && !hasSuffix2) {
                // v1 has suffix (pre-release), v2 has no suffix (stable) -> pre-release is older
                return -1
            }
            if (hasSuffix1) {
                // Both have suffixes because neither was without suffix
                val suffix1 = clean1.substringAfter("-").substringBefore("+")
                val suffix2 = clean2.substringAfter("-").substringBefore("+")

                val tokens1 = extractVersionTokens(suffix1)
                val tokens2 = extractVersionTokens(suffix2)

                for (i in 0 until maxOf(tokens1.size, tokens2.size)) {
                    val t1 = tokens1.getOrNull(i)
                    val t2 = tokens2.getOrNull(i)
                    if (t1 == null) return -1 // Fewer pre-release tokens has lower precedence (e.g., beta < beta.1)
                    if (t2 == null) return 1

                    val num1 = t1.toLongOrNull()
                    val num2 = t2.toLongOrNull()

                    if (num1 != null && num2 != null) {
                        if (num1 != num2) return num1.compareTo(num2)
                    } else if (num1 != null) {
                        // Numeric token has lower precedence than non-numeric token (e.g., 1 < alpha)
                        return -1
                    } else if (num2 != null) {
                        return 1
                    } else {
                        val cmp = t1.compareTo(t2, ignoreCase = true)
                        if (cmp != 0) return cmp
                    }
                }
            }

            // If still equal, compare build metadata (after '+') if present
            val hasBuild1 = clean1.contains("+")
            val hasBuild2 = clean2.contains("+")
            if (hasBuild1 && hasBuild2) {
                val b1 = clean1.substringAfter("+")
                val b2 = clean2.substringAfter("+")
                val tokens1 = extractVersionTokens(b1)
                val tokens2 = extractVersionTokens(b2)

                for (i in 0 until maxOf(tokens1.size, tokens2.size)) {
                    val t1 = tokens1.getOrNull(i) ?: return -1
                    val t2 = tokens2.getOrNull(i) ?: return 1

                    val num1 = t1.toLongOrNull()
                    val num2 = t2.toLongOrNull()
                    if (num1 != null && num2 != null) {
                        if (num1 != num2) return num1.compareTo(num2)
                    } else {
                        val cmp = t1.compareTo(t2, ignoreCase = true)
                        if (cmp != 0) return cmp
                    }
                }
            }

            return 0
        }

        private fun extractVersionTokens(str: String): List<String> {
            val regex = Regex("([0-9]+|[a-zA-Z]+)")
            return regex.findAll(str).map { it.value }.toList()
        }
    }
}
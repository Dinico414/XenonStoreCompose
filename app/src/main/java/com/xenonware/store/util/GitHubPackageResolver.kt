package com.xenonware.store.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

object GitHubPackageResolver {

    private val httpClient = OkHttpClient.Builder().build()

    private val GITHUB_RAW_CANDIDATE_PATHS = listOf(
        "composeApp/build.gradle.kts",
        "app/build.gradle.kts",
        "gradle/libs.versions.toml",
        "composeApp/src/androidMain/AndroidManifest.xml",
        "app/src/main/AndroidManifest.xml",
        "android/app/build.gradle.kts",
        "androidApp/build.gradle.kts",
        "android/app/build.gradle",
        "app/build.gradle",
        "androidApp/src/main/AndroidManifest.xml",
        "android/app/src/main/AndroidManifest.xml",
        "src/main/AndroidManifest.xml"
    )

    private val BRANCH_CANDIDATES = listOf("main", "master")

    /**
     * Attempts to find the real Android package name / applicationId for a GitHub repository.
     * 1. Inspects remote build.gradle.kts / libs.versions.toml / manifest files in GitHub repo first
     * 2. Checks F-Droid package search
     * 3. Falls back to normalized fallback: com.<owner>.<repo>
     */
    suspend fun resolvePackageName(owner: String, repo: String, token: String = ""): String = withContext(Dispatchers.IO) {
        if (owner.isBlank() || repo.isBlank()) return@withContext ""

        // 1. Query GitHub build.gradle.kts / toml / manifest files first
        for (branch in BRANCH_CANDIDATES) {
            for (path in GITHUB_RAW_CANDIDATE_PATHS) {
                val url = "https://raw.githubusercontent.com/$owner/$repo/$branch/$path"
                val content = fetchUrl(url, token) ?: continue

                val pkg = extractPackageFromContent(content, path)
                if (pkg != null && isValidPackageName(pkg)) {
                    return@withContext pkg
                }
            }
        }

        // 2. Try finding by repo / name in F-Droid
        val fdroidResult = queryFDroid(owner, repo)
        if (fdroidResult.isNotBlank()) {
            return@withContext fdroidResult
        }

        // 3. Fallback
        val cleanOwner = owner.lowercase().replace("-", "_").replace(".", "_")
        val cleanRepo = repo.lowercase().replace("-", "_").replace(".", "_")
        "com.$cleanOwner.$cleanRepo"
    }

    private fun queryFDroid(owner: String, repo: String): String {
        return try {
            val searchUrl = "https://search.f-droid.org/?q=${repo.trim()}"
            val request = Request.Builder().url(searchUrl).header("User-Agent", "Mozilla/5.0").build()
            val html = httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body.string() else null
            } ?: return ""

            val pattern = Pattern.compile("https://f-droid\\.org/en/packages/([a-zA-Z0-9_.]+)/?")
            val matcher = pattern.matcher(html)
            while (matcher.find()) {
                val candidatePkg = matcher.group(1) ?: continue
                if (isPackageMatchForRepo(candidatePkg, owner, repo)) {
                    return candidatePkg
                }
            }
            ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun isPackageMatchForRepo(pkg: String, owner: String, repo: String): Boolean {
        return try {
            val pkgUrl = "https://f-droid.org/en/packages/$pkg/"
            val request = Request.Builder().url(pkgUrl).header("User-Agent", "Mozilla/5.0").build()
            val detailHtml = httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body.string() else null
            } ?: return false

            val expectedRepo = "$owner/$repo".lowercase()
            detailHtml.lowercase().contains(expectedRepo)
        } catch (_: Exception) {
            false
        }
    }

    private fun fetchUrl(url: String, token: String): String? {
        return try {
            val builder = Request.Builder().url(url)
            if (token.isNotBlank()) {
                builder.addHeader("Authorization", "Bearer $token")
            }
            httpClient.newCall(builder.build()).execute().use { response ->
                if (response.isSuccessful) response.body.string() else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractPackageFromContent(content: String, path: String): String? {
        if (path.endsWith("AndroidManifest.xml", ignoreCase = true)) {
            // Check package attribute in manifest
            val manifestPattern = Pattern.compile("<manifest[\\s\\S]*?package\\s*=\\s*\"([^\"]+)\"")
            val manifestMatcher = manifestPattern.matcher(content)
            if (manifestMatcher.find()) {
                return manifestMatcher.group(1)
            }

            // Check action strings (e.g. <action android:name="zed.rainxch.githubstore.action.CANCEL_DOWNLOAD" />)
            val actionPattern = Pattern.compile("android:name=\"([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)\\.action\\.[a-zA-Z0-9_]+\"")
            val actionMatcher = actionPattern.matcher(content)
            while (actionMatcher.find()) {
                val actionPkg = actionMatcher.group(1) ?: continue
                if (!actionPkg.startsWith("android.")) {
                    return actionPkg
                }
            }
        } else if (path.endsWith(".gradle.kts", ignoreCase = true) || path.endsWith(".gradle", ignoreCase = true) || path.endsWith(".toml", ignoreCase = true)) {
            // Check projectApplicationId, applicationId, bundleID, or namespace
            val gradlePattern = Pattern.compile("(?m)^\\s*(?:projectApplicationId|applicationId|bundleID|namespace)\\s*[\"=:]\\s*[\"']([^\"']+)[\"']")
            val gradleMatcher = gradlePattern.matcher(content)
            if (gradleMatcher.find()) {
                val found = gradleMatcher.group(1)
                // Filter out non-package IDs like desktop app names
                if (found != null && !found.contains("-") && isValidPackageName(found)) {
                    return found
                }
            }
        }
        return null
    }

    private fun isValidPackageName(pkg: String): Boolean {
        val parts = pkg.split('.')
        if (parts.size < 2) return false
        val regex = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
        return parts.all { regex.matches(it) }
    }
}

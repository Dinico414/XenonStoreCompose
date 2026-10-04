package com.xenonware.store.presentation.sign_in

import android.app.Activity
import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import com.xenonware.store.data.SharedPreferenceManager
import com.xenonware.store.viewmodel.classes.GitHubUserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class GitHubAuthUiClient(
    private val context: Context
) {
    private val auth = Firebase.auth
    private val sharedPreferenceManager = SharedPreferenceManager(context)
    private val httpClient = OkHttpClient.Builder().build()
    private val jsonSerializer = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun signIn(activity: Activity): SignInResult {
        return try {
            val provider = OAuthProvider.newBuilder("github.com").apply {
                scopes = listOf("repo", "user")
            }

            var token = ""
            var displayName: String? = null
            var photoUrl: String? = null

            try {
                val result = auth.startActivityForSignInWithProvider(activity, provider.build()).await()
                val user = result.user
                val credential = result.credential as? OAuthCredential
                token = credential?.accessToken ?: ""
                displayName = user?.displayName ?: user?.email?.substringBefore("@")
                photoUrl = user?.photoUrl?.toString()
            } catch (collisionEx: FirebaseAuthUserCollisionException) {
                val credential = collisionEx.updatedCredential as? OAuthCredential
                token = credential?.accessToken ?: ""

                auth.currentUser?.let { currentUser ->
                    try {
                        if (credential != null) {
                            val linkResult = currentUser.linkWithCredential(credential).await()
                            displayName = linkResult.user?.displayName ?: displayName
                            photoUrl = linkResult.user?.photoUrl?.toString() ?: photoUrl
                        }
                    } catch (_: Exception) {}
                }
            }

            var githubLogin = displayName ?: "GitHub User"
            var githubAvatar = photoUrl ?: ""

            if (token.isNotBlank()) {
                try {
                    val request = Request.Builder()
                        .url("https://api.github.com/user")
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Accept", "vnd.github+json")
                        .build()

                    withContext(Dispatchers.IO) {
                        httpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body.string()
                                val ghUser = jsonSerializer.decodeFromString<GitHubUserResponse>(body)
                                githubLogin = ghUser.login
                                githubAvatar = ghUser.avatarUrl ?: githubAvatar
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            if (token.isNotBlank() || githubLogin != "GitHub User") {
                sharedPreferenceManager.isGitHubLoggedIn = true
                sharedPreferenceManager.githubToken = token
                sharedPreferenceManager.githubUsername = githubLogin
                sharedPreferenceManager.githubAvatarUrl = githubAvatar

                SignInResult(
                    data = UserData(
                        userId = githubLogin,
                        username = githubLogin,
                        profilePictureUrl = githubAvatar,
                        email = ""
                    ),
                    errorMessage = null
                )
            } else {
                SignInResult(
                    data = null,
                    errorMessage = "GitHub sign in failed: No OAuth token returned."
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SignInResult(
                data = null,
                errorMessage = e.message ?: "GitHub sign in error"
            )
        }
    }

    fun signOut() {
        try {
            sharedPreferenceManager.isGitHubLoggedIn = false
            sharedPreferenceManager.githubToken = ""
            sharedPreferenceManager.githubUsername = ""
            sharedPreferenceManager.githubAvatarUrl = ""
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSignedInUser(): UserData? {
        if (!sharedPreferenceManager.isGitHubLoggedIn) return null
        return UserData(
            userId = sharedPreferenceManager.githubUsername,
            username = sharedPreferenceManager.githubUsername,
            profilePictureUrl = sharedPreferenceManager.githubAvatarUrl,
            email = ""
        )
    }
}

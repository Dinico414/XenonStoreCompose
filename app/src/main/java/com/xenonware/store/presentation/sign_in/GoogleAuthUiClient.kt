package com.xenonware.store.presentation.sign_in

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.xenonware.store.R
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class GoogleAuthUiClient(
    private val context: Context
) {
    @Deprecated("No longer used with Credential Manager")
    constructor(context: Context, @Suppress("UNUSED_PARAMETER") oneTapClient: Any?) : this(context)

    private val auth = Firebase.auth
    private val sharedPreferenceManager = com.xenonware.store.data.SharedPreferenceManager(context)

    suspend fun signIn(activityContext: Context = context): SignInResult {
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } else {
                SignInResult(
                    data = null,
                    errorMessage = "Unexpected credential type"
                )
            }
        } catch (_: GetCredentialCancellationException) {
            SignInResult(
                data = null,
                errorMessage = null
            )
        } catch (e: GetCredentialException) {
            SignInResult(
                data = null,
                errorMessage = e.message
            )
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            SignInResult(
                data = null,
                errorMessage = e.message
            )
        }
    }

    private suspend fun firebaseAuthWithGoogle(googleIdToken: String?): SignInResult {
        val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)
        return try {
            val user = auth.signInWithCredential(googleCredentials).await().user
            if (user != null) {
                sharedPreferenceManager.isUserLoggedIn = true
                sharedPreferenceManager.googleUserId = user.uid
                sharedPreferenceManager.googleUsername = user.displayName ?: ""
                sharedPreferenceManager.googleEmail = user.email ?: ""
                sharedPreferenceManager.googlePhotoUrl = user.photoUrl?.toString() ?: ""
            }
            SignInResult(
                data = user?.run {
                    UserData(
                        userId = uid,
                        username = displayName,
                        profilePictureUrl = photoUrl?.toString(),
                        email = email.toString()
                    )
                },
                errorMessage = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            SignInResult(
                data = null,
                errorMessage = e.message
            )
        }
    }

    suspend fun signOut() {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            sharedPreferenceManager.clearGoogleUser()
            // If the Firebase auth user belongs to Google, sign out
            val currentUser = auth.currentUser
            val isGoogle = currentUser?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true
            if (isGoogle) {
                auth.signOut()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
        }
    }

    fun getSignedInUser(): UserData? = try {
        if (!sharedPreferenceManager.isUserLoggedIn) {
            null
        } else {
            val currentUser = auth.currentUser
            val isGoogleUser = currentUser?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true
            if (currentUser != null && isGoogleUser) {
                UserData(
                    userId = currentUser.uid,
                    username = currentUser.displayName,
                    profilePictureUrl = currentUser.photoUrl?.toString(),
                    email = currentUser.email ?: ""
                )
            } else if (sharedPreferenceManager.googleUserId.isNotEmpty()) {
                UserData(
                    userId = sharedPreferenceManager.googleUserId,
                    username = sharedPreferenceManager.googleUsername,
                    profilePictureUrl = sharedPreferenceManager.googlePhotoUrl.ifBlank { null },
                    email = sharedPreferenceManager.googleEmail
                )
            } else {
                null
            }
        }
    } catch (_: Exception) {
        null
    }
}

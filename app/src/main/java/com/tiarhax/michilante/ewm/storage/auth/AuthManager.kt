import android.app.Activity
import android.content.Context
import android.util.Log

import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.tiarhax.michilante.R
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.result.Credentials
import com.auth0.android.callback.Callback
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.authentication.storage.CredentialsManagerException
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.provider.WebAuthProvider

class Auth0Manager(private val context: Context) {
    private val auth0 = Auth0.getInstance(
        context.getString(R.string.com_auth0_client_id),
        context.getString(R.string.com_auth0_domain)
    )
    val authClient = AuthenticationAPIClient(auth0)
    val credentialsManager = CredentialsManager(
        authenticationClient = authClient,
        storage = SharedPreferencesStorage(context)
    )




    fun login(onSuccess: (Credentials) -> Unit, onError: (AuthenticationException) -> Unit) {
        WebAuthProvider.login(auth0)
            .withScheme("michilante")
            .withScope("openid profile email offline_access")
            .withAudience("https://api.michilante.com")
            .start(context as Activity, object : Callback<Credentials, AuthenticationException> {
                override fun onSuccess(result: Credentials) {
                    Log.i("Auth0Manager.login#onSuccess", "Login successful")
                    credentialsManager.saveCredentials(result)
                    onSuccess(result)
                }

                override fun onFailure(error: AuthenticationException) {
                    Log.e("Auth0Manager.login#onFailure", error.getDescription())
                    onError(error)
                }
            })
    }

    fun getAccessToken(callback: (String?, String?) -> Unit) {
        credentialsManager.getCredentials(object : Callback<Credentials, CredentialsManagerException> {
            override fun onSuccess(result: Credentials) {
                callback(result.accessToken, null)
            }

            override fun onFailure(error: CredentialsManagerException) {
                Log.e("AuthManager guaka guaka", error.message?:"")
                callback(null, error.message)
            }
        })
    }

    fun refreshTokens(callback: (Boolean, String?) -> Unit) {
        credentialsManager.getCredentials(object : Callback<Credentials, CredentialsManagerException> {
            override fun onSuccess(result: Credentials) {
                callback(true, null)
            }

            override fun onFailure(error: CredentialsManagerException) {
                callback(false, error.message)
            }
        })
    }

    // Check if user is authenticated
    fun isAuthenticated(): Boolean {
        val result = credentialsManager.hasValidCredentials();
        Log.i("Auth0Manager.isAuthenticated()", "$result")
        return result
    }

    fun logout(onComplete: () -> Unit) {
        WebAuthProvider.logout(auth0)
            .withScheme("app")
            .start(context, callback = object: Callback<Void?, AuthenticationException>  {
                override  fun onSuccess(payload: Void?) {
                    credentialsManager.clearCredentials()
                    onComplete()
                }

                override fun onFailure(error: AuthenticationException) {
                    onComplete() // Handle error as needed
                }
            })
    }
}
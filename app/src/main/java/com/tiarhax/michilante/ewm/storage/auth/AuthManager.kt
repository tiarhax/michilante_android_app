import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.auth0.android.Auth0
import com.tiarhax.michilante.R
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.result.Credentials
import com.auth0.android.callback.Callback
import com.auth0.android.Auth0Exception
import com.auth0.android.provider.WebAuthProvider
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth");
class Auth0Manager(private val context: Context) {
    private val auth0 = Auth0.getInstance(
        context.getString(R.string.com_auth0_client_id),
        context.getString(R.string.com_auth0_domain)
    )

    private suspend fun saveTokenSecurely(accessToken: String, refreshToken: String?, idToken: String) {
        context.dataStore.edit { pref ->
            pref[stringPreferencesKey("accessToken")] = accessToken
            pref[stringPreferencesKey("refreshToken")] = refreshToken?:""
            pref[stringPreferencesKey("idToken")] = idToken
        }
    }

    private suspend fun deleteToken() {
        context.dataStore.edit { pref ->
            pref[stringPreferencesKey("accessToken")] = ""
            pref[stringPreferencesKey("refreshToken")] = ""
            pref[stringPreferencesKey("idToken")] = ""
        }
    }


    fun login(onSuccess: (Credentials) -> Unit, onError: (AuthenticationException) -> Unit) {
        WebAuthProvider.login(auth0)
            .withScheme("michilante") // Use your app's scheme
            .start(context as Activity, object : Callback<Credentials, AuthenticationException> {
                override fun onSuccess(result: Credentials) {
                    onSuccess(result)
                    runBlocking {
                        saveTokenSecurely(result.accessToken, result.refreshToken, result.idToken)
                    }
                }

                override fun onFailure(error: AuthenticationException) {
                    onError(error)
                }
            })
    }

    fun logout(onComplete: () -> Unit) {
        WebAuthProvider.logout(auth0)
            .withScheme("app")
            .start(context, callback = object: Callback<Void?, AuthenticationException>  {
                override  fun onSuccess(payload: Void?) {
                    onComplete()
                }

                override fun onFailure(error: AuthenticationException) {
                    onComplete() // Handle error as needed
                }
            })
    }
}
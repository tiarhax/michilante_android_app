import android.util.Log
import androidx.lifecycle.ViewModel
import com.auth0.android.result.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthState(
    val isAuthenticated: Boolean = false,
    val user: UserProfile? = null,
    val accessToken: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(private val auth0Manager: Auth0Manager) : ViewModel() {
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    init {
        checkAuthStatus()
    }
    private fun checkAuthStatus() {
        _authState.value = _authState.value.copy(
            isAuthenticated = auth0Manager.isAuthenticated()
        )
    }

    fun login() {
        _authState.value = _authState.value.copy(isLoading = true, error = null)

        auth0Manager.login(
            onSuccess = { credentials ->
                Log.i("AuthViewModel.login#onSuccess", "login was succesfull")
                _authState.value = _authState.value.copy(
                    isAuthenticated = true,
                    accessToken = credentials.accessToken,
                    isLoading = false
                )
                fetchUserProfile(credentials.accessToken)
            },
            onError = { error ->
                Log.i("AuthViewModel.login#onError", "login found an error")
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = error.getDescription()
                )
            }
        )
    }

    fun logout() {
        auth0Manager.logout {
            _authState.value = AuthState() // Reset to initial state
        }
    }

    private fun fetchUserProfile(accessToken: String) {
        // Implement user profile fetching if needed
    }
}
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

    fun login() {
        _authState.value = _authState.value.copy(isLoading = true, error = null)

        auth0Manager.login(
            onSuccess = { credentials ->
                _authState.value = _authState.value.copy(
                    isAuthenticated = true,
                    accessToken = credentials.accessToken,
                    isLoading = false
                )
                // Optionally fetch user profile
                fetchUserProfile(credentials.accessToken)
            },
            onError = { error ->
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
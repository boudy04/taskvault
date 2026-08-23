package dev.boudy04.taskvault.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.data.source.network.AuthRequest
import dev.boudy04.taskvault.data.source.network.TaskApiService
import dev.boudy04.taskvault.settings.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class LoginUiState(
    val registerMode: Boolean = false,
    val username: String = "",
    val password: String = "",
    @StringRes val errorRes: Int? = null,
    val busy: Boolean = false,
)

/**
 * Drives the login/register form. Both modes hit their auth endpoint; on success the JWT +
 * username are persisted and session gating in [dev.boudy04.taskvault.TodoActivity] swaps to
 * the task graph.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: TaskApiService,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value, errorRes = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorRes = null) }
    }

    /** Toggles between login and register (same form); clears any inline error. */
    fun toggleMode() {
        _uiState.update { it.copy(registerMode = !it.registerMode, errorRes = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.busy || state.username.isBlank() || state.password.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, errorRes = null) }
            try {
                val body = AuthRequest(state.username.trim(), state.password)
                val response =
                    if (state.registerMode) api.register(body) else api.login(body)
                settings.setSession(response.token, state.username.trim())
            } catch (e: IOException) {
                _uiState.update { it.copy(errorRes = R.string.login_error_network) }
            } catch (e: HttpException) {
                // ponytail: unmapped HTTP codes fall back to the network message; add a
                // server-error string only if users actually report one
                @StringRes val res = when (e.code()) {
                    401 -> R.string.login_error_bad_credentials
                    409 -> R.string.login_error_username_taken
                    422 -> R.string.login_error_weak_password
                    else -> R.string.login_error_network
                }
                _uiState.update { it.copy(errorRes = res) }
            } finally {
                _uiState.update { it.copy(busy = false) }
            }
        }
    }
}

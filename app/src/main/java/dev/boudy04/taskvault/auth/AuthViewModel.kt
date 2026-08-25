package dev.boudy04.taskvault.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.data.source.network.AdminVerifyRequest
import dev.boudy04.taskvault.data.source.network.MemberLoginRequest
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
    val name: String = "",
    val password: String = "",
    /** Inline 422 rejection of the typed name. */
    val nameError: Boolean = false,
    /** Inline 401 rejection of a typed workspace key. */
    val keyError: Boolean = false,
    /** Transient connectivity failures surface as a snackbar. */
    @StringRes val snackbarRes: Int? = null,
    val busy: Boolean = false,
)

/**
 * Two-input login: a name alone logs in as (or auto-provisions) that member;
 * typing the workspace key in the password field verifies as administrator.
 * Success persists token+role+username, then resolves /api/members/me to store
 * the workspace member id used by the "assigned to you" section.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: TaskApiService,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, nameError = false) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, keyError = false) }
    }

    fun enter() {
        val state = _uiState.value
        if (state.busy) return
        if (state.password.isBlank()) {
            loginMember(state.name.trim())
        } else {
            verifyAdmin(state.password)
        }
    }

    private fun loginMember(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, snackbarRes = null) }
            try {
                val resp = api.membersLogin(MemberLoginRequest(name))
                // Store first so membersMe goes out carrying the fresh token.
                settings.setSession(resp.token, resp.role, resp.username)
                settings.setUserId(api.membersMe().id)
            } catch (e: HttpException) {
                if (e.code() == HTTP_UNPROCESSABLE) {
                    _uiState.update { it.copy(nameError = true) }
                } else {
                    _uiState.update { it.copy(snackbarRes = R.string.login_error_offline) }
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(snackbarRes = R.string.login_error_offline) }
            } finally {
                _uiState.update { it.copy(busy = false) }
            }
        }
    }

    private fun verifyAdmin(key: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, keyError = false) }
            try {
                val me = api.adminVerify(AdminVerifyRequest(key))
                settings.setSession(key, me.role, me.username)
                settings.setUserId(me.id)
            } catch (e: HttpException) {
                _uiState.update { it.copy(keyError = true) }
            } catch (e: IOException) {
                _uiState.update { it.copy(snackbarRes = R.string.login_error_offline) }
            } finally {
                _uiState.update { it.copy(busy = false) }
            }
        }
    }

    fun snackbarShown() {
        _uiState.update { it.copy(snackbarRes = null) }
    }

    private companion object {
        const val HTTP_UNPROCESSABLE = 422
    }
}

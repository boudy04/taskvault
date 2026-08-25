package dev.boudy04.taskvault.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.data.source.network.AdminVerifyRequest
import dev.boudy04.taskvault.data.source.network.MemberDto
import dev.boudy04.taskvault.data.source.network.MemberLoginRequest
import dev.boudy04.taskvault.data.source.network.TaskApiService
import dev.boudy04.taskvault.settings.OWNER_USERNAME
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
    val members: List<MemberDto> = emptyList(),
    val membersFailed: Boolean = false,
    /** Workspace-key entry expanded below the Administrator row. */
    val adminExpanded: Boolean = false,
    val workspaceKey: String = "",
    /** Inline workspace-key rejection (401). */
    val keyError: Boolean = false,
    /** Transient member-login failures surface as a snackbar. */
    @StringRes val snackbarRes: Int? = null,
    val busy: Boolean = false,
)

/**
 * Drives the identity picker. Members log in with one tap (username-only);
 * the administrator expands a masked workspace-key field. Success persists
 * token+role+username and session gating swaps to the task graph.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: TaskApiService,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        loadMembers()
    }

    fun loadMembers() {
        viewModelScope.launch {
            try {
                _uiState.update {
                    // The workspace owner logs in via the key below, never as a member.
                    it.copy(members = api.listMembers().filter { m -> m.username != OWNER_USERNAME }, membersFailed = false)
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(membersFailed = true) }
            }
        }
    }

    fun toggleAdmin(expanded: Boolean) {
        _uiState.update { it.copy(adminExpanded = expanded, keyError = false) }
    }

    fun updateWorkspaceKey(value: String) {
        _uiState.update { it.copy(workspaceKey = value, keyError = false) }
    }

    fun loginAs(member: MemberDto) {
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, snackbarRes = null) }
            try {
                val resp = api.membersLogin(MemberLoginRequest(member.username))
                settings.setSession(resp.token, resp.role, resp.username)
            } catch (e: HttpException) {
                @StringRes val res = if (e.code() == HTTP_NOT_FOUND) {
                    R.string.login_error_unknown_member
                } else {
                    R.string.login_error_offline
                }
                _uiState.update { it.copy(snackbarRes = res) }
            } catch (e: IOException) {
                _uiState.update { it.copy(snackbarRes = R.string.login_error_offline) }
            } finally {
                _uiState.update { it.copy(busy = false) }
            }
        }
    }

    fun verifyAdmin() {
        val key = _uiState.value.workspaceKey.trim()
        if (_uiState.value.busy || key.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, keyError = false) }
            try {
                val me = api.adminVerify(AdminVerifyRequest(key))
                settings.setSession(key, me.role, me.username)
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
        const val HTTP_NOT_FOUND = 404
    }
}

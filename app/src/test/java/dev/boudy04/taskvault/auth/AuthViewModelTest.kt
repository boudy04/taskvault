package dev.boudy04.taskvault.auth

import androidx.annotation.StringRes
import com.google.common.truth.Truth.assertThat
import dev.boudy04.taskvault.MainCoroutineRule
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.data.source.network.AdminVerifyRequest
import dev.boudy04.taskvault.data.source.network.AuthRequest
import dev.boudy04.taskvault.data.source.network.AuthResponse
import dev.boudy04.taskvault.data.source.network.MeResponse
import dev.boudy04.taskvault.data.source.network.MemberDto
import dev.boudy04.taskvault.data.source.network.MemberLoginRequest
import dev.boudy04.taskvault.data.source.network.MemberLoginResponse
import dev.boudy04.taskvault.data.source.network.MemberRequest
import dev.boudy04.taskvault.data.source.network.TaskApiService
import dev.boudy04.taskvault.data.source.network.TaskDto
import dev.boudy04.taskvault.settings.FakeSettingsRepository
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Scriptable fake: each login path can be told to fail with a given error. */
private class FakeApi(
    var memberError: Exception? = null,
    var verifyError: Exception? = null,
) : TaskApiService {

    override suspend fun listMembers(): List<MemberDto> =
        listOf(MemberDto(1, "boudy04"), MemberDto(2, "alice"), MemberDto(3, "bob"))
    
    override suspend fun membersLogin(body: MemberLoginRequest): MemberLoginResponse {
        memberError?.let { throw it }
        return MemberLoginResponse("member-token", "member", body.username)
    }

    override suspend fun adminVerify(body: AdminVerifyRequest): MeResponse {
        verifyError?.let { throw it }
        return MeResponse(1, "boudy04", "admin")
    }

    override suspend fun membersMe() = MeResponse(1, "x", "member")

    override suspend fun register(body: AuthRequest) = AuthResponse("")
    override suspend fun login(body: AuthRequest) = AuthResponse("")
    override suspend fun listTasks(status: String?) = emptyList<TaskDto>()
    override suspend fun getTask(id: Int) = TaskDto(id = id)
    override suspend fun createTask(task: TaskDto) = task
    override suspend fun updateTask(id: Int, task: TaskDto) = task
    override suspend fun deleteTask(id: Int) = Response.success(Unit)
    override suspend fun createMember(body: MemberRequest) = MemberDto(9, body.username)
    override suspend fun deleteMember(id: Int) = Response.success(Unit)
}

private fun httpException(code: Int): HttpException =
    HttpException(Response.error<Any>(code, "".toResponseBody()))

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var api: FakeApi
    private lateinit var settings: FakeSettingsRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        api = FakeApi()
        settings = FakeSettingsRepository()
        viewModel = AuthViewModel(api, settings)
    }

    @Test
    fun loadsWorkspaceMembersForPicker() = runTest {
        advanceUntilIdle()
        // The workspace owner is excluded; he signs in via the key.
        assertThat(viewModel.uiState.value.members.map { it.username })
            .containsExactly("alice", "bob")
        assertThat(viewModel.uiState.value.membersFailed).isFalse()
    }

    @Test
    fun loadMembersFailure_flagsRetryState() = runTest {
        // The picker degrades to the Administrator row + retry when offline.
        val failingApi = FakeApi().apply { }
        val broken = object : TaskApiService by failingApi {
            override suspend fun listMembers(): List<MemberDto> = throw IOException("offline")
        }
        viewModel = AuthViewModel(broken, settings)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.members).isEmpty()
        assertThat(viewModel.uiState.value.membersFailed).isTrue()
    }

    @Test
    fun memberLogin_success_storesTokenRoleUsername() = runTest {
        advanceUntilIdle()

        viewModel.loginAs(MemberDto(2, "alice"))
        advanceUntilIdle()

        assertThat(settings.sessionToken).isEqualTo("member-token")
        assertThat(settings.sessionRole).isEqualTo("member")
        assertThat(settings.sessionUsername).isEqualTo("alice")
    }

    @Test
    fun memberLogin_unknownMember_maps404ToSnackbar() = runTest {
        advanceUntilIdle()
        api.memberError = httpException(404)

        viewModel.loginAs(MemberDto(2, "alice"))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.snackbarRes)
            .isEqualTo(R.string.login_error_unknown_member)
        // No session was stored.
        assertThat(settings.sessionToken).isEmpty()
    }

    @Test
    fun memberLogin_offline_mapsIoExceptionToOfflineMessage() = runTest {
        advanceUntilIdle()
        api.memberError = IOException("airplane mode")

        viewModel.loginAs(MemberDto(2, "alice"))
        advanceUntilIdle()

        @StringRes val res = viewModel.uiState.value.snackbarRes
        assertThat(res).isEqualTo(R.string.login_error_offline)
    }

    @Test
    fun adminVerify_correctKey_storesAdminSession() = runTest {
        advanceUntilIdle()
        viewModel.toggleAdmin(true)
        viewModel.updateWorkspaceKey("dev-token")

        viewModel.verifyAdmin()
        advanceUntilIdle()

        assertThat(settings.sessionToken).isEqualTo("dev-token")
        assertThat(settings.sessionRole).isEqualTo("admin")
        assertThat(settings.sessionUsername).isEqualTo("boudy04")
        assertThat(viewModel.uiState.value.keyError).isFalse()
    }

    @Test
    fun adminVerify_wrongKey_setsInlineErrorWithoutSession() = runTest {
        advanceUntilIdle()
        api.verifyError = httpException(401)
        viewModel.toggleAdmin(true)
        viewModel.updateWorkspaceKey("nope")

        viewModel.verifyAdmin()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.keyError).isTrue()
        assertThat(settings.sessionToken).isEmpty()
    }

    @Test
    fun adminVerify_offline_surfacesOfflineSnackbar() = runTest {
        advanceUntilIdle()
        api.verifyError = IOException("offline")
        viewModel.toggleAdmin(true)
        viewModel.updateWorkspaceKey("dev-token")

        viewModel.verifyAdmin()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.snackbarRes)
            .isEqualTo(R.string.login_error_offline)
    }

    @Test
    fun snackbarShown_clearsMessage() = runTest {
        advanceUntilIdle()
        api.memberError = httpException(404)
        viewModel.loginAs(MemberDto(2, "alice"))
        advanceUntilIdle()

        viewModel.snackbarShown()

        assertThat(viewModel.uiState.value.snackbarRes).isNull()
    }
}

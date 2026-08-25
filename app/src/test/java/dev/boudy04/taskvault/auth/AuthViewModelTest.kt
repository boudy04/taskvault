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
import dev.boudy04.taskvault.data.source.network.NoteRequest
import dev.boudy04.taskvault.data.source.network.TaskApiService
import dev.boudy04.taskvault.data.source.network.TaskDto
import dev.boudy04.taskvault.data.source.network.TaskStatusUpdate
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
    var meError: Exception? = null,
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

    override suspend fun membersMe(): MeResponse {
        meError?.let { throw it }
        return MeResponse(7, "alice", "member")
    }

    override suspend fun addNote(id: Int, body: NoteRequest) = Response.success(Unit)
    override suspend fun updateTaskStatus(id: Int, body: TaskStatusUpdate) = TaskDto(id = id)

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
    fun enter_blankPassword_logsInAsMember_storesTokenRoleUsernameAndId() = runTest {
        viewModel.updateName("alice")

        viewModel.enter()
        advanceUntilIdle()

        assertThat(settings.sessionToken).isEqualTo("member-token")
        assertThat(settings.sessionRole).isEqualTo("member")
        assertThat(settings.sessionUsername).isEqualTo("alice")
        // /api/members/me resolved and stored the workspace id.
        assertThat(settings.sessionUserId).isEqualTo(7)
        assertThat(viewModel.uiState.value.nameError).isFalse()
        assertThat(viewModel.uiState.value.keyError).isFalse()
    }

    @Test
    fun enter_filledPassword_verifiesAdmin_storesKeyAndIdentity() = runTest {
        viewModel.updateName("ignored")
        viewModel.updatePassword("dev-token")

        viewModel.enter()
        advanceUntilIdle()

        assertThat(settings.sessionToken).isEqualTo("dev-token")
        assertThat(settings.sessionRole).isEqualTo("admin")
        assertThat(settings.sessionUsername).isEqualTo("boudy04")
        assertThat(settings.sessionUserId).isEqualTo(1)
        assertThat(viewModel.uiState.value.keyError).isFalse()
    }

    @Test
    fun enter_invalidName_422_setsInlineNameErrorWithoutSession() = runTest {
        api.memberError = httpException(422)
        viewModel.updateName("!!")

        viewModel.enter()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.nameError).isTrue()
        assertThat(settings.sessionToken).isEmpty()
    }

    @Test
    fun enter_wrongWorkspaceKey_401_setsInlineErrorWithoutSession() = runTest {
        api.verifyError = httpException(401)
        viewModel.updatePassword("nope")

        viewModel.enter()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.keyError).isTrue()
        assertThat(settings.sessionToken).isEmpty()
    }

    @Test
    fun enter_memberLoginOffline_mapsIoExceptionToOfflineMessage() = runTest {
        api.memberError = IOException("airplane mode")
        viewModel.updateName("alice")

        viewModel.enter()
        advanceUntilIdle()

        assertSnackbarIs(R.string.login_error_offline)
        assertThat(settings.sessionToken).isEmpty()
    }

    @Test
    fun enter_adminVerifyOffline_surfacesOfflineSnackbar() = runTest {
        api.verifyError = IOException("offline")
        viewModel.updatePassword("dev-token")

        viewModel.enter()
        advanceUntilIdle()

        assertSnackbarIs(R.string.login_error_offline)
    }

    @Test
    fun typingAgain_clearsInlineErrors() = runTest {
        api.memberError = httpException(422)
        viewModel.updateName("!!")
        viewModel.enter()
        advanceUntilIdle()

        viewModel.updateName("ok-name")
        viewModel.updatePassword("key")

        assertThat(viewModel.uiState.value.nameError).isFalse()
        assertThat(viewModel.uiState.value.keyError).isFalse()
    }

    @Test
    fun snackbarShown_clearsMessage() = runTest {
        api.memberError = IOException("offline")
        viewModel.updateName("alice")
        viewModel.enter()
        advanceUntilIdle()

        viewModel.snackbarShown()

        assertThat(viewModel.uiState.value.snackbarRes).isNull()
    }

    @Test
    fun busy_guard_blocksDoubleEnter() = runTest {
        viewModel.updateName("alice")
        viewModel.enter()
        // Second tap while the first is in flight must not crash or double-store.
        viewModel.enter()
        advanceUntilIdle()

        assertThat(settings.sessionUsername).isEqualTo("alice")
    }

    private fun assertSnackbarIs(@StringRes expected: Int) {
        assertThat(viewModel.uiState.value.snackbarRes).isEqualTo(expected)
    }
}

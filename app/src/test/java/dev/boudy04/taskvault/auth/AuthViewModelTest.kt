package dev.boudy04.taskvault.auth

import com.google.common.truth.Truth.assertThat
import dev.boudy04.taskvault.MainCoroutineRule
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.data.source.network.AuthRequest
import dev.boudy04.taskvault.data.source.network.AuthResponse
import dev.boudy04.taskvault.data.source.network.TaskApiService
import dev.boudy04.taskvault.data.source.network.TaskDto
import dev.boudy04.taskvault.settings.FakeSettingsRepository
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

private fun httpException(code: Int): HttpException =
    HttpException(Response.error<Any>(code, "".toResponseBody()))

/** Hand-rolled fake of the auth endpoints; task endpoints are unused here. */
private class FakeAuthApi(
    var registerError: Exception? = null,
    var loginError: Exception? = null,
) : TaskApiService {
    val bodies = mutableListOf<Pair<String, AuthRequest>>()
    var nextToken = "jwt-1"

    override suspend fun listTasks(status: String?): List<TaskDto> = error("not used")
    override suspend fun getTask(id: Int): TaskDto = error("not used")
    override suspend fun createTask(task: TaskDto): TaskDto = error("not used")
    override suspend fun updateTask(id: Int, task: TaskDto): TaskDto = error("not used")
    override suspend fun deleteTask(id: Int): Response<Unit> = error("not used")

    override suspend fun register(body: AuthRequest): AuthResponse {
        bodies += "register" to body
        registerError?.let { throw it }
        return AuthResponse(nextToken)
    }

    override suspend fun login(body: AuthRequest): AuthResponse {
        bodies += "login" to body
        loginError?.let { throw it }
        return AuthResponse(nextToken)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule val mainDispatcherRule = MainCoroutineRule()

    private lateinit var api: FakeAuthApi
    private lateinit var settings: FakeSettingsRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        api = FakeAuthApi()
        settings = FakeSettingsRepository()
        viewModel = AuthViewModel(api, settings)
    }

    @Test
    fun `login success persists token and username`() = runTest(UnconfinedTestDispatcher()) {
        viewModel.updateUsername("boudy04")
        viewModel.updatePassword("secret123")
        viewModel.submit()

        assertThat(settings.sessionToken).isEqualTo("jwt-1")
        assertThat(settings.accountName).isEqualTo("boudy04")
        assertThat(viewModel.uiState.value.errorRes).isNull()
        assertThat(api.bodies.single().first).isEqualTo("login")
    }

    @Test
    fun `register mode hits register endpoint and persists session`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.toggleMode()
            viewModel.updateUsername("newuser")
            viewModel.updatePassword("longenough")
            viewModel.submit()

            assertThat(api.bodies.single().first).isEqualTo("register")
            assertThat(settings.sessionToken).isEqualTo("jwt-1")
            assertThat(settings.accountName).isEqualTo("newuser")
        }

    @Test
    fun `401 maps to wrong credentials message`() = runTest(UnconfinedTestDispatcher()) {
        api.loginError = httpException(401)
        submitCredentials()

        assertThat(viewModel.uiState.value.errorRes)
            .isEqualTo(R.string.login_error_bad_credentials)
        assertThat(settings.sessionToken).isEmpty()
    }

    @Test
    fun `409 maps to username taken`() = runTest(UnconfinedTestDispatcher()) {
        viewModel.toggleMode()
        api.registerError = httpException(409)
        submitCredentials()

        assertThat(viewModel.uiState.value.errorRes)
            .isEqualTo(R.string.login_error_username_taken)
    }

    @Test
    fun `422 maps to weak password message`() = runTest(UnconfinedTestDispatcher()) {
        viewModel.toggleMode()
        api.registerError = httpException(422)
        submitCredentials()

        assertThat(viewModel.uiState.value.errorRes)
            .isEqualTo(R.string.login_error_weak_password)
    }

    @Test
    fun `IOException maps to can't reach server`() = runTest(UnconfinedTestDispatcher()) {
        api.loginError = IOException("airplane mode")
        submitCredentials()

        assertThat(viewModel.uiState.value.errorRes).isEqualTo(R.string.login_error_network)
    }

    @Test
    fun `blank fields do not hit the network`() = runTest(UnconfinedTestDispatcher()) {
        viewModel.updateUsername("boudy04")
        // no password
        viewModel.submit()

        assertThat(api.bodies).isEmpty()
        assertThat(settings.sessionToken).isEmpty()
    }

    @Test
    fun `toggleMode clears previous error`() = runTest(UnconfinedTestDispatcher()) {
        api.loginError = httpException(401)
        submitCredentials()
        assertThat(viewModel.uiState.value.errorRes).isNotNull()

        viewModel.toggleMode()

        assertThat(viewModel.uiState.value.errorRes).isNull()
        assertThat(viewModel.uiState.value.registerMode).isTrue()
    }

    private fun submitCredentials() {
        viewModel.updateUsername("boudy04")
        viewModel.updatePassword("secret123")
        viewModel.submit()
    }
}

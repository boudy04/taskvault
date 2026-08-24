package dev.boudy04.taskvault.settings

import com.google.common.truth.Truth.assertThat
import dev.boudy04.taskvault.MainCoroutineRule
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.data.source.network.AuthRequest
import dev.boudy04.taskvault.data.source.network.AuthResponse
import dev.boudy04.taskvault.data.source.network.MemberDto
import dev.boudy04.taskvault.data.source.network.MemberRequest
import dev.boudy04.taskvault.data.source.network.TaskApiService
import dev.boudy04.taskvault.data.source.network.TaskDto
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Hand-rolled fake of [TaskApiService]: in-memory members, records calls. */
private class FakeMemberApi(
    initial: List<MemberDto> = emptyList(),
) : TaskApiService {

    val members = initial.toMutableList()
    val createdNames = mutableListOf<String>()
    val deletedIds = mutableListOf<Int>()
    var offline = false

    private fun checkOffline() {
        if (offline) throw IOException("server unreachable")
    }

    override suspend fun listMembers(): List<MemberDto> {
        checkOffline()
        return members.sortedBy { it.username }
    }

    override suspend fun createMember(body: MemberRequest): MemberDto {
        checkOffline()
        if (members.any { it.username.equals(body.username, ignoreCase = true) }) {
            throw HttpException(Response.error<Any>(409, "".toResponseBody()))
        }
        if (!Regex("[a-z0-9_.-]{3,24}").matches(body.username)) {
            throw HttpException(Response.error<Any>(422, "".toResponseBody()))
        }
        createdNames += body.username
        val member = MemberDto((members.maxOfOrNull { it.id } ?: 0) + 1, body.username)
        members += member
        return member
    }

    override suspend fun deleteMember(id: Int): Response<Unit> {
        checkOffline()
        deletedIds += id
        members.removeAll { it.id == id }
        return Response.success(Unit)
    }

    // Tasks are not part of the Team card.
    override suspend fun listTasks(status: String?): List<TaskDto> = error("not used")
    override suspend fun getTask(id: Int): TaskDto = error("not used")
    override suspend fun createTask(task: TaskDto): TaskDto = error("not used")
    override suspend fun updateTask(id: Int, task: TaskDto): TaskDto = error("not used")
    override suspend fun deleteTask(id: Int): Response<Unit> = error("not used")
    override suspend fun register(body: AuthRequest): AuthResponse = error("not used")
    override suspend fun login(body: AuthRequest): AuthResponse = error("not used")
}

@ExperimentalCoroutinesApi
class TeamViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val seed = listOf(
        MemberDto(1, "boudy04"),
        MemberDto(2, "alice"),
        MemberDto(3, "bob"),
        MemberDto(4, "carol"),
    )

    @Test
    fun init_loadsMembers() = runTest {
        val vm = TeamViewModel(FakeMemberApi(seed))

        assertThat(vm.uiState.value.members.map { it.username })
            .containsExactly("alice", "bob", "boudy04", "carol").inOrder()
        assertThat(vm.uiState.value.errorRes).isNull()
    }

    @Test
    fun addMember_postsAndRefreshes_clearsInput() = runTest {
        val api = FakeMemberApi(seed)
        val vm = TeamViewModel(api)

        vm.updateNewName(" Dave_Dev ")
        vm.addMember()

        assertThat(api.createdNames).containsExactly("dave_dev")
        assertThat(vm.uiState.value.members.map { it.username }).contains("dave_dev")
        assertThat(vm.uiState.value.newName).isEmpty()
        assertThat(vm.uiState.value.errorRes).isNull()
    }

    @Test
    fun removeMember_deletesAndRefreshes() = runTest {
        val api = FakeMemberApi(seed)
        val vm = TeamViewModel(api)

        vm.removeMember(3)

        assertThat(api.deletedIds).containsExactly(3)
        assertThat(vm.uiState.value.members.map { it.username }).doesNotContain("bob")
    }

    @Test
    fun addDuplicate_mapsToDuplicateMessage() = runTest {
        val vm = TeamViewModel(FakeMemberApi(seed))

        vm.updateNewName("ALICE")
        vm.addMember()

        assertThat(vm.uiState.value.errorRes).isEqualTo(R.string.team_error_duplicate)
        // Failed add keeps what the user typed so they can correct it.
        assertThat(vm.uiState.value.newName).isEqualTo("ALICE")
    }

    @Test
    fun addInvalid_mapsToInvalidMessage() = runTest {
        val vm = TeamViewModel(FakeMemberApi(seed))

        vm.updateNewName("ab!")
        vm.addMember()

        assertThat(vm.uiState.value.errorRes).isEqualTo(R.string.team_error_invalid)
    }

    @Test
    fun offlineFailure_mapsToOfflineMessage() = runTest {
        val api = FakeMemberApi(seed)
        val vm = TeamViewModel(api)
        api.offline = true

        vm.refresh()

        assertThat(vm.uiState.value.errorRes).isEqualTo(R.string.team_error_offline)
    }
}

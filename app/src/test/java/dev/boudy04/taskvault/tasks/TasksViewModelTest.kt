/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.boudy04.taskvault.tasks

import androidx.lifecycle.SavedStateHandle
import dev.boudy04.taskvault.ADD_EDIT_RESULT_OK
import dev.boudy04.taskvault.DELETE_RESULT_OK
import dev.boudy04.taskvault.EDIT_RESULT_OK
import dev.boudy04.taskvault.MainCoroutineRule
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.data.FakeTaskRepository
import dev.boudy04.taskvault.data.Task
import dev.boudy04.taskvault.data.TaskPriority
import dev.boudy04.taskvault.data.TaskStatus
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
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

/** Minimal API stub so the ViewModel can resolve workspace members offline-free. */
private class FakeApi : TaskApiService {
    override suspend fun register(body: AuthRequest) = AuthResponse("")

    override suspend fun login(body: AuthRequest) = AuthResponse("")

    override suspend fun listTasks(status: String?) = emptyList<TaskDto>()

    override suspend fun getTask(id: Int) = TaskDto(id = id)

    override suspend fun createTask(task: TaskDto) = task

    override suspend fun updateTask(id: Int, task: TaskDto) = task

    override suspend fun deleteTask(id: Int) = Response.success(Unit)

    override suspend fun listMembers() = listOf(
        MemberDto(1, "alice"),
        MemberDto(2, "bob"),
    )

    override suspend fun createMember(body: MemberRequest) = MemberDto(9, body.username)

    override suspend fun deleteMember(id: Int) = Response.success(Unit)

    override suspend fun membersLogin(body: MemberLoginRequest) = MemberLoginResponse("t", "member", body.username)
    override suspend fun adminVerify(body: AdminVerifyRequest) = MeResponse(1, "x", "admin")
    override suspend fun membersMe() = MeResponse(1, "x", "member")
}

/**
 * Unit tests for the implementation of [TasksViewModel]
 */
@ExperimentalCoroutinesApi
class TasksViewModelTest {

    // Subject under test
    private lateinit var tasksViewModel: TasksViewModel

    // Use a fake repository to be injected into the viewmodel
    private lateinit var tasksRepository: FakeTaskRepository

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel() {
        // We initialise the tasks to 3, with one active and two completed
        tasksRepository = FakeTaskRepository()
        val task1 = Task(id = "1", title = "Title1", description = "Desc1")
        val task2 = Task(id = "2", title = "Title2", description = "Desc2", status = TaskStatus.DONE)
        val task3 = Task(id = "3", title = "Title3", description = "Desc3", status = TaskStatus.DONE)
        tasksRepository.addTasks(task1, task2, task3)

        tasksViewModel = TasksViewModel(
            tasksRepository,
            FakeApi(),
            dev.boudy04.taskvault.settings.FakeSettingsRepository(),
            dev.boudy04.taskvault.sync.ViewOnlyRejections(),
            SavedStateHandle(),
        )
    }

    @Test
    fun loadAllTasksFromRepository_loadingTogglesAndDataLoaded() = runTest {
        // Set Main dispatcher to not run coroutines eagerly, for just this one test
        Dispatchers.setMain(StandardTestDispatcher())

        // Given an initialized TasksViewModel with initialized tasks
        // When loading of Tasks is requested
        tasksViewModel.setFiltering(TasksFilterType.ALL_TASKS)

        // Trigger loading of tasks
        tasksViewModel.refresh()

        // Then progress indicator is shown
        assertThat(tasksViewModel.uiState.first().isLoading).isTrue()

        // Execute pending coroutines actions
        advanceUntilIdle()

        // Then progress indicator is hidden
        assertThat(tasksViewModel.uiState.first().isLoading).isFalse()

        // And data correctly loaded
        assertThat(tasksViewModel.uiState.first().items).hasSize(3)
    }

    @Test
    fun loadActiveTasksFromRepositoryAndLoadIntoView() = runTest {
        // Given an initialized TasksViewModel with initialized tasks
        // When loading of Tasks is requested
        tasksViewModel.setFiltering(TasksFilterType.ACTIVE_TASKS)

        // Load tasks
        tasksViewModel.refresh()

        // Then progress indicator is hidden
        assertThat(tasksViewModel.uiState.first().isLoading).isFalse()

        // And data correctly loaded
        assertThat(tasksViewModel.uiState.first().items).hasSize(1)
    }

    @Test
    fun loadCompletedTasksFromRepositoryAndLoadIntoView() = runTest {
        // Given an initialized TasksViewModel with initialized tasks
        // When loading of Tasks is requested
        tasksViewModel.setFiltering(TasksFilterType.COMPLETED_TASKS)

        // Load tasks
        tasksViewModel.refresh()

        // Then progress indicator is hidden
        assertThat(tasksViewModel.uiState.first().isLoading).isFalse()

        // And data correctly loaded
        assertThat(tasksViewModel.uiState.first().items).hasSize(2)
    }

    @Test
    fun loadTasks_error() = runTest {
        // Make the repository throw errors
        tasksRepository.setShouldThrowError(true)

        // Load tasks
        tasksViewModel.refresh()

        // Then progress indicator is hidden
        assertThat(tasksViewModel.uiState.first().isLoading).isFalse()

        // And the list of items is empty
        assertThat(tasksViewModel.uiState.first().items).isEmpty()
        assertThat(tasksViewModel.uiState.first().userMessage)
            .isEqualTo(R.string.loading_tasks_error)
    }

    @Test
    fun clearCompletedTasks_clearsTasks() = runTest {
        // When completed tasks are cleared
        tasksViewModel.clearCompletedTasks()

        // Fetch tasks
        tasksViewModel.refresh()

        // Fetch tasks
        val allTasks = tasksViewModel.uiState.first().items
        val completedTasks = allTasks?.filter { it.isCompleted }

        // Verify there are no completed tasks left
        assertThat(completedTasks).isEmpty()

        // Verify active task is not cleared
        assertThat(allTasks).hasSize(1)

        // Verify snackbar is updated
        assertThat(tasksViewModel.uiState.first().userMessage)
            .isEqualTo(R.string.completed_tasks_cleared)
    }

    @Test
    fun showEditResultMessages_editOk_snackbarUpdated() = runTest {
        // When the viewmodel receives a result from another destination
        tasksViewModel.showEditResultMessage(EDIT_RESULT_OK)

        // The snackbar is updated
        assertThat(tasksViewModel.uiState.first().userMessage)
            .isEqualTo(R.string.successfully_saved_task_message)
    }

    @Test
    fun showEditResultMessages_addOk_snackbarUpdated() = runTest {
        // When the viewmodel receives a result from another destination
        tasksViewModel.showEditResultMessage(ADD_EDIT_RESULT_OK)

        // The snackbar is updated
        assertThat(tasksViewModel.uiState.first().userMessage)
            .isEqualTo(R.string.successfully_added_task_message)
    }

    @Test
    fun showEditResultMessages_deleteOk_snackbarUpdated() = runTest {
        // When the viewmodel receives a result from another destination
        tasksViewModel.showEditResultMessage(DELETE_RESULT_OK)

        // The snackbar is updated
        assertThat(tasksViewModel.uiState.first().userMessage)
            .isEqualTo(R.string.successfully_deleted_task_message)
    }

    @Test
    fun completeTask_dataAndSnackbarUpdated() = runTest {
        // With a repository that has an active task
        val task = Task(id = "id", title = "Title", description = "Description")
        tasksRepository.addTasks(task)

        // Complete task
        tasksViewModel.completeTask(task, true)

        // Verify the task is completed
        assertThat(tasksRepository.savedTasks.value[task.id]?.isCompleted).isTrue()

        // The snackbar is updated
        assertThat(tasksViewModel.uiState.first().userMessage)
            .isEqualTo(R.string.task_marked_complete)
    }

    @Test
    fun activateTask_dataAndSnackbarUpdated() = runTest {
        // With a repository that has a completed task
        val task = Task(id = "id", title = "Title", description = "Description", status = TaskStatus.DONE)
        tasksRepository.addTasks(task)

        // Activate task
        tasksViewModel.completeTask(task, false)

        // Verify the task is active
        assertThat(tasksRepository.savedTasks.value[task.id]?.isActive).isTrue()

        // The snackbar is updated
        assertThat(tasksViewModel.uiState.first().userMessage)
            .isEqualTo(R.string.task_marked_active)
    }

    @Test
    fun pendingIds_exposedThroughUiState() = runTest {
        // Given a task with a queued pending op
        tasksRepository.setPendingSyncIds(setOf("1"))

        // Then the unsynced id set is exposed through the ui state
        assertThat(tasksViewModel.uiState.first().pendingSyncIds).containsExactly("1")
    }

    @Test
    fun selectGroup_narrowsList_andAllClearsIt() = runTest {
        tasksRepository.addTasks(
            Task(id = "10", title = "Work task", description = "", tags = listOf("work")),
            Task(id = "11", title = "Home task", description = "", tags = listOf("home")),
        )

        tasksViewModel.selectGroup("work")

        val filtered = tasksViewModel.uiState.first().items
        assertThat(filtered.map { it.id }).containsExactly("10")
        assertThat(tasksViewModel.uiState.first().selectedGroup).isEqualTo("work")
        // Presets are always offered even before any task uses them.
        assertThat(tasksViewModel.uiState.first().availableGroups)
            .containsAtLeast("home", "work", "errands")

        tasksViewModel.selectGroup(null)
        assertThat(tasksViewModel.uiState.first().selectedGroup).isNull()
        assertThat(tasksViewModel.uiState.first().items).hasSize(5)
    }

    @Test
    fun personalTeamSplit_zeroAssigneesIsPersonal() = runTest {
        tasksRepository.addTasks(
            Task(id = "40", title = "Solo", description = ""),
            Task(id = "41", title = "Shared", description = "", assigneeIds = listOf(1)),
        )

        val state = tasksViewModel.uiState.first()
        // Team shows everything; Personal only unassigned tasks.
        assertThat(state.items.map { it.id }).containsExactly("1", "2", "3", "40", "41")
        assertThat(state.personalItems.map { it.id }).containsExactly("1", "2", "3", "40")
    }

    @Test
    fun groupPersonStatusFilter_combineWithAnd() = runTest {
        tasksRepository.addTasks(
            Task(id = "30", title = "A work thing", description = "", tags = listOf("work"), status = TaskStatus.TODO),
            Task(id = "31", title = "Another work thing", description = "", tags = listOf("work"), status = TaskStatus.DONE),
            Task(id = "32", title = "A home thing", description = "", tags = listOf("home")),
            Task(id = "33", title = "Work assigned", description = "", tags = listOf("work"), assigneeIds = listOf(2)),
        )

        tasksViewModel.selectGroup("work")
        assertThat(tasksViewModel.uiState.first().items.map { it.id })
            .containsExactly("30", "31", "33")

        tasksViewModel.setSearchQuery("thing")
        assertThat(tasksViewModel.uiState.first().items.map { it.id })
            .containsExactly("30", "31")

        tasksViewModel.setFiltering(TasksFilterType.ACTIVE_TASKS)
        assertThat(tasksViewModel.uiState.first().items.map { it.id })
            .containsExactly("30")

        // Person filter ANDs too: alice (id 1) owns none of the remaining.
        tasksViewModel.selectPerson(1)
        assertThat(tasksViewModel.uiState.first().items).isEmpty()

        tasksViewModel.setFiltering(TasksFilterType.ALL_TASKS)
        tasksViewModel.setSearchQuery("")
        tasksViewModel.selectPerson(null)
        assertThat(tasksViewModel.uiState.first().items.map { it.id })
            .containsExactly("30", "31", "33")
    }

    @Test
    fun personFilter_unassignedMatchesZeroAssignees() = runTest {
        tasksRepository.addTasks(
            Task(id = "50", title = "Free", description = ""),
            Task(id = "51", title = "Bob's", description = "", assigneeIds = listOf(2)),
        )

        tasksViewModel.selectPerson(TasksViewModel.PERSON_UNASSIGNED)
        assertThat(tasksViewModel.uiState.first().items.map { it.id })
            .containsExactly("1", "2", "3", "50")

        tasksViewModel.selectPerson(2)
        assertThat(tasksViewModel.uiState.first().items.map { it.id }).containsExactly("51")
    }

    @Test
    fun sort_nearestDue_nullsLast_ascending() = runTest {
        tasksRepository.addTasks(
            Task(id = "60", title = "Late", description = "", dueAt = "2026-08-25T00:00:00Z"),
            Task(id = "61", title = "Soon", description = "", dueAt = "2026-08-24T00:00:00Z"),
            Task(id = "62", title = "No date", description = ""),
        )

        tasksViewModel.setSort(TasksSort.NEAREST_DUE)

        assertThat(tasksViewModel.uiState.first().items.map { it.id })
            .containsExactly("61", "60", "1", "2", "3", "62").inOrder()
    }

    @Test
    fun sort_newestAndOldest_byCreatedAt() = runTest {
        tasksRepository.addTasks(
            Task(id = "70", title = "Old", description = "", createdAt = "2026-01-01T00:00:00Z"),
            Task(id = "71", title = "New", description = "", createdAt = "2026-06-01T00:00:00Z"),
        )

        tasksViewModel.setSort(TasksSort.NEWEST)
        assertThat(tasksViewModel.uiState.first().items.map { it.id })
            .containsExactly("71", "70", "1", "2", "3").inOrder()

        tasksViewModel.setSort(TasksSort.OLDEST)
        assertThat(tasksViewModel.uiState.first().items.map { it.id })
            .containsExactly("70", "71", "1", "2", "3").inOrder()
    }

    @Test
    fun sort_priority_highFirst() = runTest {
        tasksRepository.addTasks(
            Task(id = "80", title = "Low", description = "", priority = TaskPriority.LOW),
            Task(id = "81", title = "High", description = "", priority = TaskPriority.HIGH),
            Task(id = "82", title = "Med", description = "", priority = TaskPriority.MEDIUM),
        )

        tasksViewModel.setSort(TasksSort.PRIORITY)

        assertThat(tasksViewModel.uiState.first().items.map { it.id })
            .containsExactly("81", "1", "2", "3", "82", "80").inOrder()
    }

    @Test
    fun setSearchQuery_matchesDescription() = runTest {
        tasksViewModel.setSearchQuery("desc2")

        val items = tasksViewModel.uiState.first().items
        assertThat(items.map { it.id }).containsExactly("2")
    }

    @Test
    fun memberSession_exposesViewOnlyFlags() = runTest {
        val settings = dev.boudy04.taskvault.settings.FakeSettingsRepository(
            initialToken = "tok",
            initialRole = dev.boudy04.taskvault.settings.Session.MEMBER_ROLE,
            initialUsername = "alice",
        )
        tasksViewModel = TasksViewModel(
            tasksRepository, FakeApi(), settings, dev.boudy04.taskvault.sync.ViewOnlyRejections(), SavedStateHandle(),
        )

        advanceUntilIdle()
        val state = tasksViewModel.uiState.first()
        // FAB hidden + checkboxes disabled both derive from this flag.
        assertThat(state.isMember).isTrue()
        assertThat(state.sessionUsername).isEqualTo("alice")
    }

    @Test
    fun adminSession_notMember() = runTest {
        val settings = dev.boudy04.taskvault.settings.FakeSettingsRepository(
            initialToken = "dev-token",
            initialRole = "admin",
            initialUsername = "boudy04",
        )
        tasksViewModel = TasksViewModel(
            tasksRepository, FakeApi(), settings, dev.boudy04.taskvault.sync.ViewOnlyRejections(), SavedStateHandle(),
        )

        advanceUntilIdle()
        assertThat(tasksViewModel.uiState.first().isMember).isFalse()
    }

    @Test
    fun viewOnlyRejection_surfacesSnackbar() = runTest {
        val rejections = dev.boudy04.taskvault.sync.ViewOnlyRejections()
        tasksViewModel = TasksViewModel(
            tasksRepository, FakeApi(), dev.boudy04.taskvault.settings.FakeSettingsRepository(), rejections, SavedStateHandle(),
        )

        rejections.signal()
        advanceUntilIdle()

        assertThat(tasksViewModel.uiState.first().userMessage)
            .isEqualTo(R.string.view_only_access)
    }

    @Test
    fun setSearchQuery_matchesGroup() = runTest {
        tasksRepository.addTasks(Task(id = "20", title = "Plain", description = "", tags = listOf("someday")))

        tasksViewModel.setSearchQuery("SOMEDAY")

        assertThat(tasksViewModel.uiState.first().items.map { it.id }).containsExactly("20")
    }
}

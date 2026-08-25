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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.boudy04.taskvault.ADD_EDIT_RESULT_OK
import dev.boudy04.taskvault.DELETE_RESULT_OK
import dev.boudy04.taskvault.EDIT_RESULT_OK
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.data.GROUP_PRESETS
import dev.boudy04.taskvault.data.NoteResult
import dev.boudy04.taskvault.data.Task
import dev.boudy04.taskvault.data.TaskRepository
import dev.boudy04.taskvault.data.source.network.MemberDto
import dev.boudy04.taskvault.data.source.network.TaskApiService
import dev.boudy04.taskvault.settings.SettingsRepository
import dev.boudy04.taskvault.settings.Session
import dev.boudy04.taskvault.sync.ViewOnlyRejections
import dev.boudy04.taskvault.tasks.TasksFilterType.ACTIVE_TASKS
import dev.boudy04.taskvault.tasks.TasksFilterType.ALL_TASKS
import dev.boudy04.taskvault.tasks.TasksFilterType.COMPLETED_TASKS
import dev.boudy04.taskvault.util.WhileUiSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UiState for the task list screen (UX v3). [items] backs the Team tab
 * (non-personal tasks matching the filters); [personalItems] is the
 * LOCAL-ONLY Personal subset. Team subsections derive from [sessionUserId].
 */
data class TasksUiState(
    val items: List<Task> = emptyList(),
    val personalItems: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val filteringUiInfo: FilteringUiInfo = FilteringUiInfo(),
    val userMessage: Int? = null,
    val pendingSyncIds: Set<String> = emptySet(),
    val availableGroups: List<String> = emptyList(),
    val members: List<MemberDto> = emptyList(),
    val searchQuery: String = "",
    /** null = All groups; otherwise a canonical group/tag name. */
    val selectedGroup: String? = null,
    /** null = Anyone; [PERSON_UNASSIGNED] = unassigned; else member id. */
    val personFilter: Int? = null,
    /** The Status menu mirrors the top-bar/drawer filter (they are one setting). */
    val statusFilter: TasksFilterType = ALL_TASKS,
    val sort: TasksSort = TasksSort.NEAREST_DUE,
    /** Consolidated Filters sheet visibility. */
    val filtersOpen: Boolean = false,
    /** How many of group/person/status/sort/search are actively narrowing. */
    val activeFilterCount: Int = 0,
    /** Member sessions toggle status only on their own assigned tasks. */
    val isMember: Boolean = false,
    val sessionUsername: String = "",
    val sessionUserId: Int = 0,
)

/** Everything the Filters sheet can set, held together so combine stays within 5 flows. */
private data class FilterState(
    val query: String = "",
    val group: String? = null,
    val person: Int? = null,
    val sort: TasksSort = TasksSort.NEAREST_DUE,
)

/** Team + personal lists after the shared filter pipeline; error carries a string res. */
private data class TaskLists(val team: List<Task>, val personal: List<Task>, val errorRes: Int? = null)

/**
 * ViewModel for the task list screen.
 */
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val api: TaskApiService,
    private val settingsRepository: SettingsRepository,
    private val viewOnlyRejections: ViewOnlyRejections,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _savedFilterType =
        savedStateHandle.getStateFlow(TASKS_FILTER_SAVED_STATE_KEY, ALL_TASKS)

    private val _filterUiInfo = _savedFilterType.map { getFilterUiInfo(it) }.distinctUntilChanged()
    private val _userMessage: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val _isLoading = MutableStateFlow(false)
    private val _filtersOpen = MutableStateFlow(false)

    private val _filterState = MutableStateFlow(FilterState())

    /** Current identity; drives the member role-aware UI flags. */
    private val _session = MutableStateFlow(Session())

    /** Workspace members for the Person filter; degrades to empty when offline. */
    private val _members = MutableStateFlow<List<MemberDto>>(emptyList())

    init {
        viewModelScope.launch { loadMembers() }
        viewModelScope.launch {
            settingsRepository.session.collect { _session.value = it }
        }
        viewModelScope.launch {
            // Server rejected a queued write with 403: disclose the role limit.
            viewOnlyRejections.events.collect { showSnackbarMessage(R.string.view_only_access) }
        }
    }

    /** Workspace members for the Person filter/assignee badges; empty when offline. */
    private suspend fun loadMembers() {
        try {
            _members.value = api.listMembers()
        } catch (_: Exception) {
            // ponytail: offline member filter just shows no people; retried on next refresh
        }
    }

    fun setSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(query = query)
    }

    fun selectGroup(group: String?) {
        _filterState.value = _filterState.value.copy(group = group)
    }

    fun selectPerson(personId: Int?) {
        _filterState.value = _filterState.value.copy(person = personId)
    }

    fun setSort(sort: TasksSort) {
        _filterState.value = _filterState.value.copy(sort = sort)
    }

    fun openFilters() {
        _filtersOpen.value = true
    }

    fun closeFilters() {
        _filtersOpen.value = false
    }

    fun resetFilters() {
        _filterState.value = FilterState()
        setFiltering(ALL_TASKS)
    }

    private data class ListExtras(
        val pendingSyncIds: Set<String> = emptySet(),
        val availableTags: List<String> = emptyList(),
        val query: String = "",
        val group: String? = null,
        val person: Int? = null,
        val sort: TasksSort = TasksSort.NEAREST_DUE,
        val members: List<MemberDto> = emptyList(),
        val statusFilter: TasksFilterType = ALL_TASKS,
        val isMember: Boolean = false,
        val sessionUsername: String = "",
        val sessionUserId: Int = 0,
        val filtersOpen: Boolean = false,
        val activeFilterCount: Int = 0,
    )

    private val _extras: Flow<ListExtras> = combine(
        _session,
        _members,
        _filtersOpen,
        _filterState,
        combine(
            taskRepository.getPendingSyncIdsStream(),
            // Errors surface through the main list pipeline; the group menu degrades silently.
            taskRepository.getTasksStream()
                .map { tasks -> tasks.flatMap { it.tags }.distinct().sorted() }
                .catch { emit(emptyList()) },
            _savedFilterType,
        ) { pending, tags, statusType ->
            Triple(pending, tags, statusType)
        },
    ) { session, members, filtersOpen, filterState, (pending, tags, statusType) ->
        val (query, group, person, sort) = filterState
        ListExtras(
            pendingSyncIds = pending,
            availableTags = tags,
            query = query,
            group = group,
            person = person,
            sort = sort,
            members = members,
            statusFilter = statusType,
            isMember = session.isMember,
            sessionUsername = session.username,
            sessionUserId = session.userId,
            filtersOpen = filtersOpen,
            activeFilterCount = listOfNotNull(
                if (query.isNotBlank()) Unit else null,
                if (group != null) Unit else null,
                if (person != null) Unit else null,
                if (sort != TasksSort.NEAREST_DUE) Unit else null,
                if (statusType != ALL_TASKS) Unit else null,
            ).size,
        )
    }

    /** One pass over the repo stream splits personal vs team, then applies all filters. */
    private val _taskListsAsync =
        combine(
            taskRepository.getTasksStream(),
            _savedFilterType,
            _filterState,
        ) { allTasks, type, filterState ->
            var team = filterTasks(allTasks.filterNot { it.isPersonal }, type)
            var personal = filterTasks(allTasks.filter { it.isPersonal }, type)
            if (filterState.group != null) {
                team = team.filter { filterState.group in it.tags }
                personal = personal.filter { filterState.group in it.tags }
            }
            when (val person = filterState.person) {
                PERSON_UNASSIGNED -> team = team.filter { it.assigneeIds.isEmpty() }
                null -> {}
                else -> team = team.filter { person in it.assigneeIds }
            }
            TaskLists(
                team = sortTasks(team.filter { matchesQuery(it, filterState.query) }, filterState.sort),
                personal = sortTasks(personal.filter { matchesQuery(it, filterState.query) }, filterState.sort),
            )
        }
            .map<TaskLists, TaskLists> { it }
            .catch<TaskLists> { emit(TaskLists(emptyList(), emptyList(), R.string.loading_tasks_error)) }

    val uiState: StateFlow<TasksUiState> = combine(
        _filterUiInfo, _isLoading, _userMessage, _taskListsAsync, _extras
    ) { filterUiInfo, isLoading, userMessage, lists, extras ->
        TasksUiState(
            items = lists.team,
            personalItems = lists.personal,
            pendingSyncIds = extras.pendingSyncIds,
            filteringUiInfo = filterUiInfo,
            isLoading = isLoading,
            userMessage = lists.errorRes ?: userMessage,
            availableGroups = (GROUP_PRESETS + extras.availableTags).distinct().sorted(),
            members = extras.members,
            searchQuery = extras.query,
            selectedGroup = extras.group,
            personFilter = extras.person,
            statusFilter = extras.statusFilter,
            sort = extras.sort,
            filtersOpen = extras.filtersOpen,
            activeFilterCount = extras.activeFilterCount,
            isMember = extras.isMember,
            sessionUsername = extras.sessionUsername,
            sessionUserId = extras.sessionUserId,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = WhileUiSubscribed,
            initialValue = TasksUiState(isLoading = true)
        )

    fun setFiltering(requestType: TasksFilterType) {
        savedStateHandle[TASKS_FILTER_SAVED_STATE_KEY] = requestType
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            taskRepository.clearCompletedTasks()
            showSnackbarMessage(R.string.completed_tasks_cleared)
            refresh()
        }
    }

    fun completeTask(task: Task, completed: Boolean) = viewModelScope.launch {
        if (completed) {
            taskRepository.completeTask(task.id)
            showSnackbarMessage(R.string.task_marked_complete)
        } else {
            taskRepository.activateTask(task.id)
            showSnackbarMessage(R.string.task_marked_active)
        }
    }

    fun addNote(task: Task, body: String) = viewModelScope.launch {
        when (taskRepository.addNote(task.id, body)) {
            NoteResult.ADDED -> Unit // pull already picked the new note up
            NoteResult.FORBIDDEN -> showSnackbarMessage(R.string.note_error_forbidden)
            NoteResult.FAILED -> showSnackbarMessage(R.string.login_error_offline)
        }
    }

    fun showEditResultMessage(result: Int) {
        when (result) {
            EDIT_RESULT_OK -> showSnackbarMessage(R.string.successfully_saved_task_message)
            ADD_EDIT_RESULT_OK -> showSnackbarMessage(R.string.successfully_added_task_message)
            DELETE_RESULT_OK -> showSnackbarMessage(R.string.successfully_deleted_task_message)
        }
    }

    fun snackbarMessageShown() {
        _userMessage.value = null
    }

    private fun showSnackbarMessage(message: Int) {
        _userMessage.value = message
    }

    fun refresh() {
        _isLoading.value = true
        viewModelScope.launch {
            taskRepository.refresh()
            loadMembers()
            _isLoading.value = false
        }
    }

    private fun filterTasks(tasks: List<Task>, filteringType: TasksFilterType): List<Task> {
        val tasksToShow = ArrayList<Task>()
        // We filter the tasks based on the requestType
        for (task in tasks) {
            when (filteringType) {
                ALL_TASKS -> tasksToShow.add(task)
                ACTIVE_TASKS -> if (task.isActive) {
                    tasksToShow.add(task)
                }
                COMPLETED_TASKS -> if (task.isCompleted) {
                    tasksToShow.add(task)
                }
            }
        }
        return tasksToShow
    }

    /** Case-insensitive contains over title/description/groups; blank matches all. */
    private fun matchesQuery(task: Task, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return task.title.contains(q, ignoreCase = true) ||
            task.description.contains(q, ignoreCase = true) ||
            task.tags.any { it.contains(q, ignoreCase = true) }
    }

    // ponytail: parse per comparison call; lists are small enough that caching is noise
    private fun dueInstant(task: Task): Instant? =
        task.dueAt?.let { runCatching { Instant.parse(it) }.getOrNull() }

    /** Null due dates / creation stamps always sink to the bottom, both directions. */
    private fun sortTasks(tasks: List<Task>, sort: TasksSort): List<Task> = when (sort) {
        TasksSort.NEAREST_DUE -> tasks.sortedWith(
            compareBy<Task> { dueInstant(it) == null }.thenBy { dueInstant(it) }
        )
        TasksSort.NEWEST -> tasks.sortedWith(
            compareBy<Task> { it.createdAt == null }.thenByDescending { it.createdAt }
        )
        TasksSort.OLDEST -> tasks.sortedWith(
            compareBy<Task> { it.createdAt == null }.thenBy { it.createdAt }
        )
        TasksSort.PRIORITY -> tasks.sortedByDescending { it.priority.ordinal }
    }

    private fun getFilterUiInfo(requestType: TasksFilterType): FilteringUiInfo =
        when (requestType) {
            ALL_TASKS -> {
                FilteringUiInfo(
                    R.string.label_all, R.string.no_tasks_all,
                    R.drawable.logo_no_fill
                )
            }
            ACTIVE_TASKS -> {
                FilteringUiInfo(
                    R.string.label_active, R.string.no_tasks_active,
                    R.drawable.ic_check_circle_96dp
                )
            }
            COMPLETED_TASKS -> {
                FilteringUiInfo(
                    R.string.label_completed, R.string.no_tasks_completed,
                    R.drawable.ic_verified_user_96dp
                )
            }
        }

    companion object {
        /** Person-filter sentinel meaning "no assignees". */
        const val PERSON_UNASSIGNED = -1
    }
}

// Used to save the current filtering in SavedStateHandle.
const val TASKS_FILTER_SAVED_STATE_KEY = "TASKS_FILTER_SAVED_STATE_KEY"

data class FilteringUiInfo(
    val currentFilteringLabel: Int = R.string.label_all,
    val noTasksLabel: Int = R.string.no_tasks_all,
    val noTaskIconRes: Int = R.drawable.logo_no_fill,
)

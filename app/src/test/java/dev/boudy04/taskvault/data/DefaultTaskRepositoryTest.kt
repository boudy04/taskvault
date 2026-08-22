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

package dev.boudy04.taskvault.data

import dev.boudy04.taskvault.data.source.local.FakeTaskDao
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the implementation of the in-memory repository with cache.
 * Remote-sync semantics are covered from Task 8 once DefaultTaskRepository talks to TaskApiService;
 * the fake-network based tests were removed along with that data source.
 */
@ExperimentalCoroutinesApi
class DefaultTaskRepositoryTest {

    private val task1 = Task(id = "1", title = "Title1", description = "Description1")
    private val task2 = Task(id = "2", title = "Title2", description = "Description2")
    private val task3 = Task(id = "3", title = "Title3", description = "Description3")

    private val newTaskTitle = "Title new"
    private val newTaskDescription = "Description new"
    private val newTask = Task(id = "new", title = newTaskTitle, description = newTaskDescription)

    private val localTasks = listOf(task3.toLocal())

    // Test dependencies
    private lateinit var localDataSource: FakeTaskDao

    private var testDispatcher = UnconfinedTestDispatcher()
    private var testScope = TestScope(testDispatcher)

    // Class under test
    private lateinit var taskRepository: DefaultTaskRepository

    @ExperimentalCoroutinesApi
    @Before
    fun createRepository() {
        localDataSource = FakeTaskDao(localTasks)
        // Get a reference to the class under test
        taskRepository = DefaultTaskRepository(
            localDataSource = localDataSource,
            dispatcher = testDispatcher,
            scope = testScope
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun getTasks_emptyRepositoryAndUninitializedCache() = testScope.runTest {
        localDataSource.deleteAll()

        assertThat(taskRepository.getTasks().size).isEqualTo(0)
    }

    @Test
    fun saveTask_savesToLocal() = testScope.runTest {
        // When a task is saved to the tasks repository
        val newTaskId = taskRepository.createTask(newTask.title, newTask.description)

        // Then the local source contains the new task
        assertThat(localDataSource.tasks?.map { it.id }?.contains(newTaskId)).isTrue()
    }

    @Test
    fun getTasks_tasksAreRetrievedFromLocal() =
        testScope.runTest {
            // The repository fetches from the local source
            assertThat(taskRepository.getTasks()).isEqualTo(localTasks.toExternal())
        }

    @Test(expected = Exception::class)
    fun getTasks_localUnavailable_throwsError() = testScope.runTest {
        // When the local source is unavailable
        localDataSource.tasks = null

        // The repository throws an error
        taskRepository.getTasks()
    }

    @Test
    fun completeTask_completesTaskUpdatesCache() = testScope.runTest {
        // Save a task
        val newTaskId = taskRepository.createTask(newTask.title, newTask.description)

        // Make sure it's active
        assertThat(taskRepository.getTask(newTaskId)?.isCompleted).isFalse()

        // Mark is as complete
        taskRepository.completeTask(newTaskId)

        // Verify it's now completed
        assertThat(taskRepository.getTask(newTaskId)?.isCompleted).isTrue()
    }

    @Test
    fun completeTask_activeTaskToServiceAPIUpdatesCache() = testScope.runTest {
        // Save a task
        val newTaskId = taskRepository.createTask(newTask.title, newTask.description)
        taskRepository.completeTask(newTaskId)

        // Make sure it's completed
        assertThat(taskRepository.getTask(newTaskId)?.isActive).isFalse()

        // Mark is as active
        taskRepository.activateTask(newTaskId)

        // Verify it's now activated
        assertThat(taskRepository.getTask(newTaskId)?.isActive).isTrue()
    }

    @Test
    fun clearCompletedTasks() = testScope.runTest {
        val completedTask = task1.copy(status = TaskStatus.DONE)
        localDataSource.tasks = listOf(completedTask.toLocal(), task2.toLocal())
        taskRepository.clearCompletedTasks()

        val tasks = taskRepository.getTasks(true)

        assertThat(tasks).hasSize(1)
        assertThat(tasks).contains(task2)
        assertThat(tasks).doesNotContain(completedTask)
    }

    @Test
    fun deleteAllTasks() = testScope.runTest {
        val initialTasks = taskRepository.getTasks()

        // Verify tasks are returned
        assertThat(initialTasks.size).isEqualTo(1)

        // Delete all tasks
        taskRepository.deleteAllTasks()

        // Verify tasks are empty now
        val afterDeleteTasks = taskRepository.getTasks()
        assertThat(afterDeleteTasks).isEmpty()
    }

    @Test
    fun deleteSingleTask() = testScope.runTest {
        localDataSource.tasks = listOf(task1.toLocal(), task2.toLocal())
        val initialTasksSize = taskRepository.getTasks(true).size

        // Delete first task
        taskRepository.deleteTask(task1.id)

        // Fetch data again
        val afterDeleteTasks = taskRepository.getTasks(true)

        // Verify only one task was deleted
        assertThat(afterDeleteTasks.size).isEqualTo(initialTasksSize - 1)
        assertThat(afterDeleteTasks).doesNotContain(task1)
    }
}

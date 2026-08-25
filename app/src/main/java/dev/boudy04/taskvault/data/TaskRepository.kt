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

import kotlinx.coroutines.flow.Flow

/** Outcome of a note post; the UI maps each to a distinct message. */
enum class NoteResult { ADDED, FORBIDDEN, FAILED }

/**
 * Interface to the data layer.
 */
interface TaskRepository {

    fun getTasksStream(): Flow<List<Task>>

    suspend fun getTasks(forceUpdate: Boolean = false): List<Task>

    suspend fun refresh()

    fun getTaskStream(taskId: String): Flow<Task?>

    suspend fun getTask(taskId: String, forceUpdate: Boolean = false): Task?

    suspend fun refreshTask(taskId: String)

    suspend fun createTask(
        title: String,
        description: String,
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueAt: String? = null,
        tags: List<String> = emptyList(),
        assigneeIds: List<Int> = emptyList(),
        /** LOCAL-ONLY task: Room write only, never queued or pushed to the server. */
        isPersonal: Boolean = false,
    ): String

    suspend fun updateTask(
        taskId: String,
        title: String,
        description: String,
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueAt: String? = null,
        tags: List<String> = emptyList(),
        assigneeIds: List<Int> = emptyList(),
    )

    suspend fun completeTask(taskId: String)

    suspend fun activateTask(taskId: String)

    suspend fun clearCompletedTasks()

    suspend fun deleteAllTasks()

    suspend fun deleteTask(taskId: String)

    /** Local ids of tasks with queued pending ops; drives offline badges in the UI. */
    fun getPendingSyncIdsStream(): Flow<Set<String>>

    fun getSyncStatsStream(): Flow<SyncStats>

    /** All distinct canonical tags across tasks (for the tag filter bar / suggestions). */
    suspend fun getAllTags(): List<String>

    /** Posts a note to the server task; result discloses 403 vs connectivity failure. */
    suspend fun addNote(taskId: String, body: String): NoteResult
}


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

import dev.boudy04.taskvault.data.source.local.LocalTask
import dev.boudy04.taskvault.data.source.local.PendingOpDao
import dev.boudy04.taskvault.data.source.local.PendingOpEntity
import dev.boudy04.taskvault.data.source.local.PendingOpType
import dev.boudy04.taskvault.data.source.local.TaskDao
import dev.boudy04.taskvault.data.source.network.toApi
import dev.boudy04.taskvault.di.DefaultDispatcher
import dev.boudy04.taskvault.sync.ReminderScheduler
import dev.boudy04.taskvault.sync.SyncScheduler
import dev.boudy04.taskvault.sync.TaskPayload
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Offline-first [TaskRepository]: every mutation writes to Room first, enqueues a pending op
 * carrying a serializable [TaskPayload], then asks the [SyncScheduler] for a unique sync run.
 */
@Singleton
class DefaultTaskRepository @Inject constructor(
    private val localDataSource: TaskDao,
    private val pendingOps: PendingOpDao,
    private val syncScheduler: SyncScheduler,
    private val reminders: ReminderScheduler,
    private val json: Json,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : TaskRepository {

    override suspend fun createTask(
        title: String,
        description: String,
        priority: TaskPriority,
        dueAt: String?,
    ): String {
        // ID creation might be a complex operation so it's executed using the supplied
        // coroutine dispatcher
        val taskId = withContext(dispatcher) {
            UUID.randomUUID().toString()
        }
        val task = LocalTask(
            id = taskId,
            title = title,
            description = description,
            isCompleted = false,
            status = TaskStatus.TODO,
            priority = priority,
            dueAt = dueAt,
        )
        localDataSource.upsert(task)
        enqueue(PendingOpType.CREATE, task)
        scheduleReminder(task.id, task.title, task.dueAt)
        return taskId
    }

    override suspend fun updateTask(
        taskId: String,
        title: String,
        description: String,
        priority: TaskPriority,
        dueAt: String?,
    ) {
        val task = localDataSource.getById(taskId) ?: throw Exception("Task ($taskId) not found")
        val updated = task.copy(title = title, description = description, priority = priority, dueAt = dueAt)
        localDataSource.upsert(updated)
        enqueue(PendingOpType.UPDATE, updated)
        // dueAt replaces or clears wholesale, so drop any old alarm first
        reminders.cancel(taskId)
        scheduleReminder(taskId, updated.title, updated.dueAt)
    }

    override suspend fun completeTask(taskId: String) = setStatus(taskId, TaskStatus.DONE)

    override suspend fun activateTask(taskId: String) = setStatus(taskId, TaskStatus.TODO)

    /** Single DAO write preserving both the status column and the legacy isCompleted flag. */
    private suspend fun setStatus(taskId: String, status: TaskStatus) {
        val task = localDataSource.getById(taskId) ?: return
        val updated = task.copy(status = status, isCompleted = status == TaskStatus.DONE)
        localDataSource.upsert(updated)
        enqueue(PendingOpType.UPDATE, updated)
        if (status == TaskStatus.DONE) {
            reminders.cancel(taskId)
        } else {
            // re-activation keeps the stored dueAt; re-arm only while it's still future
            scheduleReminder(taskId, updated.title, updated.dueAt)
        }
    }

    override suspend fun deleteTask(taskId: String) {
        val task = localDataSource.getById(taskId) ?: return
        pendingOps.clearForTask(taskId) // drop stale ops for this row before queueing the tombstone
        localDataSource.deleteById(taskId) // UI reflects deletion instantly
        reminders.cancel(taskId)
        if (task.serverId != null) enqueue(PendingOpType.DELETE, task)
    }

    override suspend fun clearCompletedTasks() {
        localDataSource.getCompleted().forEach { deleteTask(it.id) }
    }

    override suspend fun deleteAllTasks() {
        localDataSource.getAll().forEach { deleteTask(it.id) }
    }

    override suspend fun getTasks(forceUpdate: Boolean): List<Task> {
        if (forceUpdate) syncScheduler.requestSync()
        return withContext(dispatcher) {
            localDataSource.getAll().toExternal()
        }
    }

    override fun getTasksStream(): Flow<List<Task>> =
        localDataSource.observeAll().map { rows ->
            withContext(dispatcher) { rows.toExternal() }
        }

    override suspend fun refresh() {
        syncScheduler.requestSync()
    }

    override fun getTaskStream(taskId: String): Flow<Task?> =
        localDataSource.observeById(taskId).map { it?.toExternal() }

    override suspend fun getTask(taskId: String, forceUpdate: Boolean): Task? {
        if (forceUpdate) syncScheduler.requestSync()
        return localDataSource.getById(taskId)?.toExternal()
    }

    override suspend fun refreshTask(taskId: String) {
        refresh()
    }

    override fun getPendingSyncIdsStream(): Flow<Set<String>> =
        pendingOps.observePendingTaskIds().map { it.toSet() }

    private suspend fun enqueue(type: PendingOpType, task: LocalTask) {
        val payload = TaskPayload(
            localId = task.id,
            title = task.title,
            description = task.description,
            status = task.status.toApi(),
            priority = task.priority.toApi(),
            serverId = task.serverId,
            dueAt = task.dueAt,
        )
        pendingOps.insert(
            PendingOpEntity(taskLocalId = task.id, opType = type, payload = json.encodeToString(payload)),
        )
        syncScheduler.requestSync()
    }

    /** Arms the reminder when the stored due ISO parses to a future instant. */
    private fun scheduleReminder(localId: String, title: String, dueAt: String?) {
        if (dueAt == null) return
        val millis = runCatching { Instant.parse(dueAt).toEpochMilli() }.getOrNull() ?: return
        if (millis > System.currentTimeMillis()) {
            reminders.schedule(localId, title, millis)
        }
    }
}

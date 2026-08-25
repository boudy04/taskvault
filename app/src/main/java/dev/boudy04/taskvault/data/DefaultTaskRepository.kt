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
import dev.boudy04.taskvault.data.source.network.NoteRequest
import dev.boudy04.taskvault.data.source.network.TaskApiService
import dev.boudy04.taskvault.data.source.network.toApi
import dev.boudy04.taskvault.data.joinIds
import dev.boudy04.taskvault.data.joinTags
import dev.boudy04.taskvault.data.parseIds
import dev.boudy04.taskvault.data.parseTags
import dev.boudy04.taskvault.di.DefaultDispatcher
import dev.boudy04.taskvault.settings.SettingsRepository
import dev.boudy04.taskvault.sync.ReminderScheduler
import dev.boudy04.taskvault.sync.SyncScheduler
import dev.boudy04.taskvault.sync.TaskPayload
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Offline-first [TaskRepository]: every team mutation writes to Room first, enqueues a pending op
 * carrying a serializable [TaskPayload], then asks the [SyncScheduler] for a unique sync run.
 * Personal tasks are LOCAL-ONLY: Room writes never enqueue and never reach the network.
 */
@Singleton
class DefaultTaskRepository @Inject constructor(
    private val localDataSource: TaskDao,
    private val pendingOps: PendingOpDao,
    private val syncScheduler: SyncScheduler,
    private val reminders: ReminderScheduler,
    private val api: TaskApiService,
    private val settings: SettingsRepository,
    private val json: Json,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : TaskRepository {

    override suspend fun createTask(
        title: String,
        description: String,
        priority: TaskPriority,
        dueAt: String?,
        tags: List<String>,
        assigneeIds: List<Int>,
        isPersonal: Boolean,
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
            tags = joinTags(tags),
            assigneeIds = joinIds(assigneeIds),
            isPersonal = isPersonal,
        )
        localDataSource.upsert(task)
        if (!isPersonal) {
            enqueue(PendingOpType.CREATE, task)
        }
        scheduleReminder(task.id, task.title, task.dueAt)
        return taskId
    }

    override suspend fun updateTask(
        taskId: String,
        title: String,
        description: String,
        priority: TaskPriority,
        dueAt: String?,
        tags: List<String>,
        assigneeIds: List<Int>,
    ) {
        val task = localDataSource.getById(taskId) ?: throw Exception("Task ($taskId) not found")
        val updated = task.copy(
            title = title,
            description = description,
            priority = priority,
            dueAt = dueAt,
            tags = joinTags(tags),
            assigneeIds = joinIds(assigneeIds),
        )
        localDataSource.upsert(updated)
        // ponytail: personal rows are Room-only by design; if team edits ever need
        // offline queueing per-field, revisit the payload here
        if (!updated.isPersonal) {
            enqueue(PendingOpType.UPDATE, updated)
        }
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
        when {
            // Personal rows stay purely local.
            updated.isPersonal -> Unit
            // Members may only send {"status": ...}; the server rejects anything richer.
            settings.session.first().isMember && updated.serverId != null ->
                enqueue(PendingOpType.STATUS, updated)
            else -> enqueue(PendingOpType.UPDATE, updated)
        }
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
        if (task.serverId != null && !task.isPersonal) enqueue(PendingOpType.DELETE, task)
    }

    override suspend fun addNote(taskId: String, body: String): NoteResult {
        val serverId = localDataSource.getById(taskId)?.serverId ?: return NoteResult.FAILED
        return try {
            api.addNote(serverId, NoteRequest(body))
            refresh() // pull brings the new note back inside the task's TaskRead
            NoteResult.ADDED
        } catch (e: HttpException) {
            if (e.code() == HTTP_FORBIDDEN) NoteResult.FORBIDDEN else NoteResult.FAILED
        } catch (_: Exception) {
            NoteResult.FAILED
        }
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

    override fun getSyncStatsStream(): Flow<SyncStats> =
        combine(localDataSource.observeAll(), pendingOps.observePendingTaskIds()) { rows, pendingIds ->
            val pending = pendingIds.toSet()
            val queued = rows.count { it.id in pending }
            SyncStats(total = rows.size, synced = rows.size - queued, queued = queued)
        }

    override suspend fun getAllTags(): List<String> = withContext(dispatcher) {
        localDataSource.getAllTagGroups().flatMap { parseTags(it) }.distinct()
    }

    private suspend fun enqueue(type: PendingOpType, task: LocalTask) {
        val payload = TaskPayload(
            localId = task.id,
            title = task.title,
            description = task.description,
            status = task.status.toApi(),
            priority = task.priority.toApi(),
            serverId = task.serverId,
            dueAt = task.dueAt,
            tags = parseTags(task.tags),
            assigneeIds = parseIds(task.assigneeIds),
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

    private companion object {
        const val HTTP_FORBIDDEN = 403
    }
}

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
import dev.boudy04.taskvault.data.source.network.TaskDto
import dev.boudy04.taskvault.data.joinIds
import dev.boudy04.taskvault.data.joinTags
import dev.boudy04.taskvault.data.parseIds
import dev.boudy04.taskvault.data.parseTags
import dev.boudy04.taskvault.di.DefaultDispatcher
import dev.boudy04.taskvault.settings.SettingsRepository
import dev.boudy04.taskvault.sync.SyncScheduler
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
 * carrying a serializable [TaskDto], then asks the [SyncScheduler] for a unique sync run.
 * Personal tasks are LOCAL-ONLY: Room writes never enqueue and never reach the network.
 */
@Singleton
class DefaultTaskRepository @Inject constructor(
    private val localDataSource: TaskDao,
    private val pendingOps: PendingOpDao,
    private val syncScheduler: SyncScheduler,
    private val reminderEngine: ReminderEngine,
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
        PendingOpClassifier.forCreate(isPersonal = isPersonal)?.let { enqueue(it, task) }
        reminderEngine.onDueAtSet(task.id, task.title, task.dueAt)
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
        PendingOpClassifier.forUpdate(isPersonal = updated.isPersonal)?.let { enqueue(it, updated) }
        // dueAt replaces or clears wholesale, so drop any old alarm first
        reminderEngine.onCancel(taskId)
        reminderEngine.onDueAtSet(taskId, updated.title, updated.dueAt)
    }

    override suspend fun completeTask(taskId: String) = setStatus(taskId, TaskStatus.DONE)

    override suspend fun activateTask(taskId: String) = setStatus(taskId, TaskStatus.TODO)

    /** Single DAO write preserving both the status column and the legacy isCompleted flag. */
    private suspend fun setStatus(taskId: String, status: TaskStatus) {
        val task = localDataSource.getById(taskId) ?: return
        val updated = task.copy(status = status, isCompleted = status == TaskStatus.DONE)
        localDataSource.upsert(updated)
        // Personal rows stay purely local; members may only send {"status": ...} for
        // server-known rows, anything else falls back to a full UPDATE.
        PendingOpClassifier.forStatusChange(
            isPersonal = updated.isPersonal,
            isMember = settings.session.first().isMember,
            hasServerId = updated.serverId != null,
        )?.let { enqueue(it, updated) }
        if (status == TaskStatus.DONE) {
            reminderEngine.onCancel(taskId)
        } else {
            // re-activation keeps the stored dueAt; re-arm only while it's still future
            reminderEngine.onDueAtSet(taskId, updated.title, updated.dueAt)
        }
    }

    override suspend fun deleteTask(taskId: String) {
        val task = localDataSource.getById(taskId) ?: return
        pendingOps.clearForTask(taskId) // drop stale ops for this row before queueing the tombstone
        localDataSource.deleteById(taskId) // UI reflects deletion instantly
        reminderEngine.onCancel(taskId)
        PendingOpClassifier.forDelete(
            isPersonal = task.isPersonal,
            hasServerId = task.serverId != null,
        )?.let { enqueue(it, task) }
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
        val payload = TaskDto(
            id = task.serverId ?: 0,
            title = task.title,
            description = task.description,
            status = task.status.toApi(),
            priority = task.priority.toApi(),
            dueAt = task.dueAt,
            tags = parseTags(task.tags),
            assigneeIds = parseIds(task.assigneeIds),
        )
        pendingOps.insert(
            PendingOpEntity(taskLocalId = task.id, opType = type, payload = json.encodeToString(payload)),
        )
        syncScheduler.requestSync()
    }

    private companion object {
        const val HTTP_FORBIDDEN = 403
    }
}

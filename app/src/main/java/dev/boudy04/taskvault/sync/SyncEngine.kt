package dev.boudy04.taskvault.sync

import dev.boudy04.taskvault.data.copyFromDto
import dev.boudy04.taskvault.data.source.local.PendingOpDao
import dev.boudy04.taskvault.data.source.local.PendingOpState
import dev.boudy04.taskvault.data.source.local.PendingOpType
import dev.boudy04.taskvault.data.source.local.TaskDao
import dev.boudy04.taskvault.data.source.network.TaskApiService
import dev.boudy04.taskvault.data.source.network.TaskDto
import dev.boudy04.taskvault.data.source.network.TaskStatusUpdate
import dev.boudy04.taskvault.data.toLocal
import dev.boudy04.taskvault.di.IoDispatcher
import dev.boudy04.taskvault.settings.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Drains the offline pending-op queue against the remote API, then pulls the server list to
 * reconcile local rows. Terminal outcomes map 1:1 onto WorkManager results via [SyncWorker].
 */
class SyncEngine @Inject constructor(
    private val api: TaskApiService,
    private val tasks: TaskDao,
    private val ops: PendingOpDao,
    private val json: Json,
    private val settings: SettingsRepository,
    private val viewOnlyRejections: ViewOnlyRejections,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun run(): SyncOutcome = withContext(io) {
        ops.resetRunningToPending()
        val drain = drain()
        if (drain != null) return@withContext drain
        if (pull()) SyncOutcome.SUCCESS else SyncOutcome.CONNECTIVITY_RETRY
    }

    /** Returns terminal outcome to bubble, or null when drain completed cleanly. */
    private suspend fun drain(): SyncOutcome? {
        while (true) {
            val op = ops.nextPending() ?: return null
            ops.updateState(op.opId, PendingOpState.RUNNING)
            val dto = json.decodeFromString<TaskDto>(op.payload)
            try {
                when (op.opType) {
                    PendingOpType.CREATE -> {
                        val created = api.createTask(dto)
                        tasks.getById(op.taskLocalId)?.let { row ->
                            tasks.upsert(
                                row.copy(
                                    serverId = created.id,
                                    createdAt = created.createdAt,
                                    updatedAt = created.updatedAt,
                                ),
                            )
                        }
                    }
                    PendingOpType.UPDATE -> {
                        if (dto.id == 0) error("UPDATE without serverId")
                        val updated = api.updateTask(dto.id, dto)
                        tasks.getById(op.taskLocalId)?.let { row ->
                            tasks.upsert(row.copy(updatedAt = updated.updatedAt))
                        }
                    }
                    PendingOpType.STATUS -> {
                        // Assignee writes carry ONLY {"status": ...}; anything richer gets 403.
                        if (dto.id == 0) error("STATUS without serverId")
                        val updated = api.updateTaskStatus(dto.id, TaskStatusUpdate(dto.status))
                        tasks.getById(op.taskLocalId)?.let { row ->
                            tasks.upsert(row.copy(updatedAt = updated.updatedAt))
                        }
                    }
                    PendingOpType.DELETE -> {
                        if (dto.id != 0) api.deleteTask(dto.id)
                    }
                }
                ops.deleteByIds(listOf(op.opId))
            } catch (e: IOException) {
                ops.updateState(op.opId, PendingOpState.PENDING)
                return SyncOutcome.CONNECTIVITY_RETRY
            } catch (e: HttpException) {
                when (e.code()) {
                    401 -> {
                        // Expired/invalid JWT: stop as terminal failure, keep the stored token.
                        ops.updateState(op.opId, PendingOpState.PENDING)
                        return SyncOutcome.FAILURE
                    }
                    404 -> {
                        if (dto.id != 0) {
                            tasks.getByServerId(dto.id)?.let { row -> tasks.deleteById(row.id) }
                        }
                        ops.deleteByIds(listOf(op.opId))
                    }
                    in 500..599 -> {
                        ops.updateState(op.opId, PendingOpState.PENDING)
                        return SyncOutcome.RETRY
                    }
                    403 -> {
                        // Member token hit a write route: drop the op, keep the row,
                        // and let the task list disclose the role limit.
                        ops.deleteByIds(listOf(op.opId))
                        viewOnlyRejections.signal()
                    }
                    else ->
                        // unrecoverable 4xx: drop op, keep row
                        ops.deleteByIds(listOf(op.opId))
                }
            }
        }
    }

    /** @return true when the server was reachable (regardless of what reconciled). */
    private suspend fun pull(): Boolean {
        val remote = try {
            api.listTasks(null)
        } catch (e: IOException) {
            return false
        } catch (e: Exception) {
            // Non-connectivity failure (parse/5xx/401): skip reconcile silently.
            return true
        }
        val protected = ops.getAll().filter { it.state == PendingOpState.PENDING }.map { it.taskLocalId }.toSet()
        val remoteIds = remote.map { it.id }.toSet()
        remote.forEach { dto ->
            val existing = tasks.getByServerId(dto.id)
            if (existing == null) {
                tasks.upsert(dto.toLocal())
            } else {
                tasks.upsert(existing.copyFromDto(dto))
            }
        }
        tasks.getAll().forEach { row ->
            val sid = row.serverId
            // Personal rows are local-only: reconcile never deletes them.
            if (!row.isPersonal && sid != null && sid !in remoteIds && row.id !in protected) {
                tasks.deleteById(row.id)
            }
        }
        return true
    }
}

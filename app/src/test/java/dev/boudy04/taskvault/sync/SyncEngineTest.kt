package dev.boudy04.taskvault.sync

import com.google.common.truth.Truth.assertThat
import dev.boudy04.taskvault.data.source.local.FakePendingOpDao
import dev.boudy04.taskvault.data.source.local.FakeTaskDao
import dev.boudy04.taskvault.data.source.local.LocalTask
import dev.boudy04.taskvault.data.source.local.PendingOpEntity
import dev.boudy04.taskvault.data.source.local.PendingOpState
import dev.boudy04.taskvault.data.source.local.PendingOpType
import dev.boudy04.taskvault.data.source.network.TaskApiService
import dev.boudy04.taskvault.data.source.network.TaskDto
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Hand-rolled fake of [TaskApiService]: scripted responses, records calls. */
private class FakeApi(
    var listResult: List<TaskDto> = emptyList(),
    var createResult: TaskDto = TaskDto(),
    var updateResult: TaskDto = TaskDto(),
    var createError: Exception? = null,
    var updateError: Exception? = null,
    var listError: Exception? = null,
) : TaskApiService {

    val putIds = mutableListOf<Int>()
    val deletedIds = mutableListOf<Int>()

    override suspend fun listTasks(status: String?): List<TaskDto> {
        listError?.let { throw it }
        return listResult
    }

    override suspend fun getTask(id: Int): TaskDto = error("not used")

    override suspend fun createTask(task: TaskDto): TaskDto {
        createError?.let { throw it }
        return createResult
    }

    override suspend fun updateTask(id: Int, task: TaskDto): TaskDto {
        putIds += id
        updateError?.let { throw it }
        return updateResult
    }

    override suspend fun deleteTask(id: Int): Response<Unit> {
        deletedIds += id
        return Response.success(Unit)
    }
}

private fun httpException(code: Int): HttpException =
    HttpException(Response.error<Any>(code, "".toResponseBody()))

@ExperimentalCoroutinesApi
class SyncEngineTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var api: FakeApi
    private lateinit var tasks: FakeTaskDao
    private lateinit var ops: FakePendingOpDao
    private lateinit var engine: SyncEngine

    @Before
    fun setUp() {
        api = FakeApi()
        tasks = FakeTaskDao()
        ops = FakePendingOpDao()
        engine = SyncEngine(api, tasks, ops, json, dispatcher)
    }

    private fun payload(
        localId: String,
        serverId: Int? = null,
        title: String = "T",
    ) = TaskPayload(
        localId = localId,
        title = title,
        description = "D",
        status = "todo",
        priority = "high",
        serverId = serverId,
    )

    private suspend fun enqueue(type: PendingOpType, p: TaskPayload) {
        ops.insert(
            PendingOpEntity(taskLocalId = p.localId, opType = type, payload = json.encodeToString(p)),
        )
    }

    private fun localTask(id: String, title: String = "Local", serverId: Int? = null) =
        LocalTask(id = id, title = title, description = "D", isCompleted = false, serverId = serverId)

    @Test
    fun drain_createsRemote_assignsServerId_clearsOp() = runTest(dispatcher) {
        tasks.upsert(localTask("l1"))
        enqueue(PendingOpType.CREATE, payload("l1"))
        api.createResult = TaskDto(id = 9, createdAt = "c9", updatedAt = "u9")
        api.listResult = listOf(TaskDto(id = 9, title = "T", status = "todo", priority = "high", createdAt = "c9", updatedAt = "u9"))

        val outcome = engine.run()

        assertThat(outcome).isEqualTo(SyncOutcome.SUCCESS)
        assertThat(tasks.getById("l1")?.serverId).isEqualTo(9)
        assertThat(ops.getAll()).isEmpty()
    }

    @Test
    fun update_usesPutWithServerId() = runTest(dispatcher) {
        tasks.upsert(localTask("l2", serverId = 5))
        enqueue(PendingOpType.UPDATE, payload("l2", serverId = 5))
        api.updateResult = TaskDto(id = 5, updatedAt = "u-new")
        api.listResult = listOf(TaskDto(id = 5, updatedAt = "u-new"))

        val outcome = engine.run()

        assertThat(outcome).isEqualTo(SyncOutcome.SUCCESS)
        assertThat(api.putIds).containsExactly(5)
        assertThat(tasks.getById("l2")?.updatedAt).isEqualTo("u-new")
    }

    @Test
    fun delete_remotely_then_opRemoved() = runTest(dispatcher) {
        enqueue(PendingOpType.DELETE, payload("l3", serverId = 7))

        val outcome = engine.run()

        assertThat(outcome).isEqualTo(SyncOutcome.SUCCESS)
        assertThat(api.deletedIds).containsExactly(7)
        assertThat(ops.getAll()).isEmpty()
    }

    @Test
    fun serverGone_404_dropsOpAndRow() = runTest(dispatcher) {
        tasks.upsert(localTask("l4", serverId = 5))
        enqueue(PendingOpType.UPDATE, payload("l4", serverId = 5))
        api.updateError = httpException(404)

        val outcome = engine.run()

        assertThat(outcome).isEqualTo(SyncOutcome.SUCCESS)
        assertThat(ops.getAll()).isEmpty()
        assertThat(tasks.getById("l4")).isNull()
    }

    @Test
    fun ioError_returnsConnectivityRetry_keepsOpPending() = runTest(dispatcher) {
        tasks.upsert(localTask("l5"))
        enqueue(PendingOpType.CREATE, payload("l5"))
        api.createError = IOException("offline")

        val outcome = engine.run()

        assertThat(outcome).isEqualTo(SyncOutcome.CONNECTIVITY_RETRY)
        assertThat(ops.getAll().single().state).isEqualTo(PendingOpState.PENDING)
    }

    @Test
    fun orphanRunningOp_fromKilledWorker_isReclaimedAndDrained() = runTest(dispatcher) {
        tasks.upsert(localTask("l7"))
        enqueue(PendingOpType.CREATE, payload("l7"))
        // Simulate a worker death mid-drain: the op was marked RUNNING when the process died.
        ops.updateState(ops.getAll().single().opId, PendingOpState.RUNNING)
        api.createResult = TaskDto(id = 11, createdAt = "c11", updatedAt = "u11")
        api.listResult = listOf(
            TaskDto(id = 11, title = "T", status = "todo", priority = "high", createdAt = "c11", updatedAt = "u11"),
        )

        val outcome = engine.run()

        assertThat(outcome).isEqualTo(SyncOutcome.SUCCESS)
        assertThat(tasks.getById("l7")?.serverId).isEqualTo(11)
        assertThat(ops.getAll()).isEmpty()
    }

    @Test
    fun unauthorized_stopsAsFailure() = runTest(dispatcher) {
        tasks.upsert(localTask("l6"))
        enqueue(PendingOpType.CREATE, payload("l6"))
        api.createError = httpException(401)

        val outcome = engine.run()

        assertThat(outcome).isEqualTo(SyncOutcome.FAILURE)
        assertThat(ops.getAll().single().state).isEqualTo(PendingOpState.PENDING)
    }

    @Test
    fun afterDrain_pullMergesServer_deletesVanishedRows_unlessPendingOpsProtectThem() =
        runTest(dispatcher) {
            tasks.upsert(localTask("x", title = "X", serverId = 100))
            // X is absent remotely with no pending ops protecting it; Y is new on the server.
            api.listResult = listOf(TaskDto(id = 200, title = "Y", status = "todo", priority = "low"))

            val outcome = engine.run()

            assertThat(outcome).isEqualTo(SyncOutcome.SUCCESS)
            assertThat(tasks.getByServerId(100)).isNull()
            assertThat(tasks.getByServerId(200)?.title).isEqualTo("Y")
        }

    @Test
    fun offline_pullWithEmptyQueue_yieldsConnectivityRetry() = runTest(dispatcher) {
        api.listError = IOException("airplane mode")

        val outcome = engine.run()

        assertThat(outcome).isEqualTo(SyncOutcome.CONNECTIVITY_RETRY)
    }

    @Test
    fun nonConnectivity_pullFailure_staysSilentSuccess() = runTest(dispatcher) {
        api.listError = httpException(502)

        val outcome = engine.run()

        assertThat(outcome).isEqualTo(SyncOutcome.SUCCESS)
    }
}

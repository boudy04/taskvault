/*
 * Copyright 2026 The Android Open Source Project
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

import dev.boudy04.taskvault.MainCoroutineRule
import dev.boudy04.taskvault.data.source.local.FakePendingOpDao
import dev.boudy04.taskvault.data.source.local.FakeTaskDao
import dev.boudy04.taskvault.data.source.local.LocalTask
import dev.boudy04.taskvault.data.source.local.PendingOpState
import dev.boudy04.taskvault.data.source.local.PendingOpType
import dev.boudy04.taskvault.sync.ReminderScheduler
import dev.boudy04.taskvault.sync.SyncScheduler
import dev.boudy04.taskvault.sync.TaskPayload
import dev.boudy04.taskvault.data.source.network.AdminVerifyRequest
import dev.boudy04.taskvault.data.source.network.AuthRequest
import dev.boudy04.taskvault.data.source.network.AuthResponse
import dev.boudy04.taskvault.data.source.network.MeResponse
import dev.boudy04.taskvault.data.source.network.MemberDto
import dev.boudy04.taskvault.data.source.network.MemberLoginRequest
import dev.boudy04.taskvault.data.source.network.MemberLoginResponse
import dev.boudy04.taskvault.data.source.network.MemberRequest
import dev.boudy04.taskvault.data.source.network.NoteRequest
import dev.boudy04.taskvault.data.source.network.TaskApiService
import dev.boudy04.taskvault.data.source.network.TaskDto
import dev.boudy04.taskvault.settings.FakeSettingsRepository
import dev.boudy04.taskvault.data.source.network.TaskStatusUpdate
import dev.boudy04.taskvault.settings.Session
import retrofit2.Response
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Contract tests for the offline-first repository write path: every mutation writes locally,
 * enqueues a pending op with a serializable [TaskPayload], and requests a sync.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstRepositoryTest {

    @get:Rule val mainDispatcherRule = MainCoroutineRule()

    private val json = Json

    class RecordingSyncScheduler : SyncScheduler {
        val requests = mutableListOf<Long>()
        override fun requestSync() {
            requests += System.nanoTime()
        }
    }

    /** Fake ReminderScheduler recording every schedule/cancel call. */
    class RecordingReminderScheduler : ReminderScheduler {
        val scheduled = mutableListOf<Triple<String, String, Long>>()
        val cancelled = mutableListOf<String>()
        override fun schedule(localId: String, title: String, dueAtMillis: Long) {
            scheduled += Triple(localId, title, dueAtMillis)
        }
        override fun cancel(localId: String) {
            cancelled += localId
        }
    }

    private lateinit var fakeTaskDao: FakeTaskDao
    private lateinit var fakePendingOps: FakePendingOpDao
    private lateinit var syncRequests: RecordingSyncScheduler
    private lateinit var reminders: RecordingReminderScheduler

    private fun repoWithFakes(
        seedTasks: List<LocalTask> = emptyList(),
        memberSession: Boolean = false,
    ): DefaultTaskRepository {
        fakeTaskDao = FakeTaskDao(seedTasks)
        fakePendingOps = FakePendingOpDao()
        syncRequests = RecordingSyncScheduler()
        reminders = RecordingReminderScheduler()
        val settings = FakeSettingsRepository(
            initialToken = "tok",
            initialRole = if (memberSession) Session.MEMBER_ROLE else "admin",
            initialUsername = "tester",
        )
        return DefaultTaskRepository(
            localDataSource = fakeTaskDao,
            pendingOps = fakePendingOps,
            syncScheduler = syncRequests,
            reminders = reminders,
            api = RecordingApi(),
            settings = settings,
            json = json,
            dispatcher = StandardTestDispatcher(mainDispatcherRule.testDispatcher.scheduler),
        )
    }

    /** Empty API stub: the repository only touches it from addNote. */
    private class RecordingApi : TaskApiService {
        override suspend fun addNote(id: Int, body: NoteRequest) = Response.success(Unit)
        override suspend fun updateTaskStatus(id: Int, body: TaskStatusUpdate) = TaskDto(id = id)
        override suspend fun register(body: AuthRequest) = AuthResponse("")
        override suspend fun login(body: AuthRequest) = AuthResponse("")
        override suspend fun listTasks(status: String?) = emptyList<TaskDto>()
        override suspend fun getTask(id: Int) = TaskDto(id = id)
        override suspend fun createTask(task: TaskDto) = task
        override suspend fun updateTask(id: Int, task: TaskDto) = task
        override suspend fun deleteTask(id: Int) = Response.success(Unit)
        override suspend fun listMembers() = emptyList<MemberDto>()
        override suspend fun createMember(body: MemberRequest) = MemberDto(9, body.username)
        override suspend fun deleteMember(id: Int) = Response.success(Unit)
        override suspend fun membersLogin(body: MemberLoginRequest) =
            MemberLoginResponse("t", "member", body.username)
        override suspend fun adminVerify(body: AdminVerifyRequest) = MeResponse(1, "x", "admin")
        override suspend fun membersMe() = MeResponse(1, "x", "member")
    }

    private fun futureIso(): String = Instant.now().plusSeconds(3600).toString()

    @Test
    fun createTask_writesRow_enqueuesCreate_andRequestsSync() = runTest {
        val repo = repoWithFakes()
        val id = repo.createTask("Write plan", "body", TaskPriority.HIGH)
        val stored = fakeTaskDao.getById(id)!!
        assertEquals(TaskPriority.HIGH, stored.priority)
        assertEquals(TaskStatus.TODO, stored.status)
        val op = fakePendingOps.getAll().single()
        assertEquals(PendingOpType.CREATE, op.opType)
        assertEquals(PendingOpState.PENDING, op.state)
        val payload = json.decodeFromString<TaskPayload>(op.payload)
        assertEquals(id, payload.localId)
        assertEquals(1, syncRequests.requests.size)
    }

    @Test
    fun createTask_withTags_storesCanonicalColumn_andPayloadCarriesTags() = runTest {
        val repo = repoWithFakes()
        val id = repo.createTask("Tagged", "body", TaskPriority.LOW, null, listOf(" Work ", "home", "WORK"))

        assertEquals("work,home", fakeTaskDao.getById(id)!!.tags)
        val payload = json.decodeFromString<TaskPayload>(fakePendingOps.getAll().single().payload)
        assertEquals(listOf("work", "home"), payload.tags)
    }

    @Test
    fun updateTask_withNewTags_replacesTagsInRowAndNextPayload() = runTest {
        val repo = repoWithFakes()
        val id = repo.createTask("t", "d", TaskPriority.MEDIUM)

        repo.updateTask(id, "t2", "d2", TaskPriority.HIGH, null, listOf("urgent"))

        assertEquals("urgent", fakeTaskDao.getById(id)!!.tags)
        val lastOp = fakePendingOps.getAll().last()
        val payload = json.decodeFromString<TaskPayload>(lastOp.payload)
        assertEquals(listOf("urgent"), payload.tags)
    }

    @Test
    fun createTask_withAssignees_storesColumn_andPayloadCarriesAssigneeIds() = runTest {
        val repo = repoWithFakes()
        val id = repo.createTask("Assigned", "body", TaskPriority.MEDIUM, null, emptyList(), listOf(4, 2, 4))

        assertEquals("4,2", fakeTaskDao.getById(id)!!.assigneeIds)
        val payload = json.decodeFromString<TaskPayload>(fakePendingOps.getAll().single().payload)
        assertEquals(listOf(4, 2), payload.assigneeIds)
    }

    @Test
    fun updateTask_withNewAssignees_replacesInRowAndNextPayload() = runTest {
        val repo = repoWithFakes()
        val id = repo.createTask("t", "d", TaskPriority.MEDIUM, null, emptyList(), listOf(1))

        repo.updateTask(id, "t", "d", TaskPriority.MEDIUM, null, emptyList(), listOf(7, 8))

        assertEquals("7,8", fakeTaskDao.getById(id)!!.assigneeIds)
        val payload = json.decodeFromString<TaskPayload>(fakePendingOps.getAll().last().payload)
        assertEquals(listOf(7, 8), payload.assigneeIds)
    }

    @Test
    fun deleteTask_withServerId_enqueuesDelete_andDropsRowImmediately() = runTest {
        val seeded = LocalTask(
            id = "local-1",
            title = "Seeded",
            description = "row",
            isCompleted = false,
            serverId = 42,
        )
        val repo = repoWithFakes(seedTasks = listOf(seeded))
        repo.deleteTask("local-1")
        assertNull(fakeTaskDao.getById("local-1"))
        val op = fakePendingOps.getAll().single()
        assertEquals(PendingOpType.DELETE, op.opType)
        val payload = json.decodeFromString<TaskPayload>(op.payload)
        assertEquals(42, payload.serverId)
    }

    @Test
    fun activateComplete_toggleStatus_andEnqueueUpdate() = runTest {
        val repo = repoWithFakes()
        val id = repo.createTask("a", "b", TaskPriority.MEDIUM)
        repo.completeTask(id)
        val done = fakeTaskDao.getById(id)!!
        assertEquals(TaskStatus.DONE, done.status)
        assertEquals(true, done.isCompleted)
        repo.activateTask(id)
        val active = fakeTaskDao.getById(id)!!
        assertEquals(TaskStatus.TODO, active.status)
        assertEquals(false, active.isCompleted)
        val ops = fakePendingOps.getAll()
        assertEquals(3, ops.size)
        assertEquals(1, ops.count { it.opType == PendingOpType.CREATE })
        assertEquals(2, ops.count { it.opType == PendingOpType.UPDATE })
    }

    @Test
    fun deleteTask_unsyncedRow_enqueuesNothing() = runTest {
        val seeded = LocalTask(
            id = "local-only",
            title = "Local",
            description = "never pushed",
            isCompleted = false,
            serverId = null,
        )
        val repo = repoWithFakes(seedTasks = listOf(seeded))
        repo.deleteTask("local-only")
        assertNull(fakeTaskDao.getById("local-only"))
        assertEquals(0, fakePendingOps.getAll().size)
    }

    @Test
    fun clearCompletedTasks_deletesEachLocally() = runTest {
        val seedTasks = listOf(
            LocalTask("done-1", "One", "d", isCompleted = true, status = TaskStatus.DONE, serverId = 10),
            LocalTask("done-2", "Two", "d", isCompleted = true, status = TaskStatus.DONE, serverId = 11),
            LocalTask("active-1", "Three", "d", isCompleted = false),
        )
        val repo = repoWithFakes(seedTasks = seedTasks)
        repo.clearCompletedTasks()
        assertNull(fakeTaskDao.getById("done-1"))
        assertNull(fakeTaskDao.getById("done-2"))
        assertEquals("active-1", fakeTaskDao.getAll().single().id)
        val ops = fakePendingOps.getAll()
        assertEquals(listOf(PendingOpType.DELETE, PendingOpType.DELETE), ops.map { it.opType })
        assertEquals(setOf("done-1", "done-2"), reminders.cancelled.toSet())
    }

    @Test
    fun createTask_withFutureDue_schedulesReminder() = runTest {
        val repo = repoWithFakes()
        val iso = futureIso()
        val id = repo.createTask("Pay rent", "body", TaskPriority.HIGH, iso)
        val call = reminders.scheduled.single()
        assertEquals(id, call.first)
        assertEquals("Pay rent", call.second)
        assertTrue(call.third > System.currentTimeMillis())
        assertEquals(0, reminders.cancelled.size)
    }

    @Test
    fun createTask_withoutDue_doesNotSchedule() = runTest {
        val repo = repoWithFakes()
        repo.createTask("No date", "body", TaskPriority.MEDIUM)
        assertEquals(0, reminders.scheduled.size)
    }

    @Test
    fun updateTask_withNewDue_replacesAlarm() = runTest {
        val repo = repoWithFakes()
        val id = repo.createTask("t", "d", TaskPriority.LOW, futureIso())
        val laterIso = Instant.now().plusSeconds(7200).toString()
        repo.updateTask(id, "t2", "d2", TaskPriority.HIGH, laterIso)
        // old alarm cancelled exactly once, new one armed
        assertEquals(listOf(id), reminders.cancelled)
        assertEquals(2, reminders.scheduled.size)
        assertTrue(fakeTaskDao.getById(id)!!.dueAt == laterIso)
    }

    @Test
    fun updateTask_clearingDue_cancelsWithoutRescheduling() = runTest {
        val repo = repoWithFakes()
        val id = repo.createTask("t", "d", TaskPriority.LOW, futureIso())
        repo.updateTask(id, "t", "d", TaskPriority.LOW, null)
        assertEquals(listOf(id), reminders.cancelled)
        assertEquals(1, reminders.scheduled.size)
        assertNull(fakeTaskDao.getById(id)!!.dueAt)
    }

    @Test
    fun completeTask_cancelsReminder_activate_reschedulesWhileFuture() = runTest {
        val repo = repoWithFakes()
        val id = repo.createTask("t", "d", TaskPriority.MEDIUM, futureIso())
        repo.completeTask(id)
        assertEquals(listOf(id), reminders.cancelled)

        repo.activateTask(id)
        // activation reschedules without an extra cancel: the alarm slot is replaced in place
        assertEquals(2, reminders.scheduled.size)
        assertEquals(listOf(id), reminders.cancelled)
    }

    @Test
    fun deleteTask_cancelsReminder() = runTest {
        val seeded = listOf(
            LocalTask(
                id = "due-1", title = "Due", description = "d",
                isCompleted = false, serverId = 7,
                dueAt = futureIso(),
            ),
        )
        val repo = repoWithFakes(seedTasks = seeded)
        repo.deleteTask("due-1")
        assertNull(fakeTaskDao.getById("due-1"))
        assertEquals(listOf("due-1"), reminders.cancelled)
    }


    // ---------- Task 33: LOCAL-ONLY personal path + member status-only ops ----------

    @Test
    fun createPersonalTask_writesRow_neverEnqueuesOrRequestsSync() = runTest {
        val repo = repoWithFakes()
        val id = repo.createTask("Diary", "private", TaskPriority.LOW, null, emptyList(), emptyList(), isPersonal = true)

        assertEquals("Diary", fakeTaskDao.getById(id)!!.title)
        assertEquals(true, fakeTaskDao.getById(id)!!.isPersonal)
        assertEquals(0, fakePendingOps.getAll().size)
        assertEquals(0, syncRequests.requests.size)
    }

    @Test
    fun personalTask_editCompleteDelete_stayLocalOnly() = runTest {
        val seeded = LocalTask(
            id = "p-1", title = "Personal", description = "d",
            isCompleted = false, isPersonal = true,
        )
        val repo = repoWithFakes(seedTasks = listOf(seeded))

        repo.updateTask("p-1", "Renamed", "d2", TaskPriority.HIGH, null)
        repo.completeTask("p-1")
        repo.deleteTask("p-1")

        assertNull(fakeTaskDao.getById("p-1"))
        assertEquals(0, fakePendingOps.getAll().size)
        assertEquals(0, syncRequests.requests.size)
    }

    @Test
    fun memberComplete_enqueuesStatusOnlyOp() = runTest {
        val seeded = LocalTask(
            id = "a-1", title = "Assigned", description = "d",
            isCompleted = false, serverId = 31,
        )
        val repo = repoWithFakes(seedTasks = listOf(seeded), memberSession = true)

        repo.completeTask("a-1")

        val op = fakePendingOps.getAll().single()
        assertEquals(PendingOpType.STATUS, op.opType)
        val payload = json.decodeFromString<TaskPayload>(op.payload)
        // The wire body built from this payload carries ONLY the status field.
        assertEquals("done", payload.status)
    }

    @Test
    fun adminComplete_enqueuesFullUpdateOp() = runTest {
        val seeded = LocalTask(
            id = "a-2", title = "Team", description = "d",
            isCompleted = false, serverId = 32,
        )
        val repo = repoWithFakes(seedTasks = listOf(seeded))

        repo.completeTask("a-2")

        val op = fakePendingOps.getAll().single()
        assertEquals(PendingOpType.UPDATE, op.opType)
    }
}

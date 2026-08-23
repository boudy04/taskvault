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
    ): DefaultTaskRepository {
        fakeTaskDao = FakeTaskDao(seedTasks)
        fakePendingOps = FakePendingOpDao()
        syncRequests = RecordingSyncScheduler()
        reminders = RecordingReminderScheduler()
        return DefaultTaskRepository(
            localDataSource = fakeTaskDao,
            pendingOps = fakePendingOps,
            syncScheduler = syncRequests,
            reminders = reminders,
            json = json,
            dispatcher = StandardTestDispatcher(mainDispatcherRule.testDispatcher.scheduler),
        )
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

}

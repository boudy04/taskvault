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
import dev.boudy04.taskvault.sync.SyncScheduler
import dev.boudy04.taskvault.sync.TaskPayload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private lateinit var fakeTaskDao: FakeTaskDao
    private lateinit var fakePendingOps: FakePendingOpDao
    private lateinit var syncRequests: RecordingSyncScheduler

    private fun repoWithFakes(
        seedTasks: List<LocalTask> = emptyList(),
    ): DefaultTaskRepository {
        fakeTaskDao = FakeTaskDao(seedTasks)
        fakePendingOps = FakePendingOpDao()
        syncRequests = RecordingSyncScheduler()
        return DefaultTaskRepository(
            localDataSource = fakeTaskDao,
            pendingOps = fakePendingOps,
            syncScheduler = syncRequests,
            json = json,
            dispatcher = StandardTestDispatcher(mainDispatcherRule.testDispatcher.scheduler),
        )
    }

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
    }

}

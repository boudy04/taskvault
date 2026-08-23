package dev.boudy04.taskvault.data.source.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingOpDaoTest {

    private lateinit var db: ToDoDatabase
    private lateinit var dao: PendingOpDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ToDoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.pendingOpDao()
    }

    @Test
    fun nextPending_returnsOldestFirst() = runTest(timeout = 5.minutes) {
        dao.insert(op(enqueuedAt = 20))
        dao.insert(op(enqueuedAt = 10))
        assertEquals(10L, dao.nextPending()?.enqueuedAt)
    }

    @Test
    fun runningOpsAreSkipped_pendingCountReflectsQueue() = runTest(timeout = 5.minutes) {
        val a = dao.insert(op(enqueuedAt = 1))
        dao.updateState(a, PendingOpState.RUNNING)
        dao.insert(op(enqueuedAt = 2))
        assertEquals(1, dao.countPending())
        assertEquals(listOf("t2"), dao.observePendingTaskIds().first())
    }

    @Test
    fun deleteByIdsRemovesOps() = runTest(timeout = 5.minutes) {
        val a = dao.insert(op(enqueuedAt = 1))
        dao.deleteByIds(listOf(a))
        assertEquals(null, dao.nextPending())
    }

    private fun op(enqueuedAt: Long) = PendingOpEntity(
        taskLocalId = "t${enqueuedAt}", opType = PendingOpType.CREATE,
        payload = "{}", enqueuedAt = enqueuedAt,
    )
}

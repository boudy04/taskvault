package dev.boudy04.taskvault.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOpDao {

    @Insert
    suspend fun insert(op: PendingOpEntity): Long

    @Query(
        "SELECT * FROM pending_ops WHERE state = 'PENDING' " +
            "ORDER BY enqueuedAt ASC, opId ASC LIMIT 1"
    )
    suspend fun nextPending(): PendingOpEntity?

    /** Reclaims ops left RUNNING by a killed worker; drains are single-flight so any
     *  RUNNING op at engine start is an orphan. */
    @Query("UPDATE pending_ops SET state = 'PENDING' WHERE state = 'RUNNING'")
    suspend fun resetRunningToPending()

    @Query("UPDATE pending_ops SET state = :state, attempts = attempts + 1 WHERE opId = :opId")
    suspend fun updateState(opId: Long, state: PendingOpState)

    @Query("DELETE FROM pending_ops WHERE opId IN (:opIds)")
    suspend fun deleteByIds(opIds: List<Long>)

    @Query("SELECT * FROM pending_ops ORDER BY enqueuedAt ASC")
    suspend fun getAll(): List<PendingOpEntity>

    @Query("DELETE FROM pending_ops WHERE taskLocalId = :taskLocalId")
    suspend fun clearForTask(taskLocalId: String)

    @Query("SELECT COUNT(*) FROM pending_ops WHERE state = 'PENDING'")
    suspend fun countPending(): Int

    @Query("SELECT DISTINCT taskLocalId FROM pending_ops WHERE state = 'PENDING'")
    fun observePendingTaskIds(): Flow<List<String>>
}

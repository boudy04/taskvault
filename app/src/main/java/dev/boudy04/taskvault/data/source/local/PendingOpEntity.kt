package dev.boudy04.taskvault.data.source.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** STATUS = status-only PUT (the only write shape assignees may send). */
enum class PendingOpType { CREATE, UPDATE, DELETE, STATUS }
enum class PendingOpState { PENDING, RUNNING }

@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true) val opId: Long = 0,
    val taskLocalId: String,
    val opType: PendingOpType,
    val payload: String,
    val state: PendingOpState = PendingOpState.PENDING,
    val attempts: Int = 0,
    val enqueuedAt: Long = System.currentTimeMillis(),
)

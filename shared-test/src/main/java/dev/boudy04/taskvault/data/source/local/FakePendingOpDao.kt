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

package dev.boudy04.taskvault.data.source.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory [PendingOpDao] fake for unit tests. Mirrors the Room query semantics:
 * insert assigns an incrementing opId, nextPending picks min (enqueuedAt, opId) among PENDING,
 * observePendingTaskIds derives distinct local ids of PENDING ops.
 */
class FakePendingOpDao(initialOps: List<PendingOpEntity> = emptyList()) : PendingOpDao {

    private val ops = initialOps.sortedBy { it.opId }.toMutableList()
    private var nextOpId = (initialOps.maxOfOrNull { it.opId } ?: 0L) + 1L

    override suspend fun insert(op: PendingOpEntity): Long {
        val opId = nextOpId++
        ops += op.copy(opId = opId)
        return opId
    }

    override suspend fun nextPending(): PendingOpEntity? =
        ops.filter { it.state == PendingOpState.PENDING }
            .minWithOrNull(compareBy({ it.enqueuedAt }, { it.opId }))

    override suspend fun updateState(opId: Long, state: PendingOpState) {
        val index = ops.indexOfFirst { it.opId == opId }
        if (index >= 0) {
            val current = ops[index]
            ops[index] = current.copy(state = state, attempts = current.attempts + 1)
        }
    }

    override suspend fun deleteByIds(opIds: List<Long>) {
        ops.removeAll { it.opId in opIds }
    }

    override suspend fun getAll(): List<PendingOpEntity> = ops.toList()

    override suspend fun clearForTask(taskLocalId: String) {
        ops.removeAll { it.taskLocalId == taskLocalId }
    }

    override suspend fun countPending(): Int = ops.count { it.state == PendingOpState.PENDING }

    override fun observePendingTaskIds(): Flow<List<String>> =
        flowOf(
            ops.filter { it.state == PendingOpState.PENDING }
                .map { it.taskLocalId }
                .distinct(),
        )
}

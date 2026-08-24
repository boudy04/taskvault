/*
 * Copyright 2023 The Android Open Source Project
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
import dev.boudy04.taskvault.data.source.network.TaskDto
import dev.boudy04.taskvault.data.source.network.toTaskPriority
import dev.boudy04.taskvault.data.source.network.toTaskStatus
import dev.boudy04.taskvault.sync.TaskPayload

/**
 * Data model mapping extension functions. There are two model types:
 *
 * - Task: External model exposed to other layers in the architecture.
 * Obtained using `toExternal`.
 *
 * - LocalTask: Internal model used to represent a task stored locally in a database. Obtained
 * using `toLocal`.
 *
 * TaskDto ↔ domain mappings live with the DTO in `data.source.network.TaskDto.kt`.
 */

// External to local
fun Task.toLocal() = LocalTask(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
).also {
    it.status = status
    it.priority = priority
    it.dueAt = dueAt
    it.tags = joinTags(tags)
    it.assigneeIds = joinIds(assigneeIds)
}

fun List<Task>.toLocal() = map(Task::toLocal)

// Local to External
fun LocalTask.toExternal() = Task(
    id = id,
    title = title,
    description = description,
    status = if (isCompleted) TaskStatus.DONE else TaskStatus.TODO,
    priority = priority,
    dueAt = dueAt,
    tags = parseTags(tags),
    assigneeIds = parseIds(assigneeIds),
    createdAt = createdAt,
)

// Note: JvmName is used to provide a unique name for each extension function with the same name.
// Without this, type erasure will cause compiler errors because these methods will have the same
// signature on the JVM.
@JvmName("localToExternal")
fun List<LocalTask>.toExternal() = map(LocalTask::toExternal)

// Pending-op payload / server DTO mappings used by the sync engine
fun TaskPayload.toDtoWithoutServerId() = TaskDto(
    id = serverId ?: 0,
    title = title,
    description = description,
    status = status,
    priority = priority,
    dueAt = dueAt,
    tags = tags,
    assigneeIds = assigneeIds,
)

fun LocalTask.copyFromDto(dto: TaskDto) = copy(
    title = dto.title,
    description = dto.description,
    isCompleted = dto.status == "done",
    status = dto.status.toTaskStatus(),
    priority = dto.priority.toTaskPriority(),
    createdAt = dto.createdAt,
    updatedAt = dto.updatedAt,
    dueAt = dto.dueAt,
    tags = joinTags(dto.tags),
    assigneeIds = joinIds(dto.assignees.map { it.id }),
)

/** Canonicalizes tags (trim, lowercase, dedupe, drop empties) into the stored column form. */
fun joinTags(tags: List<String>): String =
    tags.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct().joinToString(",")

/** Parses a comma-joined tag column back into its canonical list. */
fun parseTags(column: String): List<String> =
    if (column.isBlank()) {
        emptyList()
    } else {
        column.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }

/** Joins assignee ids into the stored column form ("1,3"). */
fun joinIds(ids: List<Int>): String = ids.filter { it > 0 }.distinct().joinToString(",")

/** Parses a comma-joined assignee-id column back into its list. */
fun parseIds(column: String): List<Int> =
    if (column.isBlank()) {
        emptyList()
    } else {
        column.split(',').mapNotNull { it.trim().toIntOrNull() }
    }

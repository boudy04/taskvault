package dev.boudy04.taskvault.data.source.network

import dev.boudy04.taskvault.data.TaskPriority
import dev.boudy04.taskvault.data.TaskStatus
import dev.boudy04.taskvault.data.joinIds
import dev.boudy04.taskvault.data.joinNotes
import dev.boudy04.taskvault.data.joinTags
import dev.boudy04.taskvault.data.parseIds
import dev.boudy04.taskvault.data.parseTags
import dev.boudy04.taskvault.data.source.local.LocalTask
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TaskDto(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val status: String = "todo",
    val priority: String = "medium",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("tags") val tags: List<String> = emptyList(),
    /** Server read shape: assignee rows alphabetized by username. */
    @SerialName("assignees") val assignees: List<MemberDto> = emptyList(),
    /** Server write shape: full replacement list of member ids. */
    @SerialName("assignee_ids") val assigneeIds: List<Int> = emptyList(),
    /** TaskRead notes, newest order as returned by the server. */
    val notes: List<NoteDto> = emptyList(),
)

fun TaskStatus.toApi(): String = when (this) {
    TaskStatus.TODO -> "todo"
    TaskStatus.IN_PROGRESS -> "in_progress"
    TaskStatus.DONE -> "done"
}

fun TaskPriority.toApi(): String = when (this) {
    TaskPriority.LOW -> "low"
    TaskPriority.MEDIUM -> "medium"
    TaskPriority.HIGH -> "high"
}

fun String.toTaskStatus(): TaskStatus = when (this) {
    "done" -> TaskStatus.DONE
    "in_progress" -> TaskStatus.IN_PROGRESS
    else -> TaskStatus.TODO
}

fun String.toTaskPriority(): TaskPriority = when (this) {
    "high" -> TaskPriority.HIGH
    "low" -> TaskPriority.LOW
    else -> TaskPriority.MEDIUM
}

/** Server task → fresh local row (new UUID, linked by serverId). */
fun TaskDto.toLocal(): LocalTask = LocalTask(
    id = UUID.randomUUID().toString(),
    title = title,
    description = description,
    isCompleted = status == "done",
    status = status.toTaskStatus(),
    priority = priority.toTaskPriority(),
    serverId = id,
    createdAt = createdAt,
    updatedAt = updatedAt,
    dueAt = dueAt,
    tags = joinTags(tags),
    assigneeIds = joinIds(assignees.map { it.id }),
    notes = joinNotes(notes),
)

fun LocalTask.toDto(): TaskDto = TaskDto(
    id = serverId ?: 0,
    title = title,
    description = description,
    status = status.toApi(),
    priority = priority.toApi(),
    dueAt = dueAt,
    tags = parseTags(tags),
    assigneeIds = parseIds(assigneeIds),
)

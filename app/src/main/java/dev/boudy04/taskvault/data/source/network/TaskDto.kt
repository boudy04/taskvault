package dev.boudy04.taskvault.data.source.network

import dev.boudy04.taskvault.data.TaskPriority
import dev.boudy04.taskvault.data.TaskStatus
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
)

fun LocalTask.toDto(): TaskDto = TaskDto(
    id = serverId ?: 0,
    title = title,
    description = description,
    status = status.toApi(),
    priority = priority.toApi(),
)

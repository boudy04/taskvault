package dev.boudy04.taskvault.data.source.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Server wire shape for a task. Plain DTO by design (refactor Step 1): every mapping to or from
 * this shape lives in `dev.boudy04.taskvault.data.ModelMappingExt.kt`.
 */
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

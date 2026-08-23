package dev.boudy04.taskvault.sync

import kotlinx.serialization.Serializable

/** Serializable snapshot of a task row carried inside a pending op payload. */
@Serializable
data class TaskPayload(
    val localId: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val serverId: Int? = null,
    val dueAt: String? = null,
)

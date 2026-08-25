package dev.boudy04.taskvault.data.source.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request body for /api/auth/register and /api/auth/login. */
@Serializable
data class AuthRequest(val username: String, val password: String)

/** JWT carrier returned by both auth endpoints. */
@Serializable
data class AuthResponse(val token: String)

/** Username-only login body for /api/members/login. */
@Serializable
data class MemberLoginRequest(val username: String)

/** Token carrier issued to a member; role is always "member" today. */
@Serializable
data class MemberLoginResponse(val token: String, val role: String, val username: String)

/** Workspace-key check body for /api/admin/verify. */
@Serializable
data class AdminVerifyRequest(val token: String)

/** Identity echo returned by /api/admin/verify and /api/members/me. */
@Serializable
data class MeResponse(val id: Int, val username: String, val role: String)

/** Server task note (read shape). */
@Serializable
data class NoteDto(
    val author: String,
    val body: String,
    @SerialName("created_at") val createdAt: String = "",
)

/** Note create body for POST /api/tasks/{id}/notes. */
@Serializable
data class NoteRequest(val body: String)

/** Status-only write body: the only payload assignees may PUT. */
@Serializable
data class TaskStatusUpdate(val status: String)

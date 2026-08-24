package dev.boudy04.taskvault.data.source.network

import kotlinx.serialization.Serializable

/** Demo workspace member: username-only row, no passwords (decision R25). */
@Serializable
data class MemberDto(val id: Int, val username: String)

@Serializable
data class MemberRequest(val username: String)

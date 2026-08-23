package dev.boudy04.taskvault.data.source.network

import kotlinx.serialization.Serializable

/** Request body for /api/auth/register and /api/auth/login. */
@Serializable
data class AuthRequest(val username: String, val password: String)

/** JWT carrier returned by both auth endpoints. */
@Serializable
data class AuthResponse(val token: String)

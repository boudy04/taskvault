package dev.boudy04.taskvault.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [SettingsRepository] for JVM tests; records config and session writes. */
class FakeSettingsRepository(
    initialUrl: String = "https://example.com",
    initialToken: String = "",
    initialRole: String = "",
    initialUsername: String = "",
) : SettingsRepository {

    private val configState = MutableStateFlow(ServerConfig(initialUrl, initialToken))
    private val sessionState = MutableStateFlow(Session(initialToken, initialRole, initialUsername))

    /** Convenience read accessors for assertions. */
    val sessionToken: String get() = sessionState.value.token
    val sessionRole: String get() = sessionState.value.role
    val sessionUsername: String get() = sessionState.value.username

    override val config: Flow<ServerConfig> = configState

    override val session: Flow<Session> = sessionState

    override val themeMode: Flow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)

    override val fontFamily: Flow<String> = MutableStateFlow("app")

    override val appLock: Flow<Boolean> = MutableStateFlow(false)

    override suspend fun current(): ServerConfig = configState.value

    override suspend fun setConfig(config: ServerConfig) {
        configState.value = config
        // The settings form writes the workspace key straight into the session token;
        // role/username are only known after a verify, so they stay untouched.
        sessionState.value = sessionState.value.copy(token = config.token)
    }

    override suspend fun setSession(token: String, role: String, username: String) {
        sessionState.value = Session(token, role, username)
        configState.value = configState.value.copy(token = token)
    }

    override suspend fun clearSession() {
        sessionState.value = Session()
    }

    override suspend fun setThemeMode(mode: ThemeMode) = Unit

    override suspend fun setFontFamily(family: String) = Unit

    override suspend fun setAppLock(enabled: Boolean) = Unit
}

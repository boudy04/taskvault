package dev.boudy04.taskvault.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [SettingsRepository] for JVM tests; records session writes. */
class FakeSettingsRepository(
    initialUrl: String = "https://example.com",
    initialToken: String = "",
    initialUsername: String = "",
) : SettingsRepository {

    private val configState = MutableStateFlow(ServerConfig(initialUrl, initialToken))
    private val usernameState = MutableStateFlow(initialUsername)

    /** Convenience read accessors for assertions. */
    val sessionToken: String get() = configState.value.token
    val accountName: String get() = usernameState.value
    val clearedCount: Int get() = clearCount

    private var clearCount = 0

    override val config: Flow<ServerConfig> = configState

    override val themeMode: Flow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)

    override val fontFamily: Flow<String> = MutableStateFlow("app")

    override val appLock: Flow<Boolean> = MutableStateFlow(false)

    override val username: Flow<String> = usernameState

    override val loggedIn: Flow<Boolean> = configState.map { it.token.isNotBlank() }

    override suspend fun current(): ServerConfig = configState.value

    override suspend fun setConfig(config: ServerConfig) {
        configState.value = config
    }

    override suspend fun setThemeMode(mode: ThemeMode) = Unit

    override suspend fun setFontFamily(family: String) = Unit

    override suspend fun setAppLock(enabled: Boolean) = Unit

    override suspend fun setSession(token: String, username: String) {
        configState.value = configState.value.copy(token = token)
        usernameState.value = username
    }

    override suspend fun clearSession() {
        clearCount++
        configState.value = configState.value.copy(token = "")
        usernameState.value = ""
    }
}

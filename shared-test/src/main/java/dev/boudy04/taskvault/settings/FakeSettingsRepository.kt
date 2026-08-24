package dev.boudy04.taskvault.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [SettingsRepository] for JVM tests; records config writes. */
class FakeSettingsRepository(
    initialUrl: String = "https://example.com",
    initialToken: String = "",
) : SettingsRepository {

    private val configState = MutableStateFlow(ServerConfig(initialUrl, initialToken))

    /** Convenience read accessors for assertions. */
    val sessionToken: String get() = configState.value.token

    override val config: Flow<ServerConfig> = configState

    override val themeMode: Flow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)

    override val fontFamily: Flow<String> = MutableStateFlow("app")

    override val appLock: Flow<Boolean> = MutableStateFlow(false)

    override suspend fun current(): ServerConfig = configState.value

    override suspend fun setConfig(config: ServerConfig) {
        configState.value = config
    }

    override suspend fun setThemeMode(mode: ThemeMode) = Unit

    override suspend fun setFontFamily(family: String) = Unit

    override suspend fun setAppLock(enabled: Boolean) = Unit
}

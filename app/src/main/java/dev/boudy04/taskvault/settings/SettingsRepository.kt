package dev.boudy04.taskvault.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

interface SettingsRepository {
    val config: Flow<ServerConfig>
    val themeMode: Flow<ThemeMode>
    val fontFamily: Flow<String>
    val appLock: Flow<Boolean>

    /** Account username of the active JWT session; "" when logged out. */
    val username: Flow<String>

    /** True while a (non-blank) JWT token is stored. */
    val loggedIn: Flow<Boolean>

    suspend fun current(): ServerConfig
    suspend fun setConfig(config: ServerConfig)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setFontFamily(family: String)
    suspend fun setAppLock(enabled: Boolean)
    suspend fun setSession(token: String, username: String)
    suspend fun clearSession()
}

private const val DEFAULT_URL = "https://prject-cv-production.up.railway.app"
// Blank by default: a fresh install has no JWT, so the login screen shows first.
private const val DEFAULT_TOKEN = ""

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val url = stringPreferencesKey("server_url")
        val token = stringPreferencesKey("auth_token")
        val username = stringPreferencesKey("username")
        val themeMode = stringPreferencesKey("theme_mode")
        val fontFamily = stringPreferencesKey("font_family")
        val appLock = booleanPreferencesKey("app_lock")
    }

    override val config: Flow<ServerConfig> = dataStore.data.map { p ->
        ServerConfig(p[Keys.url] ?: DEFAULT_URL, p[Keys.token] ?: DEFAULT_TOKEN)
    }

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { p ->
        p[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    override suspend fun current(): ServerConfig = config.first()

    override suspend fun setConfig(config: ServerConfig) {
        dataStore.edit { p ->
            p[Keys.url] = config.baseUrl
            p[Keys.token] = config.token
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { p ->
            p[Keys.themeMode] = mode.name
        }
    }

    override val fontFamily: Flow<String> = dataStore.data.map { p ->
        p[Keys.fontFamily]?.takeIf { it == "app" || it == "system" } ?: "app"
    }

    override suspend fun setFontFamily(family: String) {
        dataStore.edit { p ->
            p[Keys.fontFamily] = family
        }
    }

    override val appLock: Flow<Boolean> = dataStore.data.map { p -> p[Keys.appLock] ?: false }

    override suspend fun setAppLock(enabled: Boolean) {
        dataStore.edit { p ->
            p[Keys.appLock] = enabled
        }
    }

    override val username: Flow<String> = dataStore.data.map { p -> p[Keys.username] ?: "" }

    override val loggedIn: Flow<Boolean> = dataStore.data.map { p -> !p[Keys.token].isNullOrBlank() }

    override suspend fun setSession(token: String, username: String) {
        dataStore.edit { p ->
            p[Keys.token] = token
            p[Keys.username] = username
        }
    }

    override suspend fun clearSession() {
        dataStore.edit { p ->
            p[Keys.token] = ""
            p[Keys.username] = ""
        }
    }
}

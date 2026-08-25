package dev.boudy04.taskvault.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }
/** Who is signed in; blank token = nobody (two-input login shows). */
data class Session(
    val token: String = "",
    val role: String = "",
    val username: String = "",
    /** Workspace member id from /api/members/me; drives the "assigned to you" section. */
    val userId: Int = 0,
) {
    /** Member sessions are view-only; the UI mirrors what the server enforces. */
    val isMember: Boolean get() = role == MEMBER_ROLE

    companion object {
        const val MEMBER_ROLE = "member"
    }
}

interface SettingsRepository {
    val config: Flow<ServerConfig>
    val session: Flow<Session>
    val themeMode: Flow<ThemeMode>
    val fontFamily: Flow<String>
    val appLock: Flow<Boolean>

    suspend fun current(): ServerConfig
    suspend fun setConfig(config: ServerConfig)
    suspend fun setSession(token: String, role: String, username: String)
    /** Resolves and stores the workspace member id after a successful login. */
    suspend fun setUserId(id: Int)
    suspend fun clearSession()
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setFontFamily(family: String)
    suspend fun setAppLock(enabled: Boolean)
}

private const val DEFAULT_URL = "https://prject-cv-production.up.railway.app"
private const val DEFAULT_TOKEN = ""

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val url = stringPreferencesKey("server_url")
        val token = stringPreferencesKey("auth_token")
        val role = stringPreferencesKey("auth_role")
        val username = stringPreferencesKey("auth_username")
        val userId = intPreferencesKey("username_id")
        val themeMode = stringPreferencesKey("theme_mode")
        val fontFamily = stringPreferencesKey("font_family")
        val appLock = booleanPreferencesKey("app_lock")
    }


    override val session: Flow<Session> = dataStore.data.map { p ->
        Session(
            token = p[Keys.token] ?: DEFAULT_TOKEN,
            role = p[Keys.role] ?: "",
            username = p[Keys.username] ?: "",
            userId = p[Keys.userId] ?: 0,
        )
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

    override suspend fun setSession(token: String, role: String, username: String) {
        dataStore.edit { p ->
            p[Keys.token] = token
            p[Keys.role] = role
            p[Keys.username] = username
        }
    }


    override suspend fun setUserId(id: Int) {
        dataStore.edit { p ->
            p[Keys.userId] = id
        }
    }
    override suspend fun clearSession() {
        dataStore.edit { p ->
            p.remove(Keys.token)
            p.remove(Keys.role)
            p.remove(Keys.username)
            p.remove(Keys.userId)
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
}

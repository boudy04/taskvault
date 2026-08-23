package dev.boudy04.taskvault.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun repo(scope: CoroutineScope) = DataStoreSettingsRepository(
        PreferenceDataStoreFactory.create(scope = CoroutineScope(scope.coroutineContext + Dispatchers.IO)) {
            tmp.newFile("settings.preferences_pb")
        },
    )

    @Test
    fun defaults_areRailwayAndDemoToken() = runTest {
        val r = repo(backgroundScope)
        val c = r.current()
        assertEquals("https://prject-cv-production.up.railway.app", c.baseUrl)
        assertEquals("dev-token", c.token)
    }

    @Test
    fun setConfig_persists() = runTest {
        val r = repo(backgroundScope)
        r.setConfig(ServerConfig("http://10.0.2.2:8000", "abc"))
        assertEquals("abc", r.config.first().token)
        assertEquals("http://10.0.2.2:8000", r.config.first().baseUrl)
    }

    @Test
    fun themeMode_persists() = runTest {
        val r = repo(backgroundScope)
        assertEquals(ThemeMode.SYSTEM, r.themeMode.first())
        r.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, r.themeMode.first())
    }
}

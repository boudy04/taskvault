package dev.boudy04.taskvault.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun defaults_areRailwayAndBlankToken() = runTest {
        val r = repo(backgroundScope)
        val c = r.current()
        assertEquals("https://prject-cv-production.up.railway.app", c.baseUrl)
        assertEquals("", c.token)
    }

    @Test
    fun setConfig_persists() = runTest {
        val r = repo(backgroundScope)
        r.setConfig(ServerConfig("http://10.0.2.2:8000", "abc"))
        assertEquals("abc", r.config.first().token)
        assertEquals("http://10.0.2.2:8000", r.config.first().baseUrl)
    }

    @Test
    fun setConfig_customToken_roundTripsThroughCurrent() = runTest {
        // Regression for the restored token field: a user-entered JWT must survive save/reload.
        val r = repo(backgroundScope)
        r.setConfig(ServerConfig("https://example.org", "custom-jwt-123"))
        assertEquals("custom-jwt-123", r.current().token)
        assertEquals("https://example.org", r.current().baseUrl)
    }

    @Test
    fun themeMode_persists() = runTest {
        val r = repo(backgroundScope)
        assertEquals(ThemeMode.SYSTEM, r.themeMode.first())
        r.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, r.themeMode.first())
    }

    @Test
    fun appLock_persists() = runTest {
        val r = repo(backgroundScope)
        assertEquals(false, r.appLock.first())
        r.setAppLock(true)
        assertEquals(true, r.appLock.first())
        r.setAppLock(false)
        assertEquals(false, r.appLock.first())
    }

    @Test
    fun session_roundTripsTokenRoleUsername() = runTest {
        val r = repo(backgroundScope)
        assertEquals(Session("", "", ""), r.session.first())
        r.setSession("tok-1", "member", "alice")
        val s = r.session.first()
        assertEquals("tok-1", s.token)
        assertEquals("member", s.role)
        assertEquals("alice", s.username)
        assertTrue(s.isMember)
        // The auth interceptor reads the token via config; both keys stay in sync.
        assertEquals("tok-1", r.current().token)
    }

    @Test
    fun clearSession_wipesAllThreeKeys() = runTest {
        val r = repo(backgroundScope)
        r.setSession("tok-2", "admin", "boudy04")
        r.clearSession()
        val s = r.session.first()
        assertEquals("", s.token)
        assertEquals("", s.role)
        assertEquals("", s.username)
    }

    @Test
    fun adminSession_isNotMember() = runTest {
        val r = repo(backgroundScope)
        r.setSession("dev-token", "admin", "boudy04")
        assertFalse(r.session.first().isMember)
    }
}

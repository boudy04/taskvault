package dev.boudy04.taskvault.settings

import dev.boudy04.taskvault.MainCoroutineRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** Covers the restored direct-token settings flow: edit URL/token, save, persisted. */
@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private fun vmWith(settings: FakeSettingsRepository) = SettingsViewModel(settings)

    @Test
    fun init_loadsCurrentUrlAndToken() = runTest {
        val settings = FakeSettingsRepository(
            initialUrl = "https://example.com",
            initialToken = "stored-jwt",
        )

        val vm = vmWith(settings)

        assertThat(vm.uiState.value.baseUrl).isEqualTo("https://example.com")
        assertThat(vm.uiState.value.token).isEqualTo("stored-jwt")
    }

    @Test
    fun updateToken_updatesUiState() = runTest {
        val settings = FakeSettingsRepository()
        val vm = vmWith(settings)

        vm.updateToken("new-jwt")

        assertThat(vm.uiState.value.token).isEqualTo("new-jwt")
    }

    @Test
    fun saveConfig_persistsBaseUrlAndToken_roundTrip() = runTest {
        // Regression for the restored token field: Save must persist the edited JWT too.
        val settings = FakeSettingsRepository(initialUrl = "https://example.com")
        val vm = vmWith(settings)
        vm.updateBaseUrl(" https://example.org ")
        vm.updateToken("edited-jwt")

        vm.saveConfig()

        assertThat(settings.current().baseUrl).isEqualTo("https://example.org")
        assertThat(settings.sessionToken).isEqualTo("edited-jwt")
    }

    @Test
    fun saveConfig_showsSavedMessage_untilAcknowledged() = runTest {
        val settings = FakeSettingsRepository()
        val vm = vmWith(settings)

        vm.saveConfig()

        assertThat(vm.uiState.value.resultText).isEqualTo("Saved")
        vm.resultMessageShown()
        assertThat(vm.uiState.value.resultText).isNull()
    }
}

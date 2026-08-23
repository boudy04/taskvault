/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.boudy04.taskvault.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * UiState for the settings screen.
 */
data class SettingsUiState(
    val baseUrl: String = "",
    val token: String = "",
    val appLock: Boolean = false,
    val resultText: String? = null,
)

/**
 * ViewModel for the server settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val current = settings.current()
            _uiState.update { it.copy(baseUrl = current.baseUrl, token = current.token) }
        }
        viewModelScope.launch {
            settings.appLock.collect { lock -> _uiState.update { it.copy(appLock = lock) } }
        }
    }

    fun updateBaseUrl(value: String) {
        _uiState.update { it.copy(baseUrl = value) }
    }

    fun updateToken(value: String) {
        _uiState.update { it.copy(token = value) }
    }

    fun saveConfig() {
        viewModelScope.launch {
            settings.setConfig(ServerConfig(_uiState.value.baseUrl.trim(), _uiState.value.token))
            _uiState.update { it.copy(resultText = SAVED_MESSAGE) }
        }
    }

    fun setAppLock(enabled: Boolean) {
        viewModelScope.launch { settings.setAppLock(enabled) }
    }

    // ponytail: one-shot OkHttpClient per test press; swap for injected client if this grows
    fun testConnection() {
        viewModelScope.launch {
            val url = _uiState.value.baseUrl.trim().trimEnd('/') + "/health"
            try {
                val response = withContext(Dispatchers.IO) {
                    OkHttpClient()
                        .newCall(Request.Builder().url(url).build())
                        .execute()
                }
                response.use { resp ->
                    _uiState.update { it.copy(resultText = "HTTP ${resp.code}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(resultText = e.message ?: ERROR_MESSAGE) }
            }
        }
    }

    fun resultMessageShown() {
        _uiState.update { it.copy(resultText = null) }
    }

    private companion object {
        const val SAVED_MESSAGE = "Saved"
        const val ERROR_MESSAGE = "Connection failed"
    }
}

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

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.data.source.network.MemberDto
import dev.boudy04.taskvault.data.source.network.MemberRequest
import dev.boudy04.taskvault.data.source.network.TaskApiService
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * UiState for the Settings > Team card. [errorRes] surfaces the last failure
 * inline; null once a later call succeeds.
 */
data class TeamUiState(
    val members: List<MemberDto> = emptyList(),
    val newName: String = "",
    @StringRes val errorRes: Int? = null,
)

/**
 * Demo workspace members (decision R25). The API has no owner flag yet, so the
 * owner row is identified by username == "boudy04" in the UI layer.
 */
@HiltViewModel
class TeamViewModel @Inject constructor(
    private val api: TaskApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(members = api.listMembers(), errorRes = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorRes = R.string.team_error_offline) }
            }
        }
    }

    fun updateNewName(value: String) {
        _uiState.update { it.copy(newName = value) }
    }

    fun addMember() {
        // Server normalizes too; pre-normalizing keeps optimistic input tidy.
        val name = _uiState.value.newName.trim().lowercase()
        viewModelScope.launch {
            try {
                api.createMember(MemberRequest(name))
                _uiState.update { it.copy(newName = "", errorRes = null) }
                refresh()
            } catch (e: HttpException) {
                _uiState.update {
                    it.copy(errorRes = when (e.code()) {
                        HTTP_CONFLICT -> R.string.team_error_duplicate
                        HTTP_UNPROCESSABLE -> R.string.team_error_invalid
                        else -> R.string.team_error_offline
                    })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorRes = R.string.team_error_offline) }
            }
        }
    }

    fun removeMember(id: Int) {
        viewModelScope.launch {
            try {
                api.deleteMember(id)
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorRes = R.string.team_error_offline) }
            }
        }
    }

    private companion object {
        const val HTTP_CONFLICT = 409
        const val HTTP_UNPROCESSABLE = 422
    }
}

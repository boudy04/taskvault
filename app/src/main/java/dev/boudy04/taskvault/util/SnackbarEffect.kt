/*
 * Copyright 2022 The Android Open Source Project
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

package dev.boudy04.taskvault.util

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource

/**
 * Shows [userMessage] as a snackbar once, then invokes [onMessageShown] so the
 * ViewModel can clear the pending message. No-op while [userMessage] is null.
 */
@Composable
internal fun SnackbarHostEffect(
    userMessage: Int?,
    snackbarHostState: SnackbarHostState,
    onMessageShown: () -> Unit,
) {
    val message = userMessage ?: return
    val snackbarText = stringResource(message)
    LaunchedEffect(snackbarHostState, message, snackbarText) {
        snackbarHostState.showSnackbar(snackbarText)
        onMessageShown()
    }
}

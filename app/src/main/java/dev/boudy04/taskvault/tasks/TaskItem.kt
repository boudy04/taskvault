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

package dev.boudy04.taskvault.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.data.Task
import dev.boudy04.taskvault.util.DueDates

@Composable
internal fun TaskItem(
    task: Task,
    members: List<dev.boudy04.taskvault.data.source.network.MemberDto>,
    isUnsynced: Boolean,
    showNotes: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTaskClick: (Task) -> Unit,
    onOpenNotes: () -> Unit,
    checkEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val latestNote = task.notes.firstOrNull()
    val hasMetadata = task.tags.isNotEmpty() || task.dueAt != null ||
        task.assigneeIds.isNotEmpty() || latestNote != null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTaskClick(task) }
                .padding(all = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniCheckbox(
                    checked = task.isCompleted,
                    enabled = checkEnabled,
                    onCheckedChange = { checked ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCheckedChange(checked)
                    },
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (task.isCompleted) 0.45f else 1f)
                ) {
                    Text(
                        text = task.titleForList,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textDecoration = if (task.isCompleted) {
                            TextDecoration.LineThrough
                        } else {
                            null
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hasMetadata) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            task.tags.forEach { tag -> MetaChip(text = tag) }
                            task.dueAt?.let { iso -> DueChip(iso = iso, isActive = task.isActive) }
                            Spacer(Modifier.weight(1f))
                            AssigneeBadges(assigneeIds = task.assigneeIds, members = members)
                        }
                    }
                    latestNote?.let { note ->
                        Text(
                            text = "${note.author}: ${note.body}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                if (showNotes) {
                    IconButton(onClick = onOpenNotes, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.Notes,
                            contentDescription = stringResource(R.string.cd_open_notes),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                SyncBadge(isUnsynced = isUnsynced, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}

/** Tiny outlined pill for row metadata (group names). */
@Composable
private fun MetaChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** Due pill: amber while upcoming, red once overdue. */
@Composable
private fun DueChip(iso: String, isActive: Boolean) {
    // ponytail: cached per (dueAt, active); overdue staleness within a minute is acceptable
    val (overdue, dueText) = remember(iso, isActive) {
        val isOverdue = runCatching {
            java.time.Instant.parse(iso).isBefore(java.time.Instant.now()) && isActive
        }.getOrDefault(false)
        isOverdue to DueDates.format(iso).orEmpty()
    }
    val dueColor = if (overdue) Color(0xFFB3261E) else Color(0xFFFFB300)
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = dueColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = dueText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = dueColor,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/** 24dp circle checkbox: primary fill + check when selected, hairline ring otherwise. */
@Composable
private fun MiniCheckbox(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .then(
                if (checked) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                }
            )
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                enabled = enabled,
                onValueChange = onCheckedChange
            )
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** Sync state icon, vertically centered at the card's trailing edge. */
@Composable
private fun SyncBadge(isUnsynced: Boolean, modifier: Modifier = Modifier) {
    if (isUnsynced) {
        Icon(
            Icons.Filled.CloudOff,
            contentDescription = stringResource(R.string.cd_sync_pending),
            tint = Color(0xFFFFB300),
            modifier = modifier
                .size(18.dp)
        )
    } else {
        Icon(
            Icons.Filled.CloudDone,
            contentDescription = stringResource(R.string.cd_sync_synced),
            tint = Color(0xFF38693C).copy(alpha = 0.6f),
            modifier = modifier.size(18.dp)
        )
    }
}

// ponytail: fixed muted palette hashed by username; per-user theming would need server colors
private val assigneeColors = listOf(
    Color(0xFF7986CB), Color(0xFF4DB6AC), Color(0xFFE57373),
    Color(0xFFFFB74D), Color(0xFF9575CD), Color(0xFF81C784),
)

/** Colored initial circles for assigned members; shows at most 3 plus a "+n". */
@Composable
private fun AssigneeBadges(
    assigneeIds: List<Int>,
    members: List<dev.boudy04.taskvault.data.source.network.MemberDto>,
    modifier: Modifier = Modifier
) {
    if (assigneeIds.isEmpty()) return

    val shown = assigneeIds.take(MAX_BADGES)
    val overflow = assigneeIds.size - shown.size

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        shown.forEach { id ->
            val name = members.firstOrNull { it.id == id }?.username
            // Offline / unknown member: fall back to a stable id-derived initial.
            val initial = (name?.take(1) ?: ('A' + (Math.abs(id) % 26)).toString()).uppercase()
            val bg = assigneeColors[Math.abs((name ?: id.toString()).hashCode()) % assigneeColors.size]
            val desc = stringResource(R.string.cd_assignee_badge, name ?: initial)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp)
                    .background(bg, CircleShape)
                    .semantics { contentDescription = desc }
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
        if (overflow > 0) {
            Text(
                text = stringResource(R.string.assignees_more, overflow),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

private const val MAX_BADGES = 3

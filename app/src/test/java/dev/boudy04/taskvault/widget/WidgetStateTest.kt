package dev.boudy04.taskvault.widget

import dev.boudy04.taskvault.data.TaskStatus
import dev.boudy04.taskvault.data.source.local.LocalTask
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetStateTest {

    private fun task(
        id: String,
        title: String = id,
        completed: Boolean = false,
        status: TaskStatus = TaskStatus.TODO,
        createdAt: String? = null,
        updatedAt: String? = null,
    ) = LocalTask(
        id = id,
        title = title,
        description = "",
        isCompleted = completed,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    @Test
    fun excludesDoneAndCompleted() {
        val state = listOf(
            task("1"),
            task("2", status = TaskStatus.IN_PROGRESS),
            task("3", status = TaskStatus.DONE),
            task("4", completed = true, status = TaskStatus.DONE),
            task("5", completed = true, status = TaskStatus.TODO),
        ).toWidgetState()
        assertEquals(2, state.openCount)
        assertEquals(listOf("1", "2"), state.titles)
    }

    @Test
    fun newestFirst_byUpdatedAtThenCreatedAt_nullsLast() {
        val state = listOf(
            task("old", updatedAt = "2024-01-01T00:00:00Z"),
            task("newest", updatedAt = "2025-06-01T00:00:00Z"),
            task("no-timestamps"),
            task("newer", updatedAt = "2025-01-01T00:00:00Z"),
        ).toWidgetState()
        assertEquals(4, state.openCount)
        assertEquals(listOf("newest", "newer", "old", "no-timestamps"), state.titles)
    }

    @Test
    fun titles_cappedAtFive_countIsFullOpenCount() {
        val tasks = (1..7).map { task(it.toString(), updatedAt = "2025-01-0${it}T00:00:00Z") }
        val state = tasks.toWidgetState()
        assertEquals(7, state.openCount)
        assertEquals(listOf("7", "6", "5", "4", "3"), state.titles)
    }

    @Test
    fun empty_whenNoTasks() {
        val state = emptyList<LocalTask>().toWidgetState()
        assertEquals(0, state.openCount)
        assertEquals(emptyList<String>(), state.titles)
    }
}

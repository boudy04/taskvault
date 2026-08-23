package dev.boudy04.taskvault.data

import dev.boudy04.taskvault.data.source.local.LocalTask
import dev.boudy04.taskvault.data.source.network.TaskDto
import dev.boudy04.taskvault.data.source.network.toDto
import dev.boudy04.taskvault.data.source.network.toLocal
import dev.boudy04.taskvault.sync.TaskPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskMappingExtTest {

    @Test
    fun `external to local preserves status and priority`() {
        val task = Task(
            title = "t", description = "d",
            status = TaskStatus.IN_PROGRESS, priority = TaskPriority.HIGH, id = "1",
        )
        val local = task.toLocal()
        assertEquals(TaskStatus.IN_PROGRESS, local.status)
        assertEquals(TaskPriority.HIGH, local.priority)
    }

    @Test
    fun `completed status round trip`() {
        val task = Task(title = "t", status = TaskStatus.DONE, id = "1")
        assertEquals(true, task.toLocal().toExternal().isCompleted)
        assertEquals(TaskStatus.DONE, task.toLocal().toExternal().status)
    }

    @Test
    fun `default task maps to active`() {
        val task = Task(id = "1")
        assertEquals(false, task.isCompleted)
        assertEquals(TaskStatus.TODO, task.toLocal().toExternal().status)
    }

    @Test
    fun `dueAt round trips external to local`() {
        val iso = "2026-08-24T09:00:00Z"
        val task = Task(title = "t", description = "d", dueAt = iso, id = "1")
        val local = task.toLocal()
        assertEquals(iso, local.dueAt)
        assertEquals(iso, local.toExternal().dueAt)
    }

    @Test
    fun `null dueAt stays null through mappings`() {
        val local = Task(id = "1").toLocal()
        assertNull(local.dueAt)
        assertNull(local.toExternal().dueAt)
    }

    @Test
    fun `dto maps due_at into local and back`() {
        val dto = TaskDto(id = 5, title = "t", description = "d", dueAt = "2026-08-24T09:00:00Z")
        val local = dto.toLocal()
        assertEquals("2026-08-24T09:00:00Z", local.dueAt)
        assertEquals("2026-08-24T09:00:00Z", local.copy(serverId = 5).let { it.toDto().dueAt })
    }

    @Test
    fun `copyFromDto threads due_at both set and cleared`() {
        val base = LocalTask(id = "l1", title = "old", description = "", isCompleted = false)
        val withDue = base.copyFromDto(TaskDto(dueAt = "2026-08-24T09:00:00Z"))
        assertEquals("2026-08-24T09:00:00Z", withDue.dueAt)
        val cleared = withDue.copyFromDto(TaskDto(dueAt = null))
        assertNull(cleared.dueAt)
    }

    @Test
    fun `payload dueAt flows into outgoing dto`() {
        val payload = TaskPayload(
            localId = "l1", title = "t", description = "",
            status = "todo", priority = "high", serverId = 3,
            dueAt = "2026-08-24T09:00:00Z",
        )
        assertEquals("2026-08-24T09:00:00Z", payload.toDtoWithoutServerId().dueAt)
    }
}

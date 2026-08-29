package dev.boudy04.taskvault.data

import dev.boudy04.taskvault.data.source.local.LocalTask
import dev.boudy04.taskvault.data.source.network.TaskDto
import dev.boudy04.taskvault.data.toDtoWithoutServerId
import dev.boudy04.taskvault.data.joinIds
import dev.boudy04.taskvault.data.joinTags
import dev.boudy04.taskvault.data.parseIds
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

    @Test
    fun `tags round trip dto local external`() {
        val dto = TaskDto(id = 5, title = "t", description = "d", tags = listOf("Work", "home"))
        val local = dto.toLocal()
        assertEquals("work,home", local.tags)
        assertEquals(listOf("work", "home"), local.toExternal().tags)
    }

    @Test
    fun `joinTags canonicalizes trim lowercase dedupe`() {
        assertEquals("work,home", joinTags(listOf(" Work ", "HOME", "work", "")))
    }

    @Test
    fun `copyFromDto threads tags both set and cleared`() {
        val base = LocalTask(id = "l1", title = "old", description = "", isCompleted = false)
        val tagged = base.copyFromDto(TaskDto(tags = listOf("Urgent")))
        assertEquals("urgent", tagged.tags)
        val cleared = tagged.copyFromDto(TaskDto())
        assertEquals("", cleared.tags)
    }

    @Test
    fun `payload tags flow into outgoing dto`() {
        val payload = TaskPayload(
            localId = "l1", title = "t", description = "",
            status = "todo", priority = "high", tags = listOf("a", "b"),
        )
        assertEquals(listOf("a", "b"), payload.toDtoWithoutServerId().tags)
    }

    @Test
    fun `assignees round trip dto local external`() {
        val dto = TaskDto(
            id = 5, title = "t", description = "d",
            assignees = listOf(
                dev.boudy04.taskvault.data.source.network.MemberDto(2, "bob"),
                dev.boudy04.taskvault.data.source.network.MemberDto(1, "alice"),
            ),
        )
        val local = dto.toLocal()
        assertEquals("2,1", local.assigneeIds)
        assertEquals(listOf(2, 1), local.toExternal().assigneeIds)
    }

    @Test
    fun `joinIds canonicalizes dedupe and drops invalid`() {
        assertEquals("1,3", joinIds(listOf(1, 1, 3, 0)))
        assertEquals("", joinIds(emptyList()))
        assertEquals(listOf(1, 3), parseIds("1,3"))
        assertEquals(emptyList<Int>(), parseIds(""))
    }

    @Test
    fun `copyFromDto threads assignees both set and cleared`() {
        val base = LocalTask(id = "l1", title = "old", description = "", isCompleted = false)
        val assigned = base.copyFromDto(
            TaskDto(assignees = listOf(dev.boudy04.taskvault.data.source.network.MemberDto(7, "carol")))
        )
        assertEquals("7", assigned.assigneeIds)
        val cleared = assigned.copyFromDto(TaskDto())
        assertEquals("", cleared.assigneeIds)
    }

    @Test
    fun `payload assigneeIds flow into outgoing dto`() {
        val payload = TaskPayload(
            localId = "l1", title = "t", description = "",
            status = "todo", priority = "high",
            assigneeIds = listOf(4, 5),
        )
        assertEquals(listOf(4, 5), payload.toDtoWithoutServerId().assigneeIds)
    }

    @Test
    fun `local in_progress row with isCompleted false reads back as TODO`() {
        // Pins CURRENT behavior (refactor gate): toExternal() derives status from the
        // legacy isCompleted flag only, so a stored IN_PROGRESS row surfaces as TODO.
        val local = LocalTask(
            id = "1", title = "t", description = "d",
            isCompleted = false, status = TaskStatus.IN_PROGRESS,
        )
        assertEquals(TaskStatus.TODO, local.toExternal().status)
    }
}

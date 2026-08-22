package dev.boudy04.taskvault.data

import org.junit.Assert.assertEquals
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
}

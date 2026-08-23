package dev.boudy04.taskvault.sync

/** Schedules exact-time reminder notifications for a task's due moment. */
interface ReminderScheduler {
    fun schedule(localId: String, title: String, dueAtMillis: Long)
    fun cancel(localId: String)
}

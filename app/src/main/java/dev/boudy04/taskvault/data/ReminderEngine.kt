package dev.boudy04.taskvault.data

import dev.boudy04.taskvault.sync.ReminderScheduler
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the reminder policy (refactor Step 4): parse the stored ISO due instant, decide
 * arm-vs-skip, and delegate alarm mechanics to [ReminderScheduler]. [Clock] is injected so
 * characterization tests control "now". Consolidates the logic previously duplicated between
 * DefaultTaskRepository.scheduleReminder and ReminderBootReceiver.
 */
@Singleton
class ReminderEngine @Inject constructor(
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
) {

    /** Arms the reminder when the stored due ISO parses to a future instant; no-op otherwise. */
    fun onDueAtSet(localId: String, title: String, dueAt: String?) {
        if (dueAt == null) return
        val millis = runCatching { Instant.parse(dueAt).toEpochMilli() }.getOrNull() ?: return
        if (millis > clock.millis()) {
            scheduler.schedule(localId, title, millis)
        }
    }

    /** Drops any armed alarm for the row (edit, completion, or delete). */
    fun onCancel(localId: String) {
        scheduler.cancel(localId)
    }
}

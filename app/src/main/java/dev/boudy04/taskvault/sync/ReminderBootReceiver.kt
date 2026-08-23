package dev.boudy04.taskvault.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.boudy04.taskvault.data.source.local.TaskDao
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms reminders for all future-due rows after a reboot (alarms don't survive it).
 * ponytail: unscoped scope + goAsync is fine here; the DAO read is one small query.
 */
@AndroidEntryPoint
class ReminderBootReceiver : BroadcastReceiver() {

    @Inject lateinit var tasks: TaskDao
    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val now = System.currentTimeMillis()
                tasks.getDueTasks().forEach { task ->
                    val millis = task.dueAt
                        ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                        ?: return@forEach
                    if (millis > now) {
                        scheduler.schedule(task.id, task.title, millis)
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}

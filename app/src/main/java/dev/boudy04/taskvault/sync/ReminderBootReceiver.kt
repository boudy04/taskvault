package dev.boudy04.taskvault.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.boudy04.taskvault.data.ReminderEngine
import dev.boudy04.taskvault.data.source.local.TaskDao
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms reminders for all future-due rows after a reboot (alarms don't survive it).
 * ponytail: unscoped scope + goAsync is fine here; the DAO read is one small query.
 * Arm/skip decisions (parse, future-check) live in [ReminderEngine], not here.
 */
@AndroidEntryPoint
class ReminderBootReceiver : BroadcastReceiver() {

    @Inject lateinit var tasks: TaskDao
    @Inject lateinit var reminderEngine: ReminderEngine

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                tasks.getDueTasks().forEach { task ->
                    reminderEngine.onDueAtSet(task.id, task.title, task.dueAt)
                }
            } finally {
                result.finish()
            }
        }
    }
}

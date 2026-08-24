package dev.boudy04.taskvault.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fires a reminder notification at the due moment: exact alarm when the OS allows it,
 * WorkManager one-time work as the fallback (API 31+ without SCHEDULE_EXACT_ALARM grant).
 */
@Singleton
class AlarmReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val workManager: WorkManager,
) : ReminderScheduler {

    override fun schedule(localId: String, title: String, dueAtMillis: Long) {
        val delay = dueAtMillis - System.currentTimeMillis()
        if (delay <= 0) return
        if (android.os.Build.VERSION.SDK_INT >= 23 && canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                dueAtMillis,
                alarmIntent(localId, title),
            )
        } else {
            workManager.enqueueUniqueWork(
                "reminder_$localId",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(
                        workDataOf(
                            ReminderWorker.KEY_LOCAL_ID to localId,
                            ReminderWorker.KEY_TITLE to title,
                        ),
                    )
                    .build(),
            )
        }
    }

    override fun cancel(localId: String) {
        alarmManager.cancel(alarmIntent(localId, ""))
        workManager.cancelUniqueWork("reminder_$localId")
    }

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    // requestCode ties the PendingIntent to this task; extras don't affect matching.
    private fun alarmIntent(localId: String, title: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            localId.hashCode(),
            Intent(context, ReminderReceiver::class.java)
                .putExtra(ReminderReceiver.EXTRA_LOCAL_ID, localId)
                .putExtra(ReminderReceiver.EXTRA_TITLE, title),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}


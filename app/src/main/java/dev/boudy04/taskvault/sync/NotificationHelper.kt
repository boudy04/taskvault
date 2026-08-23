package dev.boudy04.taskvault.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.TodoActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the sticky "can't reach the server" notification while a sync keeps failing on
 * connectivity. Denied POST_NOTIFICATIONS (or notifications disabled) makes this a no-op;
 * the in-app unsynced dot remains the fallback signal.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // ponytail: string-lookup getSystemService works on all APIs; Class variant needs 23
    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showConnectivityFailure(baseUrl: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N ||
            !manager.areNotificationsEnabled()
        ) {
            return
        }
        ensureChannel()
        val pending = PendingIntent.getActivity(
            context,
            0,
            Intent(context, TodoActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_connectivity_title))
            .setContentText(context.getString(R.string.notification_connectivity_text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun clearConnectivityFailure() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /** Silent "task due" shade notification; tapping it opens the app. */
    fun showReminder(localId: String, taskTitle: String) {
        if (!manager.areNotificationsEnabled()) return
        ensureReminderChannel()
        val pending = PendingIntent.getActivity(
            context,
            localId.hashCode(),
            Intent(context, TodoActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_task_due))
            .setContentText(taskTitle)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(localId.hashCode(), notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_sync_status),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun ensureReminderChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(REMINDERS_CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                REMINDERS_CHANNEL_ID,
                context.getString(R.string.notification_channel_reminders),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private companion object {
        const val CHANNEL_ID = "sync_status_quiet"
        const val NOTIFICATION_ID = 1001
        const val REMINDERS_CHANNEL_ID = "reminders"
    }
}


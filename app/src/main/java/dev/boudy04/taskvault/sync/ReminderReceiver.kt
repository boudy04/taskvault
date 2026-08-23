package dev.boudy04.taskvault.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Fired by [AlarmReminderScheduler] at the due moment; posts the shade notification. */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var notifications: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val localId = intent.getStringExtra(EXTRA_LOCAL_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        notifications.showReminder(localId, title)
    }

    companion object {
        const val EXTRA_LOCAL_ID = "localId"
        const val EXTRA_TITLE = "title"
    }
}

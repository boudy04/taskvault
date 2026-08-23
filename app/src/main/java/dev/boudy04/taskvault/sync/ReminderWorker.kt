package dev.boudy04.taskvault.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** WorkManager fallback when exact alarms aren't permitted (API 31+). */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val notifications: NotificationHelper,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val localId = inputData.getString(KEY_LOCAL_ID) ?: return Result.failure()
        notifications.showReminder(localId, inputData.getString(KEY_TITLE).orEmpty())
        return Result.success()
    }

    companion object {
        const val KEY_LOCAL_ID = "localId"
        const val KEY_TITLE = "title"
    }
}

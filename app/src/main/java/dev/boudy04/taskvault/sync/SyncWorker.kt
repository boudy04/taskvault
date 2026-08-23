package dev.boudy04.taskvault.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker
import dev.boudy04.taskvault.settings.SettingsRepository
import dev.boudy04.taskvault.widget.WidgetUpdater

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val engine: SyncEngine,
    private val widgetUpdater: WidgetUpdater,
    private val settings: SettingsRepository,
    private val notifications: NotificationHelper,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = when (val outcome = engine.run()) {
        SyncOutcome.SUCCESS -> {
            // Clears even when the queue was empty; a pull-only success also counts.
            notifications.clearConnectivityFailure()
            widgetUpdater.update()
            Result.success()
        }
        SyncOutcome.CONNECTIVITY_RETRY -> {
            notifications.showConnectivityFailure(settings.current().baseUrl)
            Result.retry()
        }
        SyncOutcome.RETRY -> Result.retry()
        SyncOutcome.FAILURE -> {
            // 401 stays un-notified on purpose; the in-app dot badge covers auth.
            notifications.clearConnectivityFailure()
            Result.failure()
        }
    }
}

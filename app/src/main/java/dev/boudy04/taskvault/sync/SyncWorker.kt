package dev.boudy04.taskvault.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val engine: SyncEngine,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = when (engine.run()) {
        SyncOutcome.SUCCESS -> Result.success()
        SyncOutcome.RETRY -> Result.retry()
        SyncOutcome.FAILURE -> Result.failure()
    }
}

package dev.boudy04.taskvault.sync

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow

enum class SyncOutcome { SUCCESS, RETRY, CONNECTIVITY_RETRY, FAILURE }

/**
 * One-shot signal that the server rejected a queued write with 403
 * ("Members have view-only access"); the task list surfaces it as a snackbar.
 */
@Singleton
class ViewOnlyRejections @Inject constructor() {
    val events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun signal() {
        events.tryEmit(Unit)
    }
}

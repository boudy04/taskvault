package dev.boudy04.taskvault.data

/**
 * Snapshot of how much of the local task store is mirrored on the server.
 *
 * @param total all tasks known locally
 * @param synced tasks with no pending operations (safe online / will pull cleanly)
 * @param queued tasks waiting in the offline queue to be pushed
 */
data class SyncStats(
    val total: Int,
    val synced: Int,
    val queued: Int,
) {
    val syncedPercent: Int
        get() = if (total == 0) 100 else (synced * 100) / total
}

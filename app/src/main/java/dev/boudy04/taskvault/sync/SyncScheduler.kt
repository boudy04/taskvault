package dev.boudy04.taskvault.sync

fun interface SyncScheduler {
    /** Requests a unique sync run; safe to call from anywhere. */
    fun requestSync()
}

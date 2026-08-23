package dev.boudy04.taskvault.sync

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dev.boudy04.taskvault.di.ApplicationScope
import dev.boudy04.taskvault.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches device connectivity so the offline notification reflects reality even when
 * no sync happens to be running (idle app + killed internet previously stayed silent).
 *
 * Lost validated internet -> sticky "can't reach server" notification.
 * Regained -> clear it and kick a sync so fresh data pulls immediately.
 */
@Singleton
class ConnectivityWatcher @Inject constructor(
    private val connectivityManager: ConnectivityManager,
    private val notifications: NotificationHelper,
    private val settings: SettingsRepository,
    private val syncScheduler: SyncScheduler,
    @ApplicationScope private val scope: CoroutineScope,
) {

    fun start() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    scope.launch {
                        notifications.showConnectivityFailure(settings.current().baseUrl)
                    }
                }

                override fun onAvailable(network: Network) {
                    notifications.clearConnectivityFailure()
                    syncScheduler.requestSync()
                }
            },
        )
    }
}

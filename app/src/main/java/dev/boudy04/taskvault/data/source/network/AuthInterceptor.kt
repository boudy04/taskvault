package dev.boudy04.taskvault.data.source.network

import dev.boudy04.taskvault.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val settings: SettingsRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // ponytail: blocking first-read warms DataStore cache; subsequent reads are in-memory
        val token = runBlocking { settings.current().token }
        return chain.proceed(
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build(),
        )
    }
}

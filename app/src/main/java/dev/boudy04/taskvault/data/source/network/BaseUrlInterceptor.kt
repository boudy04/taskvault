package dev.boudy04.taskvault.data.source.network

import dev.boudy04.taskvault.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class BaseUrlInterceptor @Inject constructor(
    private val settings: SettingsRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val target = runBlocking { settings.current().baseUrl }.toHttpUrlOrNull()
            ?: return chain.proceed(chain.request())
        val rewritten = chain.request().url.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .build()
        return chain.proceed(chain.request().newBuilder().url(rewritten).build())
    }
}

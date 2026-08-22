package dev.boudy04.taskvault.data.source.network

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class RetrofitNetworkDataSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: TaskApiService

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS).build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TaskApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun createTask_postsJsonAndParsesResponse() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"id":7,"title":"A","description":"B","status":"todo",""" +
                    """"priority":"high","created_at":"2026-01-01T00:00:00Z",""" +
                    """"updated_at":"2026-01-01T00:00:00Z"}""",
            ),
        )
        val created = api.createTask(TaskDto(title = "A", description = "B", priority = "high"))
        assertEquals(7, created.id)
        val recorded = server.takeRequest()
        assertEquals("/api/tasks", recorded.path)
        assertEquals("POST", recorded.method)
        assertNull(recorded.getHeader("Authorization")) // no interceptor in this raw stack
    }

    @Test
    fun deleteTask_sendsDeleteAndHandles204() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val resp = api.deleteTask(3)
        assertEquals(204, resp.code())
        assertEquals("/api/tasks/3", server.takeRequest().path)
    }

    @Test
    fun listTasks_parsesSnakeCaseTimestamps() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """[{"id":1,"title":"T","description":"","status":"in_progress",""" +
                    """"priority":"low","created_at":"c","updated_at":"u"}]""",
            ),
        )
        val list = api.listTasks(null)
        assertEquals("in_progress", list.single().status)
        assertEquals("u", list.single().updatedAt)
    }
}

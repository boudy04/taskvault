package dev.boudy04.taskvault.data.source.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface TaskApiService {

    @POST("api/auth/register")
    suspend fun register(@Body body: AuthRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse
    @GET("api/tasks")
    suspend fun listTasks(@Query("status") status: String? = null): List<TaskDto>

    @GET("api/tasks/{id}")
    suspend fun getTask(@Path("id") id: Int): TaskDto

    @POST("api/tasks")
    suspend fun createTask(@Body task: TaskDto): TaskDto

    @PUT("api/tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body task: TaskDto): TaskDto

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<Unit>
}

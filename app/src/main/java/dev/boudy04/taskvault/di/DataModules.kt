/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.boudy04.taskvault.di

import android.app.AlarmManager
import android.content.Context
import android.net.ConnectivityManager
import androidx.room.Room
import androidx.work.WorkManager
import dev.boudy04.taskvault.BuildConfig
import dev.boudy04.taskvault.data.DefaultTaskRepository
import dev.boudy04.taskvault.data.TaskRepository
import dev.boudy04.taskvault.data.source.local.PendingOpDao
import dev.boudy04.taskvault.data.source.local.TaskDao
import dev.boudy04.taskvault.data.source.local.ToDoDatabase
import dev.boudy04.taskvault.data.source.network.AuthInterceptor
import dev.boudy04.taskvault.data.source.network.BaseUrlInterceptor
import dev.boudy04.taskvault.data.source.network.TaskApiService
import dev.boudy04.taskvault.sync.AlarmReminderScheduler
import dev.boudy04.taskvault.sync.ReminderScheduler
import dev.boudy04.taskvault.sync.SyncScheduler
import dev.boudy04.taskvault.sync.WorkManagerSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindTaskRepository(repository: DefaultTaskRepository): TaskRepository

    @Singleton
    @Binds
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    @Singleton
    @Binds
    abstract fun bindReminderScheduler(impl: AlarmReminderScheduler): ReminderScheduler
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDataBase(@ApplicationContext context: Context): ToDoDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ToDoDatabase::class.java,
            "Tasks.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideTaskDao(database: ToDoDatabase): TaskDao = database.taskDao()

    @Provides
    fun providePendingOpDao(database: ToDoDatabase): PendingOpDao = database.pendingOpDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttp(
        auth: AuthInterceptor,
        baseUrl: BaseUrlInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(baseUrl)
        .addInterceptor(auth)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        // Server returns null for optional fields (e.g. description); coerce
        // those nulls to the Kotlin defaults instead of failing the parse.
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl("http://localhost/") // rewritten per-request by BaseUrlInterceptor
        .client(client)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType()),
        )
        .build()

    @Provides
    @Singleton
    fun provideTaskApi(retrofit: Retrofit): TaskApiService = retrofit.create(TaskApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    @Singleton
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
}

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    /** Epoch-millis source, identical to the previous direct System.currentTimeMillis() reads. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}

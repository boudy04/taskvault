# TaskVault Offline-First Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Ship `boudy04/taskvault` — a rebranded Kotlin/Compose Android client that consumes the deployed FastAPI task-api through Retrofit with a full offline-first mutation queue (Room source of truth + WorkManager sync).

**Architecture:** Clone of Google's `android/architecture-samples`, refactored to package `dev.boudy04.taskvault`. Room stays the single source of truth; every mutation writes locally and enqueues a `PendingOpEntity`; a HiltWorker drains the queue through Retrofit (Bearer auth), then pulls and reconciles the server list. Settings screen stores server URL + token in DataStore.

**Tech Stack:** Kotlin 2.1.10, Compose (BOM 2024.12.01), Room 2.6.1, Hilt 2.53.1, WorkManager 2.10.0, Retrofit 2.9.0 + kotlinx-serialization converter, OkHttp 4.10.0, DataStore Preferences 1.1.1, JUnit4/Truth/Turbine, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-08-22-taskvault-offline-client-design.md` (this plan argues from the spec; executors read both)

## Global Constraints

- JDK 17 required (`./gradlew` fails otherwise). Windows host: run gradle via `.\gradlew`.
- Package/namespace/applicationId: `dev.boudy04.taskvault` everywhere (no `com.example.*` remnants).
- Upstream has NO product flavors despite its README claiming mock/prod — single variant; CI runs `testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest`. *(Deviation from spec §3 noted here.)*
- The old fake `NetworkDataSource` wholesale-sync is deleted; remote access lives in `TaskApiService` used only by `SyncEngine`. *(Refines spec §2.)*
- `LocalTask` keeps its legacy `isCompleted` column alongside `status` (invariant maintained only in mappers/DAO writes).
- API contract (fixed): base `https://prject-cv-production.up.railway.app`; `Authorization: Bearer <token>`; JSON fields snake_case (`created_at`, `updated_at`); `status ∈ todo|in_progress|done`; `priority ∈ low|medium|high`.
- Default settings: baseUrl = Railway URL above, token = `dev-token`.
- DB version bumps once (Task 5); `fallbackToDestructiveMigration()` acceptable (no released users).
- Apache-2.0 LICENSE file must remain verbatim; credit line to `android/architecture-samples` lands in README (Task 11).
- Commit style: conventional commits (`feat:`, `test:`, `chore:` …), commit after every green verification.
- Never commit real tokens; defaults are demo values.

---

### Task 1: Bootstrap repo from upstream

**Files:**
- Create: `D:\app\prject cv\p4\` (repo root), `p4/docs/superpowers/{specs,plans}/`

**Interfaces:**
- Produces: git repo at `p4` whose history starts with upstream baseline import.

- [x] **Step 1: Clone and detach**

```powershell
git clone https://github.com/android/architecture-samples.git p4
Remove-Item -Recurse -Force p4\.git
git -C p4 init
git -C p4 checkout -b main
```

- [x] **Step 2: Move spec + plan into the repo**

```powershell
New-Item -ItemType Directory -Force p4\docs\superpowers\specs, p4\docs\superpowers\plans | Out-Null
Copy-Item "docs\superpowers\specs\2026-08-22-taskvault-offline-client-design.md" p4\docs\superpowers\specs\
Copy-Item "docs\superpowers\plans\2026-08-22-taskvault-offline-client.md" p4\docs\superpowers\plans\
```

(The plan file itself is copied after it exists — if executing this plan from the workspace root, re-run Step 2's Copy-Item lines.)

- [x] **Step 3: Verify toolchain builds baseline**

Run (from `p4`): `.\gradlew help --quiet`
Expected: BUILD SUCCESSFUL. If JDK mismatch, install JDK 17 and set `JAVA_HOME`.

- [x] **Step 4: Commit baseline**

```bash
git add -A && git commit -m "chore: import android/architecture-samples baseline"
```

### Task 2: Rebrand package and identity

**Files:**
- Modify: all `*.kt` under `app/src/{main,test,androidTest}`, `shared-test/src/**`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values*/strings.xml`
- Rename: directory `app/src/main/java/com/example/android/architecture/blueprints/todoapp` → `app/src/main/java/dev/boudy04/taskvault` (same pattern for test/androidTest/shared-test source sets)

**Interfaces:**
- Consumes: baseline repo from Task 1.
- Produces: every Kotlin file declares `package dev.boudy04.taskvault...`; build coordinates updated.

- [x] **Step 1: Move source trees**

```powershell
$sets = @{ "main"="java"; "test"="java"; "androidTest"="java" }
git mv app/src/main/java/com/example/android/architecture/blueprints/todoapp app/src/main/java/dev/boudy04/taskvault
git mv app/src/test/java/com/example/android/architecture/blueprints/todoapp app/src/test/java/dev/boudy04/taskvault
git mv app/src/androidTest/java/com/example/android/architecture/blueprints/todoapp app/src/androidTest/java/dev/boudy04/taskvault
git mv shared-test/src/main/java/com/example/android/architecture/blueprints/todoapp shared-test/src/main/java/dev/boudy04/taskvault
Remove-Item -Recurse -Force app/src/main/java/com/example, app/src/test/java/com/example, app/src/androidTest/java/com/example, shared-test/src/main/java/com/example
```

- [x] **Step 2: Rewrite package/import references**

```powershell
Get-ChildItem -Recurse -Include *.kt,*.kts,*.xml -Path app, shared-test |
  ForEach-Object {
    $c = Get-Content -Raw $_.FullName
    $n = $c -replace 'com\.example\.android\.architecture\.blueprints\.todoapp','dev.boudy04.taskvault'
    if ($c -ne $n) { Set-Content -NoNewline $_.FullName $n }
  }
```

- [x] **Step 3: Update gradle coordinates**

In `app/build.gradle.kts`:

```kotlin
namespace = "dev.boudy04.taskvault"
applicationId = "dev.boudy04.taskvault"
testInstrumentationRunner = "dev.boudy04.taskvault.CustomTestRunner"
```

- [x] **Step 4: App display name**

`app/src/main/res/values/strings.xml`: `<string name="app_name">TaskVault</string>` (repeat in any `values-*` variants present).

- [x] **Step 5: Verify**

Run: `.\gradlew assembleDebug testDebugUnitTest --quiet`
Expected: BUILD SUCCESSFUL, all upstream unit tests pass.

- [x] **Step 6: Commit**

```bash
git add -A && git commit -m "chore: rebrand to dev.boudy04.taskvault / TaskVault"
```

### Task 3: Dependencies and WorkManager runtime

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/java/dev/boudy04/taskvault/TodoApplication.kt` (rename to `TaskVaultApplication.kt`), `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: catalog aliases `retrofit-core`, `retrofit-kotlinx-serialization-converter`, `okhttp-logging-interceptor`, `okhttp-mockwebserver`, `robolectric` unused; app compiles with serialization plugin; WorkManager resolves workers via Hilt factory.

- [x] **Step 1: Catalog entries**

Append to `[libraries]` in `gradle/libs.versions.toml`:

```toml
retrofit-core = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization-converter = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofitKotlinxSerializationJson" }
okhttp-logging-interceptor = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
okhttp-mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "okhttp" }
```

(`androidx-work-ktx`, `androidx-work-testing`, `hilt-ext-work`, `hilt-ext-compiler`, `androidx-dataStore-preferences`, `kotlin-serialization` plugin already exist in this catalog.)

- [x] **Step 2: App module wiring**

In `app/build.gradle.kts` plugins block add:

```kotlin
alias(libs.plugins.kotlin.serialization)
```

In dependencies add:

```kotlin
implementation(libs.retrofit.core)
implementation(libs.retrofit.kotlinx.serialization.converter)
implementation(libs.okhttp.logging.interceptor)
implementation(libs.androidx.work.ktx)
implementation(libs.hilt.ext.work)
ksp(libs.hilt.ext.compiler)
implementation(libs.androidx.dataStore.preferences)
testImplementation(libs.okhttp.mockwebserver)
testImplementation(libs.androidx.work.testing)
```

Note: catalog alias `retrofit-kotlinx-serialization-converter` maps to accessor `libs.retrofit.kotlinx.serialization.converter`.

- [x] **Step 3: Application class on-demand WorkManager init**

Rename `TodoApplication.kt` → `TaskVaultApplication.kt`; manifest `android:name` updated to match:

```kotlin
@HiltAndroidApp
class TaskVaultApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

- [x] **Step 4: Remove default WorkManager initializer**

Inside `<application>` in `AndroidManifest.xml` add:

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    tools:node="merge">
    <meta-data
        android:name="androidx.work.WorkManagerInitializer"
        tools:node="remove" />
</provider>
```

(add `xmlns:tools="http://schemas.android.com/tools"` to `<manifest>` if missing)

- [x] **Step 5: Verify**

Run: `.\gradlew assembleDebug --quiet`
Expected: BUILD SUCCESSFUL.

- [x] **Step 6: Commit**

```bash
git add -A && git commit -m "chore: add retrofit/workmanager/datastore deps and hilt worker init"
```

### Task 4: Domain model extension (status + priority)

**Files:**
- Create: `app/src/main/java/dev/boudy04/taskvault/data/TaskStatus.kt`, `data/TaskPriority.kt`
- Modify: `data/Task.kt`, `data/ModelMappingExt.kt`, `data/source/network/{NetworkTask.kt, TaskNetworkDataSource.kt}` (compile-fix only; both deleted in Task 7)
- Test: `app/src/test/java/dev/boudy04/taskvault/data/TaskMappingExtTest.kt` (new)

**Interfaces:**
- Produces: `enum class TaskStatus { TODO, IN_PROGRESS, DONE }`, `enum class TaskPriority { LOW, MEDIUM, HIGH }`; domain `Task(status: TaskStatus = TaskStatus.TODO, priority: TaskPriority = TaskPriority.MEDIUM)` with computed `isCompleted`.

- [x] **Step 1: Write failing mapper tests**

`app/src/test/java/dev/boudy04/taskvault/data/TaskMappingExtTest.kt`:

```kotlin
package dev.boudy04.taskvault.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskMappingExtTest {

    @Test
    fun `external to local preserves status and priority`() {
        val task = Task(
            title = "t", description = "d",
            status = TaskStatus.IN_PROGRESS, priority = TaskPriority.HIGH, id = "1",
        )
        val local = task.toLocal()
        assertEquals(TaskStatus.IN_PROGRESS, local.status)
        assertEquals(TaskPriority.HIGH, local.priority)
    }

    @Test
    fun `completed status round trip`() {
        val task = Task(title = "t", status = TaskStatus.DONE, id = "1")
        assertEquals(true, task.toLocal().toExternal().isCompleted)
        assertEquals(TaskStatus.DONE, task.toLocal().toExternal().status)
    }

    @Test
    fun `default task maps to active`() {
        val task = Task(id = "1")
        assertEquals(false, task.isCompleted)
        assertEquals(TaskStatus.TODO, task.toLocal().toExternal().status)
    }
}
```

- [x] **Step 2: Verify compile failure**

Run: `.\gradlew :app:compileDebugUnitTestKotlin --quiet`
Expected: FAIL — `TaskStatus`/`priority` unresolved, `Task` has no such parameters.

- [x] **Step 3: Implement enums + Task refactor**

`data/TaskStatus.kt`:

```kotlin
package dev.boudy04.taskvault.data

enum class TaskStatus { TODO, IN_PROGRESS, DONE }
```

`data/TaskPriority.kt`:

```kotlin
package dev.boudy04.taskvault.data

enum class TaskPriority { LOW, MEDIUM, HIGH }
```

`data/Task.kt` body replaced with:

```kotlin
data class Task(
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val id: String,
) {
    val titleForList: String
        get() = if (title.isNotEmpty()) title else description

    val isActive: Boolean
        get() = status != TaskStatus.DONE

    val isCompleted: Boolean
        get() = status == TaskStatus.DONE

    val isEmpty: Boolean
        get() = title.isEmpty() || description.isEmpty()
}
```

- [x] **Step 4: Fix all construction sites**

Find them:

```powershell
Select-String -Path (Get-ChildItem -Recurse -Filter *.kt | % FullName) -Pattern 'Task\(' -CaseSensitive:$false
Select-String -Path (Get-ChildItem -Recurse -Filter *.kt | % FullName) -Pattern 'isCompleted\s*='
```

Mechanical rule: `Task(..., isCompleted = true, ...)` → `status = TaskStatus.DONE`; `isCompleted = false` → omit (defaults TODO). Update `ModelMappingExt.kt` external↔local pairs:

```kotlin
// External to local
fun Task.toLocal() = LocalTask(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
)

// Local to External
fun LocalTask.toExternal() = Task(
    id = id,
    title = title,
    description = description,
    status = if (isCompleted) TaskStatus.DONE else TaskStatus.TODO,
)
```

Leave network mappings compiling by treating upstream `network.TaskStatus.COMPLETE` as DONE (they are deleted in Task 7):

```kotlin
// Network to Local
fun NetworkTask.toLocal() = LocalTask(
    id = id,
    title = title,
    description = shortDescription,
    isCompleted = (status == com.example.placeholder.TaskStatus.COMPLETE),
)
```

…adjusting to whatever keeps `shared-test`'s fakes compiling; expect small edits in `FakeDataSource` and repository/viewmodel tests constructing `Task(isCompleted = …)`.

- [x] **Step 5: Verify green**

Run: `.\gradlew testDebugUnitTest --quiet`
Expected: PASS including the three new tests.

- [x] **Step 6: Commit**

```bash
git add -A && git commit -m "feat: extend domain Task with status and priority"
```

### Task 5: Room schema v2 + pending-op queue tables

**Files:**
- Modify: `data/source/local/{LocalTask.kt, TaskDao.kt, TodoDatabase.kt}`
- Create: `data/source/local/Converters.kt`, `data/source/local/PendingOpEntity.kt`, `data/source/local/PendingOpDao.kt`
- Test: `app/src/androidTest/java/dev/boudy04/taskvault/data/source/local/PendingOpDaoTest.kt` (new)

**Interfaces:**
- Consumes: `TaskStatus`/`TaskPriority` from Task 4.
- Produces: `LocalTask(id,title,description,isCompleted,status,priority,serverId:Int?,createdAt:String?,updatedAt:String?)`; `PendingOpEntity(opId:Long, taskLocalId:String, opType:PendingOpType, payload:String, state:PendingOpState, attempts:Int, enqueuedAt:Long)`; `PendingOpDao` methods used by Tasks 8–9: `insert`, `nextPending()`, `updateState(opId,state)`, `deleteByIds(List<Long>)`, `getAll()`, `clearForTask(String)`, `countPending():Int`, `observePendingTaskIds(): Flow<List<String>>`; `TaskDao.getByServerId(Int): LocalTask?`, `TaskDao.getCompleted(): List<LocalTask>`; DB version bumped once with destructive fallback.

- [x] **Step 1: Write failing DAO instrumented test**

`PendingOpDaoTest.kt`:

```kotlin
package dev.boudy04.taskvault.data.source.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingOpDaoTest {

    private lateinit var db: TodoDatabase
    private lateinit var dao: PendingOpDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TodoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.pendingOpDao()
    }

    @Test
    fun nextPending_returnsOldestFirst() = runTest {
        dao.insert(op(enqueuedAt = 20))
        dao.insert(op(enqueuedAt = 10))
        assertEquals(10L, dao.nextPending()?.enqueuedAt)
    }

    @Test
    fun runningOpsAreSkipped_pendingCountReflectsQueue() = runTest {
        val a = dao.insert(op(enqueuedAt = 1))
        dao.updateState(a, PendingOpState.RUNNING)
        dao.insert(op(enqueuedAt = 2))
        assertEquals(1, dao.countPending())
        assertEquals(listOf("t2"), dao.observePendingTaskIds().first())
    }

    @Test
    fun deleteByIdsRemovesOps() = runTest {
        val a = dao.insert(op(enqueuedAt = 1))
        dao.deleteByIds(listOf(a))
        assertEquals(null, dao.nextPending())
    }

    private fun op(enqueuedAt: Long) = PendingOpEntity(
        taskLocalId = "t${enqueuedAt}", opType = PendingOpType.CREATE,
        payload = "{}", enqueuedAt = enqueuedAt,
    )
}
```

- [x] **Step 2: Verify red**

Run: `.\gradlew :app:compileDebugAndroidTestKotlin --quiet`
Expected: FAIL — types unresolved.

- [x] **Step 3: Implement entities, converters, DAOs**

`data/source/local/Converters.kt`:

```kotlin
package dev.boudy04.taskvault.data.source.local

import androidx.room.TypeConverter
import dev.boudy04.taskvault.data.TaskPriority
import dev.boudy04.taskvault.data.TaskStatus

class Converters {
    @TypeConverter fun statusToString(s: TaskStatus): String = s.name
    @TypeConverter fun stringToStatus(v: String): TaskStatus = TaskStatus.valueOf(v)
    @TypeConverter fun priorityToString(p: TaskPriority): String = p.name
    @TypeConverter fun stringToPriority(v: String): TaskPriority = TaskPriority.valueOf(v)
    @TypeConverter fun opTypeToString(o: PendingOpType): String = o.name
    @TypeConverter fun stringToOpType(v: String): PendingOpType = PendingOpType.valueOf(v)
    @TypeConverter fun opStateToString(s: PendingOpState): String = s.name
    @TypeConverter fun stringToOpState(v: String): PendingOpState = PendingOpState.valueOf(v)
}
```

`data/source/local/PendingOpEntity.kt`:

```kotlin
package dev.boudy04.taskvault.data.source.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PendingOpType { CREATE, UPDATE, DELETE }
enum class PendingOpState { PENDING, RUNNING }

@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true) val opId: Long = 0,
    val taskLocalId: String,
    val opType: PendingOpType,
    val payload: String,
    val state: PendingOpState = PendingOpState.PENDING,
    val attempts: Int = 0,
    val enqueuedAt: Long = System.currentTimeMillis(),
)
```

`data/source/local/PendingOpDao.kt`:

```kotlin
package dev.boudy04.taskvault.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOpDao {

    @Insert
    suspend fun insert(op: PendingOpEntity): Long

    @Query(
        "SELECT * FROM pending_ops WHERE state = 'PENDING' " +
            "ORDER BY enqueuedAt ASC, opId ASC LIMIT 1"
    )
    suspend fun nextPending(): PendingOpEntity?

    @Query("UPDATE pending_ops SET state = :state, attempts = attempts + 1 WHERE opId = :opId")
    suspend fun updateState(opId: Long, state: PendingOpState)

    @Query("DELETE FROM pending_ops WHERE opId IN (:opIds)")
    suspend fun deleteByIds(opIds: List<Long>)

    @Query("SELECT * FROM pending_ops ORDER BY enqueuedAt ASC")
    suspend fun getAll(): List<PendingOpEntity>

    @Query("DELETE FROM pending_ops WHERE taskLocalId = :taskLocalId")
    suspend fun clearForTask(taskLocalId: String)

    @Query("SELECT COUNT(*) FROM pending_ops WHERE state = 'PENDING'")
    suspend fun countPending(): Int

    @Query("SELECT DISTINCT taskLocalId FROM pending_ops WHERE state = 'PENDING'")
    fun observePendingTaskIds(): Flow<List<String>>
}
```

`LocalTask.kt` gains fields (keep legacy column):

```kotlin
@Entity(tableName = "task")
data class LocalTask(
    @PrimaryKey val id: String,
    var title: String,
    var description: String,
    var isCompleted: Boolean,
    var status: TaskStatus = TaskStatus.TODO,
    var priority: TaskPriority = TaskPriority.MEDIUM,
    var serverId: Int? = null,
    var createdAt: String? = null,
    var updatedAt: String? = null,
)
```

`TaskDao.kt` additions:

```kotlin
@Query("SELECT * FROM task WHERE serverId = :serverId LIMIT 1")
suspend fun getByServerId(serverId: Int): LocalTask?

@Query("SELECT * FROM task WHERE isCompleted = 1")
suspend fun getCompleted(): List<LocalTask>

@Query("DELETE FROM task WHERE serverId IS NULL AND id NOT IN (SELECT DISTINCT taskLocalId FROM pending_ops)")
suspend fun deleteUnsyncedOrphans(): Int
```

`TodoDatabase.kt`: `version` bumped by 1, entities list gains `PendingOpEntity::class`, annotate `@TypeConverters(Converters::class)`, add `abstract fun pendingOpDao(): PendingOpDao`. Where the database is built (search `databaseBuilder`), append `.fallbackToDestructiveMigration()`.

- [x] **Step 4: Verify green (device/emulator)**

Run: `.\gradlew connectedDebugAndroidTest --quiet` (or defer execution to CI emulator job; compile check locally:)
Run: `.\gradlew :app:compileDebugAndroidTestKotlin assembleDebug --quiet`
Expected: PASS / BUILD SUCCESSFUL.

- [x] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: room schema v2 with offline pending-op queue"
```

### Task 6: Settings store (DataStore)

**Files:**
- Create: `settings/ServerConfig.kt`, `settings/SettingsRepository.kt` (interface + DataStore impl), `di/SettingsModule.kt`
- Test: `app/src/test/java/dev/boudy04/taskvault/settings/DataStoreSettingsRepositoryTest.kt`

**Interfaces:**
- Produces: `data class ServerConfig(val baseUrl: String, val token: String)`; `interface SettingsRepository { val config: Flow<ServerConfig>; suspend fun current(): ServerConfig; suspend fun setConfig(config: ServerConfig) }`; Hilt binding; defaults baseUrl=`https://prject-cv-production.up.railway.app`, token=`dev-token`.

- [x] **Step 1: Failing test**

```kotlin
package dev.boudy04.taskvault.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun repo(scope: CoroutineScope) = DataStoreSettingsRepository(
        PreferenceDataStoreFactory.create(scope = CoroutineScope(scope.coroutineContext + Dispatchers.IO)) {
            tmp.newFile("settings.preferences_pb")
        },
    )

    @Test
    fun defaults_areRailwayAndDemoToken() = runTest {
        val r = repo(backgroundScope)
        val c = r.current()
        assertEquals("https://prject-cv-production.up.railway.app", c.baseUrl)
        assertEquals("dev-token", c.token)
    }

    @Test
    fun setConfig_persists() = runTest {
        val r = repo(backgroundScope)
        r.setConfig(ServerConfig("http://10.0.2.2:8000", "abc"))
        assertEquals("abc", r.config.first().token)
        assertEquals("http://10.0.2.2:8000", r.config.first().baseUrl)
    }
}
```

- [x] **Step 2: Red**

Run: `.\gradlew :app:compileDebugUnitTestKotlin --quiet` → FAIL (unresolved).

- [x] **Step 3: Implement**

`settings/ServerConfig.kt`:

```kotlin
package dev.boudy04.taskvault.settings

data class ServerConfig(val baseUrl: String, val token: String)
```

`settings/SettingsRepository.kt`:

```kotlin
package dev.boudy04.taskvault.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsRepository {
    val config: Flow<ServerConfig>
    suspend fun current(): ServerConfig
    suspend fun setConfig(config: ServerConfig)
}

private const val DEFAULT_URL = "https://prject-cv-production.up.railway.app"
private const val DEFAULT_TOKEN = "dev-token"

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val url = stringPreferencesKey("server_url")
        val token = stringPreferencesKey("auth_token")
    }

    override val config: Flow<ServerConfig> = dataStore.data.map { p ->
        ServerConfig(p[Keys.url] ?: DEFAULT_URL, p[Keys.token] ?: DEFAULT_TOKEN)
    }

    override suspend fun current(): ServerConfig = config.first()

    override suspend fun setConfig(config: ServerConfig) {
        dataStore.edit { p ->
            p[Keys.url] = config.baseUrl
            p[Keys.token] = config.token
        }
    }
}
```

(add `import kotlinx.coroutines.flow.first`.)

`di/SettingsModule.kt`:

```kotlin
package dev.boudy04.taskvault.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("taskvault_settings")
        }
}
```

- [x] **Step 4: Green**

Run: `.\gradlew testDebugUnitTest --quiet` → PASS.

- [x] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: datastore-backed server settings repository"
```

### Task 7: Retrofit stack replacing the fake network layer

**Files:**
- Create: `data/source/network/TaskDto.kt`, `data/source/network/TaskApiService.kt`, `data/source/network/AuthInterceptor.kt`, `data/source/network/BaseUrlInterceptor.kt`
- Modify: `di/DataModules.kt` (provide OkHttp/Retrofit/api), `data/ModelMappingExt.kt` (drop `toNetwork` variants touching removed types)
- Delete: `data/source/network/{NetworkDataSource.kt, NetworkTask.kt, TaskNetworkDataSource.kt}`
- Test: `app/src/test/java/dev/boudy04/taskvault/data/source/network/RetrofitNetworkDataSourceTest.kt`

**Interfaces:**
- Consumes: `SettingsRepository` (Task 6).
- Produces: `interface TaskApiService { listTasks(status:String?):List<TaskDto>; getTask(id:Int):TaskDto; createTask(TaskDto):TaskDto; updateTask(id:Int,TaskDto):TaskDto; deleteTask(id:Int):Response<Unit> }`; `@Serializable TaskDto(id:Int, title, description, status:String, priority:String, created_at, updated_at)`; Hilt providers for `OkHttpClient`, `Retrofit`, `TaskApiService`; DTO↔domain mapping helpers `TaskDto.toLocal(newLocalId:String): LocalTask` and `LocalTask.toDto(): TaskDto`.

- [x] **Step 1: Failing test against MockWebServer**

```kotlin
package dev.boudy04.taskvault.data.source.network

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinxserialization.asConverterFactory
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
        assert(recorded.getHeader("Authorization")!!.contains("Bearer").not()) // no interceptor in this raw stack
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
```

(import `okhttp3.MediaType.Companion.toMediaType`.)

- [x] **Step 2: Red**

Run: `.\gradlew :app:compileDebugUnitTestKotlin --quiet` → FAIL (types unresolved).

- [x] **Step 3: Implement DTO, API, interceptors**

`TaskDto.kt`:

```kotlin
package dev.boudy04.taskvault.data.source.network

import dev.boudy04.taskvault.data.TaskPriority
import dev.boudy04.taskvault.data.TaskStatus
import dev.boudy04.taskvault.data.source.local.LocalTask
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TaskDto(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val status: String = "todo",
    val priority: String = "medium",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

fun TaskStatus.toApi(): String = when (this) {
    TaskStatus.TODO -> "todo"
    TaskStatus.IN_PROGRESS -> "in_progress"
    TaskStatus.DONE -> "done"
}

fun TaskPriority.toApi(): String = when (this) {
    TaskPriority.LOW -> "low"
    TaskPriority.MEDIUM -> "medium"
    TaskPriority.HIGH -> "high"
}

fun String.toTaskStatus(): TaskStatus = when (this) {
    "done" -> TaskStatus.DONE
    "in_progress" -> TaskStatus.IN_PROGRESS
    else -> TaskStatus.TODO
}

fun String.toTaskPriority(): TaskPriority = when (this) {
    "high" -> TaskPriority.HIGH
    "low" -> TaskPriority.LOW
    else -> TaskPriority.MEDIUM
}

/** Server task → fresh local row (new UUID, linked by serverId). */
fun TaskDto.toLocal(): LocalTask = LocalTask(
    id = UUID.randomUUID().toString(),
    title = title,
    description = description,
    isCompleted = status == "done",
    status = status.toTaskStatus(),
    priority = priority.toTaskPriority(),
    serverId = id,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun LocalTask.toDto(): TaskDto = TaskDto(
    id = serverId ?: 0,
    title = title,
    description = description,
    status = status.toApi(),
    priority = priority.toApi(),
)
```

`TaskApiService.kt`:

```kotlin
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
```

`AuthInterceptor.kt`:

```kotlin
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
```

`BaseUrlInterceptor.kt`:

```kotlin
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
```

`di/DataModules.kt` additions (remove old `NetworkDataSource` binding):

```kotlin
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
fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
    .baseUrl("http://localhost/") // rewritten per-request by BaseUrlInterceptor
    .client(client)
    .addConverterFactory(
        Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()),
    )
    .build()

@Provides
@Singleton
fun provideTaskApi(retrofit: Retrofit): TaskApiService = retrofit.create(TaskApiService::class.java)
```

Delete `NetworkDataSource.kt`, `NetworkTask.kt`, `TaskNetworkDataSource.kt`; strip their imports/usages (`ModelMappingExt` loses `toNetwork` functions; `DefaultTaskRepository` still references `NetworkDataSource` until Task 8 — temporarily keep compilation by deleting its `saveTasksToNetwork` body contents and field, which Task 8 replaces anyway; simplest interim edit: remove the field + method + call sites).

- [x] **Step 4: Green**

Run: `.\gradlew testDebugUnitTest assembleDebug --quiet` → PASS.

- [x] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: retrofit task-api stack with dynamic base url and bearer auth"
```

### Task 8: Repository write-path with offline queue

**Files:**
- Modify: `data/TaskRepository.kt` (interface), `data/DefaultTaskRepository.kt` (rewrite)
- Create: `sync/SyncScheduler.kt` (+ `WorkManagerSyncScheduler.kt`), `sync/TaskPayload.kt`
- Modify: `di/DataModules.kt` (bind scheduler)
- Test: `app/src/test/java/dev/boudy04/taskvault/data/OfflineFirstRepositoryTest.kt` (new; fakes inline)

**Interfaces:**
- Consumes: `PendingOpDao`, `TaskDao.getByServerId/getCompleted`, `TaskDto.toDto/toLocal` helpers.
- Produces: `TaskRepository` gains `fun getPendingSyncIdsStream(): Flow<Set<String>>`; `createTask/updateTask` gain `priority: TaskPriority` parameter; `SyncScheduler` functional interface `requestSync()` bound to WorkManager impl (unique work `taskvault_sync`, CONNECTED constraint, EXPONENTIAL backoff 30s); `TaskPayload(localId,title,description,status,priority,serverId)` serializable snapshot.

- [x] **Step 1: Failing repository tests**

`OfflineFirstRepositoryTest.kt` (fakes defined in-file):

```kotlin
package dev.boudy04.taskvault.data

import dev.boudy04.taskvault.sync.SyncScheduler
import dev.boudy04.taskvault.sync.TaskPayload
import dev.boudy04.taskvault.data.source.local.LocalTask
import dev.boudy04.taskvault.data.source.local.PendingOpDao
import dev.boudy04.taskvault.data.source.local.PendingOpEntity
import dev.boudy04.taskvault.data.source.local.PendingOpState
import dev.boudy04.taskvault.data.source.local.PendingOpType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstRepositoryTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val json = Json

    class FakeTaskDao : MutableMapBackedDao() // see helper below: implements used subset
    class FakePendingOpDao : /* in-memory */ ArrayList<PendingOpEntity>() { /* implements subset */ }

    // Full fake implementations live in test fixtures; they record inserts and serve flows.
}
```

Because fakes must implement full interfaces, generate concrete fixture files during implementation:

`app/src/test/java/dev/boudy04/taskvault/data/FakeTaskDao.kt` — in-memory `HashMap<String, LocalTask>` implementing `TaskDao` methods used by the repository (`observeAll/observeById/getAll/getById/upsert/upsertAll/updateCompleted/deleteById/deleteAll/deleteCompleted/getByServerId/getCompleted/deleteUnsyncedOrphans`), flows backed by `MutableStateFlow<List<LocalTask>>`.

`FakePendingOpDao.kt` — `MutableList<PendingOpEntity>` implementing `PendingOpDao`, `insert` assigns incrementing `opId`, `nextPending` picks min `(enqueuedAt, opId)` among PENDING, `observePendingTaskIds` derived from list.

Test cases (each asserts repository behavior + queued op):

```kotlin
@Test
fun createTask_writesRow_enqueuesCreate_andRequestsSync() = runTest {
    val repo = repoWithFakes()
    val id = repo.createTask("Write plan", "body", TaskPriority.HIGH)
    val stored = fakeTaskDao.getById(id)!!
    assertEquals(TaskPriority.HIGH, stored.priority)
    assertEquals(TaskStatus.TODO, stored.status)
    val op = fakePendingOps.single()
    assertEquals(PendingOpType.CREATE, op.opType)
    val payload = json.decodeFromString<TaskPayload>(op.payload)
    assertEquals(id, payload.localId)
    assertEquals(1, syncRequests.size)
}

@Test
fun deleteTask_withServerId_enqueuesDelete_andDropsRowImmediately() = runTest {
    val repo = repoWithFakes(seedTaskWithServerId = 42)
    val localId = fakeTaskDao.all().first().id
    repo.deleteTask(localId)
    assertNull(fakeTaskDao.getById(localId))
    assertEquals(PendingOpType.DELETE, fakePendingOps.single().opType)
}

@Test
fun activateComplete_toggleStatus_andEnqueueUpdate() = runTest {
    val repo = repoWithFakes()
    val id = repo.createTask("a", "b", TaskPriority.MEDIUM)
    repo.completeTask(id)
    assertEquals(TaskStatus.DONE, fakeTaskDao.getById(id)!!.status)
    repo.activateTask(id)
    assertEquals(TaskStatus.TODO, fakeTaskDao.getById(id)!!.status)
    assertEquals(3, fakePendingOps.count { it.opType == PendingOpType.UPDATE } +
        fakePendingOps.count { it.opType == PendingOpType.CREATE }) // 1 create + 2 updates
}

@Test
fun clearCompletedTasks_deletesEachLocally() = runTest { /* two done tasks -> both gone, 2 DELETE ops */ }
```

Helper `repoWithFakes()` wires `DefaultTaskRepository(fakeTaskDao, fakePendingOps, recordingScheduler, json, StandardTestDispatcher(testScheduler))`.

- [x] **Step 2: Red**

Run: `.\gradlew :app:compileDebugUnitTestKotlin --quiet` → FAIL (interface members missing).

- [x] **Step 3: Implement scheduler, payload, repository**

`sync/SyncScheduler.kt`:

```kotlin
package dev.boudy04.taskvault.sync

fun interface SyncScheduler {
    /** Requests a unique sync run; safe to call from anywhere. */
    fun requestSync()
}
```

`sync/WorkManagerSyncScheduler.kt`:

```kotlin
package dev.boudy04.taskvault.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkManagerSyncScheduler @Inject constructor(
    private val workManager: WorkManager,
) : SyncScheduler {
    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("taskvault_sync", ExistingWorkPolicy.REPLACE, request)
    }
}
```

`sync/TaskPayload.kt`:

```kotlin
package dev.boudy04.taskvault.sync

import kotlinx.serialization.Serializable

@Serializable
data class TaskPayload(
    val localId: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val serverId: Int? = null,
)
```

`data/TaskRepository.kt` interface changes:

```kotlin
suspend fun createTask(title: String, description: String, priority: TaskPriority = TaskPriority.MEDIUM): String
suspend fun updateTask(taskId: String, title: String, description: String, priority: TaskPriority = TaskPriority.MEDIUM)
fun getPendingSyncIdsStream(): Flow<Set<String>>
```

`data/DefaultTaskRepository.kt` rewrite (core shown; every mutation follows write-local → enqueue-op → request-sync):

```kotlin
@Singleton
class DefaultTaskRepository @Inject constructor(
    private val localDataSource: TaskDao,
    private val pendingOps: PendingOpDao,
    private val syncScheduler: SyncScheduler,
    private val json: Json,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : TaskRepository {

    override suspend fun createTask(title: String, description: String, priority: TaskPriority): String {
        val taskId = withContext(dispatcher) { UUID.randomUUID().toString() }
        val task = LocalTask(
            id = taskId, title = title, description = description,
            isCompleted = false, status = TaskStatus.TODO, priority = priority,
        )
        localDataSource.upsert(task)
        enqueue(PendingOpType.CREATE, task)
        return taskId
    }

    override suspend fun updateTask(taskId: String, title: String, description: String, priority: TaskPriority) {
        val task = localDataSource.getById(taskId) ?: throw Exception("Task ($taskId) not found")
        val updated = task.copy(title = title, description = description, priority = priority)
        localDataSource.upsert(updated)
        enqueue(PendingOpType.UPDATE, updated)
    }

    override suspend fun completeTask(taskId: String) = setStatus(taskId, TaskStatus.DONE)
    override suspend fun activateTask(taskId: String) = setStatus(taskId, TaskStatus.TODO)

    private suspend fun setStatus(taskId: String, status: TaskStatus) {
        val task = localDataSource.getById(taskId) ?: return
        val updated = task.copy(status = status, isCompleted = status == TaskStatus.DONE)
        localDataSource.upsert(updated)
        enqueue(PendingOpType.UPDATE, updated)
    }

    override suspend fun deleteTask(taskId: String) {
        val task = localDataSource.getById(taskId) ?: return
        pendingOps.clearForTask(taskId)          // drop stale ops for this row
        localDataSource.deleteById(taskId)       // UI reflects deletion instantly
        if (task.serverId != null) enqueue(PendingOpType.DELETE, task)
    }

    override suspend fun clearCompletedTasks() =
        localDataSource.getCompleted().forEach { deleteTask(it.id) }

    override suspend fun deleteAllTasks() =
        localDataSource.getAll().forEach { deleteTask(it.id) }

    override suspend fun refresh() { syncScheduler.requestSync() }   // worker pulls after drain
    override suspend fun refreshTask(taskId: String) { refresh() }
    override suspend fun getTasks(forceUpdate: Boolean): List<Task> {
        if (forceUpdate) syncScheduler.requestSync()
        return withContext(dispatcher) { localDataSource.getAll().toExternal() }
    }

    override suspend fun getTask(taskId: String, forceUpdate: Boolean): Task? {
        if (forceUpdate) syncScheduler.requestSync()
        return localDataSource.getById(taskId)?.toExternal()
    }

    override fun getTasksStream(): Flow<List<Task>> =
        localDataSource.observeAll().map { rows -> withContext(dispatcher) { rows.toExternal() } }

    override fun getTaskStream(taskId: String): Flow<Task?> =
        localDataSource.observeById(taskId).map { it?.toExternal() }

    override fun getPendingSyncIdsStream(): Flow<Set<String>> =
        pendingOps.observePendingTaskIds().map { it.toSet() }

    private suspend fun enqueue(type: PendingOpType, task: LocalTask) {
        val payload = TaskPayload(
            localId = task.id, title = task.title, description = task.description,
            status = task.status.toApi(), priority = task.priority.toApi(), serverId = task.serverId,
        )
        pendingOps.insert(
            PendingOpEntity(taskLocalId = task.id, opType = type, payload = json.encodeToString(payload)),
        )
        syncScheduler.requestSync()
    }
}
```

Constructor drops `networkDataSource` + `@ApplicationScope scope`; fix `CoroutinesModule` usages accordingly (annotation stays for other consumers). DI binding: `@Binds abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler`. Provide `Json` via `@Provides @Singleton fun provideJson(): Json = Json { ignoreUnknownKeys = true }`. Provide `WorkManager.getInstance(context)` via SettingsModule-style provider. Update `AddEditTaskViewModel`/tests call sites for the `priority` parameter (thread a selected-priority state through the edit screen — full UI in Task 10; here default `TaskPriority.MEDIUM` keeps call sites compiling except where tests exercise it).

- [x] **Step 4: Green**

Run: `.\gradlew testDebugUnitTest --quiet` → PASS.

- [x] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: offline-first repository with pending-op queue"
```

### Task 9: SyncEngine + SyncWorker (drain + pull)

**Files:**
- Create: `sync/SyncEngine.kt`, `sync/SyncOutcome.kt`, `sync/SyncWorker.kt`
- Test: `app/src/test/java/dev/boudy04/taskvault/sync/SyncEngineTest.kt`

**Interfaces:**
- Consumes: `TaskApiService`, `TaskDao`, `PendingOpDao`, `TaskPayload`, DTO helpers.
- Produces: `class SyncEngine(api, tasks, ops, json, io)` with `suspend fun run(): SyncOutcome` where `enum class SyncOutcome { SUCCESS, RETRY, FAILURE }`; `@HiltWorker class SyncWorker(...) : CoroutineWorker` delegating to engine (`RETRY` → `Result.retry()`, `FAILURE` → `Result.failure()`, else success).

- [x] **Step 1: Failing engine tests** (fakes: `FakeApi` records calls/scripted responses incl. throwing `IOException` and `HttpException`; reuse FakeTaskDao/FakePendingOpDao fixtures from Task 8)

Cases:
1. `drain_createsRemote_assignsServerId_clearsOp` — CREATE op → POST returns dto id 9 → row.serverId == 9, queue empty.
2. `update_usesPutWithServerId` — seeded row serverId 5, UPDATE op → PUT `/api/tasks/5`, timestamps refreshed on row.
3. `delete_remotely_then_opRemoved` — DELETE op → 204 → op gone.
4. `serverGone_404_dropsOpAndRow` — PUT throws HttpException(404) → op deleted, local row deleted.
5. `ioError_returnsRetry_keepsOpPending` — POST throws IOException → op back to PENDING, outcome RETRY.
6. `unauthorized_stopsAsFailure` — 401 → outcome FAILURE, op stays PENDING.
7. `afterDrain_pullMergesServer_deletesVanishedRows_unlessPendingOpsProtectThem` — remote list lacks row X (no pending ops) → row X gone; remote adds Y → inserted with serverId.

- [x] **Step 2: Red** — `.\gradlew :app:compileDebugUnitTestKotlin --quiet` → FAIL.

- [x] **Step 3: Implement**

`sync/SyncOutcome.kt`:

```kotlin
package dev.boudy04.taskvault.sync

enum class SyncOutcome { SUCCESS, RETRY, FAILURE }
```

`sync/SyncEngine.kt` core:

```kotlin
class SyncEngine @Inject constructor(
    private val api: TaskApiService,
    private val tasks: TaskDao,
    private val ops: PendingOpDao,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun run(): SyncOutcome = withContext(io) {
        val drain = drain()
        if (drain != null) return@withContext drain
        pull()
        SyncOutcome.SUCCESS
    }

    /** Returns terminal outcome to bubble, or null when drain completed cleanly. */
    private suspend fun drain(): SyncOutcome? {
        while (true) {
            val op = ops.nextPending() ?: return null
            ops.updateState(op.opId, PendingOpState.RUNNING)
            val payload = json.decodeFromString<TaskPayload>(op.payload)
            try {
                when (op.opType) {
                    PendingOpType.CREATE -> {
                        val created = api.createTask(payload.toDtoWithoutServerId())
                        tasks.getById(payload.localId)?.let { row ->
                            tasks.upsert(row.copy(serverId = created.id, createdAt = created.createdAt, updatedAt = created.updatedAt))
                        }
                    }
                    PendingOpType.UPDATE -> {
                        val target = payload.serverId ?: error("UPDATE without serverId")
                        val updated = api.updateTask(target, payload.toDtoWithoutServerId())
                        tasks.getById(payload.localId)?.let { row ->
                            tasks.upsert(row.copy(updatedAt = updated.updatedAt))
                        }
                    }
                    PendingOpType.DELETE -> {
                        val target = payload.serverId
                        if (target != null) api.deleteTask(target)
                    }
                }
                ops.deleteByIds(listOf(op.opId))
            } catch (e: IOException) {
                ops.updateState(op.opId, PendingOpState.PENDING)
                return SyncOutcome.RETRY
            } catch (e: HttpException) {
                when (e.code()) {
                    401 -> { ops.updateState(op.opId, PendingOpState.PENDING); return SyncOutcome.FAILURE }
                    404 -> {
                        if (payload.serverId != null) tasks.getByServerId(payload.serverId!!)?.let { tasks.deleteById(it.id) }
                        ops.deleteByIds(listOf(op.opId))
                    }
                    else -> if (e.code() in 500..599) {
                        ops.updateState(op.opId, PendingOpState.PENDING); return SyncOutcome.RETRY
                    } else {
                        ops.deleteByIds(listOf(op.opId)) // unrecoverable 4xx: drop op, keep row
                    }
                }
            }
        }
    }

    private suspend fun pull() {
        val remote = try { api.listTasks(null) } catch (e: Exception) { return }
        val protected = ops.getAll().filter { it.state == PendingOpState.PENDING }.map { it.taskLocalId }.toSet()
        val remoteIds = remote.map { it.id }.toSet()
        remote.forEach { dto ->
            val existing = tasks.getByServerId(dto.id)
            if (existing == null) {
                tasks.upsert(dto.toLocal())
            } else {
                tasks.upsert(existing.copyFromDto(dto))
            }
        }
        tasks.getAll().forEach { row ->
            val sid = row.serverId
            if (sid != null && sid !in remoteIds && row.id !in protected) tasks.deleteById(row.id)
        }
    }
}
```

Supporting extensions (place in `ModelMappingExt.kt`):

```kotlin
fun TaskPayload.toDtoWithoutServerId() = TaskDto(
    id = serverId ?: 0, title = title, description = description, status = status, priority = priority,
)

fun LocalTask.copyFromDto(dto: TaskDto) = copy(
    title = dto.title, description = dto.description,
    isCompleted = dto.status == "done",
    status = dto.status.toTaskStatus(), priority = dto.priority.toTaskPriority(),
    createdAt = dto.createdAt, updatedAt = dto.updatedAt,
)
```

`sync/SyncWorker.kt`:

```kotlin
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
```

- [x] **Step 4: Green**

Run: `.\gradlew testDebugUnitTest --quiet` → PASS (all seven engine cases).

- [x] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: workmanager sync worker draining offline queue with pull reconcile"
```

### Task 10: UI touches (priority, unsynced dot, settings screen)

**Files:**
- Modify: `taskedit/AddEditTaskViewModel.kt` (+ Screen), `tasks/{TasksViewModel,TasksScreen,TasksListScreen?}.kt`, `common/composable components for list items`, `navigation/TodoNavGraph?` → `TaskVaultNavGraph`, top app bar component (gear icon)
- Create: `settings/SettingsScreen.kt`, `settings/SettingsViewModel.kt`
- Test: extend `shared-test` viewmodel fakes + one `TasksViewModelTest` case asserting pending-id exposure

**Interfaces:**
- Consumes: `TaskRepository.getPendingSyncIdsStream()`, `createTask/updateTask(priority)` from Task 8; `SettingsRepository` from Task 6.
- Produces: nav route `"settings"`; priority dropdown persists chosen priority; list rows show colored priority chip + amber dot when `task.id in pendingSyncIds`.

- [x] **Step 1: Failing viewmodel test**

In `shared-test`/`app/src/test`: `TasksViewModelTest.pendingIds_exposedThroughUiState` — repository fake emits `{taskId}` → uiState contains unsynced id set.

- [x] **Step 2: Red** — run, expect missing member.

- [x] **Step 3: Implement**

- `TasksViewModel`: inject repository; combine tasks stream with `repository.getPendingSyncIdsStream()` into ui state (`data class TasksUiState(items, isLoading, pendingSyncIds: Set<String>)` mirroring existing filter handling).
- Row composable: after completion checkbox render `PriorityChip(priority)` (colors: HIGH `#B3261E`, MEDIUM `#7D5700`, LOW `#38693C` container variants) and if unsynced an 8.dp amber dot (`Color(0xFFFFB300)`).
- `AddEditTaskViewModel`: `priority: MutableStateFlow<TaskPriority>`; save calls pass priority; screen shows `ExposedDropdownMenuBox` with three options (labels Low/Medium/High).
- Navigation: add `object Settings : destination route "settings"`; gear `IconButton` (icon `Icons.Filled.Settings`) in tasks top bar navigates to it.
- `SettingsScreen`: two `OutlinedTextField`s prefilled from `settings.current()` (collect once in VM init), Save button → `setConfig`, Test button → `OkHttpClient().newCall(Request.Builder().url("$baseUrl/health").build()).executeAsync()` inside viewModelScope showing snackbar result text.
- Backwards-compat: `StatisticsViewModel` etc. untouched.

- [x] **Step 4: Green + manual smoke**

Run: `.\gradlew testDebugUnitTest --quiet` → PASS.
Manual: `.\gradlew installDebug`, open app → settings → enter token → save → create task with High priority → verify appears on `https://prject-cv-production.up.railway.app/docs` data; toggle airplane mode, edit task, restore network → dot clears.

- [x] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: priority picker, sync badges, and server settings screen"
```

### Task 11: CI workflow

**Files:**
- Create: `.github/workflows/ci.yml`

**Interfaces:**
- Produces: green required checks `unit-lint` and `instrumented` on push/PR to `main`.

- [x] **Step 1: Workflow file**

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  unit-lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - uses: gradle/actions/setup-gradle@v4
      - name: Spotless + lint + unit tests
        run: ./gradlew spotlessCheck lintDebug testDebugUnitTest --stacktrace

  instrumented:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - uses: gradle/actions/setup-gradle@v4
      - name: Instrumented tests on emulator
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          arch: x86_64
          script: ./gradlew connectedDebugAndroidTest --stacktrace
```

If `spotlessCheck` isn't applied in root build (verify: `grep -rn spotless build.gradle.kts`), drop that task argument rather than adding the plugin.

- [x] **Step 2: Verify locally what CI runs**

Run: `.\gradlew spotlessSpotlessCheck 2>$null; .\gradlew lintDebug testDebugUnitTest --quiet`
Expected: PASS (spotless variant name resolved per applied plugin; skip if unapplied).

- [x] **Step 3: Commit + push**

```bash
git add .github && git commit -m "ci: unit/lint and emulator jobs"
# After creating GitHub repo boudy04/taskvault (public):
git remote add origin https://github.com/boudy04/taskvault.git
git push -u origin main
```

### Task 12: README rewrite, screenshots, CV-PLAN bookkeeping

**Files:**
- Modify: `README.md`, `../CV-PLAN.md` (workspace tracking table)

**Interfaces:**
- Consumes: shipped app + green CI.

- [x] **Step 1: Screenshots**

Emulator: capture Tasks list (with chips + dot), Edit screen (priority dropdown), Settings screen → `docs/screenshots/*.png`, committed.

- [x] **Step 2: README content**

Rewrite with sections: What it is (offline-first Android client for my REST API, built on `android/architecture-samples` — credit + license note), Architecture diagram line (`Compose → ViewModel → Repository → Room+PendingOps → WorkManager → Retrofit → Railway`), Features (offline queue, last-write-wins reconcile, dynamic server settings), How to run (flavors n/a; settings screen setup), Testing (≥5 commands with counts, e.g. `.\gradlew testDebugUnitTest`, specific classes), CI badge, quantified numbers (unit test count, emulator job time).

- [x] **Step 3: CV-PLAN tracking update**

In workspace `CV-PLAN.md` Tracking table: weeks 6–7 row → `✅ boudy04/taskvault — Kotlin/Compose offline-first client, WorkManager queue, CI (unit+emulator)`; adjust estimate note to ~3–4 weekends.

- [x] **Step 4: Final verify**

Run: `.\gradlew testDebugUnitTest lintDebug --quiet` and confirm GitHub Actions green; pin repo on profile.

- [x] **Step 5: Commit**

```bash
git add -A && git commit -m "docs: readme, screenshots, plan tracking"
```

---

## Self-Review Notes

- Spec coverage: §1 data layer → Tasks 4–5; §2 sync flow → Tasks 8–9; §3 network/settings → Tasks 6–7 (flavor deviation documented in Global Constraints); §4 rebrand/UI → Tasks 2, 10; §5 CI → Task 11; §6 testing/README → Tasks 4–9 tests + Task 12.
- Type consistency checked: `TaskPayload`, `PendingOpEntity`, `SyncScheduler.requestSync()`, `ServerConfig`, `TaskDto.toLocal()/toDto()`, `getPendingSyncIdsStream()` used consistently across tasks.
- Known risk: exact upstream filenames in feature packages (`tasks/`, `taskedit/`, navigation) may differ slightly; Task 10 says "locate equivalents" — executor greps for `TopAppBar`/nav graph before editing.

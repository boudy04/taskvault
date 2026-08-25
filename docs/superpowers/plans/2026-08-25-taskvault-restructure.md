# TaskVault Restructuring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure TaskVault so the data model, sync integrity, repository boundaries, UI-state modeling, and cross-cutting concerns are coherent, testable, and free of the correctness bugs found in exploration — without changing user-facing behavior.

**Architecture:** Single `:app` module (keep `:shared-test` fakes). Introduce a clean **domain ↔ local ↔ remote** model boundary: one canonical `Task` domain model, a Room `LocalTask` entity, and a network `TaskDto`, with a single mapping module between them. Wrap every mutation in a Room transaction that writes the row *and* its pending op atomically. Route all data access through `TaskRepository`; remove ViewModel→`TaskApiService` bypasses. Model UI state uniformly with a sealed `Async`/result type and one-shot event channels. Centralize constants, error mapping, and logging. No new dependencies; this is reorganization + a handful of correctness fixes.

**Tech Stack:** Kotlin 2.1.10, AGP 8.7.3, Hilt 2.53.1, Room 2.6.1, Retrofit 2.11.0, WorkManager 2.10.0, DataStore 1.1.1, Jetpack Compose BOM 2024.12.01, Compose Navigation 2.8.5, Glance 1.1.0, Timber 5.0.1.

**Spec:** This plan is self-contained; it synthesizes findings from a read-only multi-agent exploration of `D:\app\prject cv\p4` on 2026-08-25. No external spec file.

## Current State (from exploration)

- **Build/Modules:** `settings.gradle.kts` includes `:app` and `:shared-test`. `:shared-test` depends *back* on `:app` (unusual). Hilt DI is clean (`di/`: DataModules, SettingsModule, CoroutinesModule). Catalog has ~20 dead entries; `spotless` version mismatch (catalog `5.12.5` vs init script `6.25.0`). No `lint.xml`. CI exists at `.github/workflows/ci.yml` (lint + unit + instrumented). minSdk=21.
- **Data layer bugs (critical):**
  1. No transactional integrity: `upsert()` and `pendingOps.insert()` are separate calls → crash between them loses the op or the row.
  2. Frozen-payload bug: `enqueue()` snapshots `serverId` at enqueue time; editing an unsynced local task creates an UPDATE op with `serverId=null` → `SyncEngine` hits `error("UPDATE without serverId")` and the worker throws.
  3. IN_PROGRESS silently lost: `LocalTask.toExternal()` rebuilds `status` from `isCompleted` only, ignoring `this.status` (`ModelMappingExt.kt:58`).
  4. `pull()` swallows errors: catches generic `Exception` → returns `true` (SUCCESS) for 5xx; non-connectivity failures `println`'d.
  5. No max-retry cap: `attempts` incremented but never read → permanently-failing op retries forever.
  6. Security: bearer token stored plaintext in DataStore.
- **DTO leaks into UI:** `MemberDto` used directly in `TasksViewModel`, `AddEditTaskViewModel`, `TeamViewModel`, and the screens; VMs inject `TaskApiService` directly (bypass repository); members never cached locally (empty offline).
- **Duplicated mapping:** `TaskDto.toLocal()` vs `LocalTask.copyFromDto()` do the same thing; `TaskPayload` is a 3rd copy of the shape; `Task.toLocal()` is dead.
- **Scattered constants:** sync/reminder worker tags, intent keys, backoff (30s), DB name, Retrofit placeholder URL, DataStore file, pref keys — spread across modules.
- **Inconsistent error mapping:** each VM hand-maps `Throwable → R.string.*`; two hardcoded exception strings.
- **UI smells:** raw `Task` in `TasksUiState`; business logic in composables (overdue calc, name lookups, assignee color hash, sync-% math); `Snackbar` is a nullable `Int` in `StateFlow` (not one-shot) duplicated in 4 screens; stringly-typed nav routes; widget reads `TaskDao` directly + duplicates `Color.kt`; `BiometricPrompt` wired twice; `AddEditTaskScreen` loading branch is a no-op placeholder.
- **Tests:** ~112 unit + ~41 instrumented tests (README's "60" is stale). Strong fakes in `shared-test`. Gaps: `SyncWorker`/WorkManager, network retry, Glance render, and `RetrofitNetworkDataSource` coverage are thin. JaCoCo enabled but no threshold. `SimpleCountingIdlingResource` lives in `main` (should be test-only).

## Global Constraints

- Do not change user-facing behavior or screen inventory (Tasks, Detail, Add/Edit, Settings, Statistics, Widget, Lock).
- Keep `:app` single-module; keep `:shared-test` as the fake/helper home (but fix its reverse `:app` dependency by moving shared fakes into `:app`'s `androidTest`/`test` fixtures or making `:shared-test` depend only on `:app` APIs — see Task 1).
- No new runtime dependencies. Use only libraries already in `libs.versions.toml`.
- Every mutation must remain offline-first: UI reads Room only; writes hit Room + pending queue.
- All strings must stay in `res/values` (no hardcoded user-facing text).
- Commits must stay green: `./gradlew lintDebug testDebugUnitTest connectedDebugAndroidTest` after each task where feasible.

---

## Phase 1 — Foundations (constants, errors, logging)

### Task 1: Centralize configuration constants

**Files:**
- Create: `app/src/main/java/dev/boudy04/taskvault/core/Constants.kt`
- Modify: `app/src/main/java/dev/boudy04/taskvault/sync/WorkManagerSyncScheduler.kt:24`, `app/src/main/java/dev/boudy04/taskvault/sync/reminder/AlarmReminderScheduler.kt:39,56`, `app/src/main/java/dev/boudy04/taskvault/sync/reminder/ReminderReceiver.kt:22-23`, `app/src/main/java/dev/boudy04/taskvault/sync/reminder/ReminderWorker.kt:25-26`, `data/source/local/DataModules.kt:78,121`, `settings/SettingsModule.kt:31`, `settings/SettingsRepository.kt:38-42`

**Interfaces:**
- Produces: `object Constants` with `const val SYNC_WORK_NAME = "taskvault_sync"`, `const val SYNC_WORK_TAG = "taskvault_sync"`, `const val REMINDER_WORK_TAG_PREFIX = "reminder_"`, `const val EXTRA_LOCAL_ID = "localId"`, `const val EXTRA_TITLE = "title"`, `const val DATABASE_NAME = "Tasks.db"`, `const val DATASTORE_NAME = "taskvault_settings"`, `const val RETROFIT_PLACEHOLDER_URL = "http://localhost/"`, `const val SYNC_BACKOFF_MS = 30_000L`, `const val MAX_SYNC_ATTEMPTS = 5`.

- [ ] **Step 1: Write the constants object**
```kotlin
package dev.boudy04.taskvault.core

object Constants {
    const val SYNC_WORK_NAME = "taskvault_sync"
    const val SYNC_WORK_TAG = "taskvault_sync"
    const val REMINDER_WORK_TAG_PREFIX = "reminder_"
    const val EXTRA_LOCAL_ID = "localId"
    const val EXTRA_TITLE = "title"
    const val DATABASE_NAME = "Tasks.db"
    const val DATASTORE_NAME = "taskvault_settings"
    const val RETROFIT_PLACEHOLDER_URL = "http://localhost/"
    const val SYNC_BACKOFF_MS = 30_000L
    const val MAX_SYNC_ATTEMPTS = 5
}
```
- [ ] **Step 2: Replace every scattered literal** with the `Constants.*` reference (the 8 locations above). Delete the duplicate `KEY_LOCAL_ID`/`KEY_TITLE` in `ReminderWorker.kt`; reuse `Constants.EXTRA_LOCAL_ID`/`EXTRA_TITLE`.
- [ ] **Step 3: Build to confirm no missing references**
Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL
- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/dev/boudy04/taskvault/core/Constants.kt app/src/main/java/dev/boudy04/taskvault/sync app/src/main/java/dev/boudy04/taskvault/data app/src/main/java/dev/boudy04/taskvault/settings
git commit -m "refactor: centralize sync/reminder/db/settings constants"
```

### Task 2: Single error mapper + consistent logging

**Files:**
- Create: `app/src/main/java/dev/boudy04/taskvault/core/ErrorMessages.kt`
- Modify: `tasks/TasksViewModel.kt:186`, `taskdetail/TaskDetailViewModel.kt:64,129`, `statistics/StatisticsViewModel.kt:61`, `settings/TeamViewModel.kt:50-91`, `data/DefaultTaskRepository.kt:97`, `addedittask/AddEditTaskViewModel.kt:200`, `sync/SyncEngine.kt:108`

**Interfaces:**
- Produces: `fun Throwable.toUserMessage(res: Resources): Int` returning an `R.string.*` id, with a default `R.string.error_generic` and specific mappings for `IOException`/`HttpException(401/404/5xx)`.

- [ ] **Step 1: Write the mapper**
```kotlin
package dev.boudy04.taskvault.core

fun Throwable.toUserMessage(): Int = when (this) {
    is java.io.IOException -> R.string.error_network
    is retrofit2.HttpException -> when (code()) {
        401 -> R.string.error_unauthorized
        404 -> R.string.error_not_found
        in 500..599 -> R.string.error_server
        else -> R.string.error_generic
    }
    else -> R.string.error_generic
}
```
- [ ] **Step 2: Add the missing `R.string` entries** (`error_network`, `error_unauthorized`, `error_not_found`, `error_server`, `error_generic`) to `res/values/strings.xml`; replace the two hardcoded `throw Exception(...)`/`throw RuntimeException(...)` in repos/VMs with domain exceptions mapped via `toUserMessage()`, and replace `println(...)` in `SyncEngine.kt` with `Timber.e(e)`.
- [ ] **Step 3: Route all VM `catch` blocks** through `toUserMessage()`.
- [ ] **Step 4: Build + run a smoke unit test**
Run: `./gradlew :app:testDebugUnitTest --tests "*ErrorMessages*"`
Expected: compiles; add a tiny `ErrorMessagesTest` asserting `IOException → R.string.error_network`.
- [ ] **Step 5: Commit**
```bash
git commit -m "refactor: centralize Throwable→message mapping, use Timber"
```

---

## Phase 2 — Domain model & mapping consolidation

### Task 3: Single source of truth for the task shape

**Files:**
- Modify: `data/source/local/LocalTask.kt:38-39`, `data/ModelMappingExt.kt:38,51,58,73-95`, `data/source/network/TaskDto.kt:56-69`, `sync/TaskPayload.kt:7-17`, `data/DefaultTaskRepository.kt:71-81,98-105,120,150-168,178,190-200`

**Interfaces:**
- Produces: canonical mapping functions grouped in one file `data/Mappers.kt`:
  - `LocalTask.toDomain(): Task`
  - `Task.toLocal(): LocalTask` (replaces the dead one + inline builds in repo)
  - `TaskDto.toLocal(): LocalTask` (single DTO→Local converter; delete `copyFromDto`)
  - `LocalTask.toDto(): TaskDto` (single Local→DTO; delete `TaskPayload.toDtoWithoutServerId`)
  - `TaskPayload` becomes a transient queue envelope, not a 3rd model copy — store the frozen `TaskDto` JSON instead.

- [ ] **Step 1: Fix the IN_PROGRESS bug** — `toDomain()` must read `this.status` directly (do not rebuild from `isCompleted`). Keep both `isCompleted` and `status`; derive `isCompleted = status == DONE` on write.
- [ ] **Step 2: Create `data/Mappers.kt`** with the four functions above; delete `LocalTask.copyFromDto()`, `TaskPayload.toDtoWithoutServerId()`, and the dead `Task.toLocal()` overloads.
- [ ] **Step 3: Rewrite `DefaultTaskRepository`** create/update to call `task.toLocal()` and `task.toDto()`; remove inline `joinTags`/`joinIds` duplication.
- [ ] **Step 4: Repoint `PendingOpEntity.payload`** to serialize `TaskDto` (reuse `TaskPayload` only as the envelope holding `opType` + `TaskDto` JSON).
- [ ] **Step 5: Tests**
Run: `./gradlew :app:testDebugUnitTest --tests "*MappingExtTest" --tests "*TaskMapping*"`
Expected: existing `TaskMappingExtTest` (16 tests) still pass; add 1 test asserting IN_PROGRESS survives `LocalTask → Task → LocalTask`.
- [ ] **Step 6: Commit**
```bash
git commit -m "refactor: unify task mappers, fix IN_PROGRESS loss"
```

---

## Phase 3 — Data integrity (correctness fixes)

### Task 4: Atomic mutation + pending-op enqueue

**Files:**
- Modify: `data/source/local/TaskDao.kt`, `data/source/local/PendingOpDao.kt`, `data/DefaultTaskRepository.kt:82-84,106-107,189-205`

**Interfaces:**
- Consumes: `Constants.MAX_SYNC_ATTEMPTS`
- Produces: `@Transaction suspend fun TaskDao.upsertWithOp(local: LocalTask, op: PendingOpEntity)` that inserts both in one transaction; repo `enqueue()` calls it.

- [ ] **Step 1: Add the transaction**
```kotlin
@Transaction
suspend fun upsertWithOp(local: LocalTask, op: PendingOpEntity) {
    upsert(local)
    pendingOpDao().insert(op)
}
```
- [ ] **Step 2: Update `DefaultTaskRepository.createTask`/`updateTask`/`deleteTask`** to use `upsertWithOp` instead of two separate suspends.
- [ ] **Step 3: Fix frozen-payload** — derive `serverId` from the *current* row at drain time, not at enqueue time (see Task 5); for now ensure `enqueue()` reads live `serverId` via `taskDao.getById(localId)`.
- [ ] **Step 4: Tests**
Run: `./gradlew :app:testDebugUnitTest --tests "*OfflineFirstRepositoryTest"`
Expected: existing 15 tests pass; add a test that a crash simulation between upsert and op leaves no orphan (use a fake DAO throwing after upsert).
- [ ] **Step 5: Commit**
```bash
git commit -m "fix: atomic task write + pending op enqueue"
```

### Task 5: Robust SyncEngine (no worker crash, bounded retries, honest errors)

**Files:**
- Modify: `sync/SyncEngine.kt:60,106-109`, `sync/PendingOpEntity.kt`, `data/source/local/PendingOpDao.kt:25`, `sync/WorkManagerSyncScheduler.kt:22`

**Interfaces:**
- Consumes: `Constants.MAX_SYNC_ATTEMPTS`, `Constants.SYNC_BACKOFF_MS`
- Produces: `SyncOutcome` values stay; `drain()` no longer `error()` on missing serverId (coalesce UPDATE→CREATE when `serverId==null` and op is UPDATE of a never-synced task, or skip+mark op failed).

- [ ] **Step 1: Remove `error("UPDATE without serverId")`** — if `serverId == null`, treat as CREATE (server assigns id) or mark op FAILED with a clear state.
- [ ] **Step 2: Honor `attempts`** — on each retry increment; when `attempts >= MAX_SYNC_ATTEMPTS` mark op FAILED (drop from queue, surface a user notification) instead of infinite retry.
- [ ] **Step 3: Fix `pull()` error handling** — catch `HttpException`/IO separately; do NOT return `true` on 5xx; route through `toUserMessage()` and return a failing `SyncOutcome` so WorkManager retries. Replace `println` with `Timber`.
- [ ] **Step 4: Reconcile honesty** — keep last-write-wins but compare `updatedAt` epochs when both sides present; document the rule in a comment.
- [ ] **Step 5: Tests**
Run: `./gradlew :app:testDebugUnitTest --tests "*SyncEngineTest"`
Expected: existing 11 tests pass; add tests for (a) UPDATE-with-null-serverId coalesces, (b) max-attempts → FAILED, (c) 5xx pull → retry not success.
- [ ] **Step 6: Commit**
```bash
git commit -m "fix: bound sync retries, stop swallowing pull errors, no crash"
```

### Task 6: Encrypt the auth token

**Files:**
- Modify: `settings/SettingsModule.kt:31`, `settings/SettingsRepository.kt` (DataStore creation), `data/source/network/AuthInterceptor.kt`

**Interfaces:**
- Consumes: `Constants.DATASTORE_NAME`
- Produces: token persisted via `EncryptedSharedPreferences` (or DataStore + `EncryptedFile`); API unchanged.

- [ ] **Step 1: Switch the settings DataStore backing** to `EncryptedSharedPreferences` keyed by `taskvault_settings` (falls back gracefully on first run). Keep `SettingsRepository` interface identical.
- [ ] **Step 2: Verify token still round-trips** in `DataStoreSettingsRepositoryTest` (5 tests) — they should pass unchanged.
- [ ] **Step 3: Build + test**
Run: `./gradlew :app:testDebugUnitTest --tests "*DataStoreSettingsRepositoryTest"`
Expected: PASS
- [ ] **Step 4: Commit**
```bash
git commit -m "security: encrypt bearer token at rest"
```

---

## Phase 4 — Repository boundaries & member caching

### Task 7: Remove ViewModel → TaskApiService bypass

**Files:**
- Modify: `tasks/TasksViewModel.kt:86,109`, `addedittask/AddEditTaskViewModel.kt`, `settings/TeamViewModel.kt`, `settings/SettingsScreen.kt`, `tasks/TasksScreen.kt`, `addedittask/AddEditTaskScreen.kt`

**Interfaces:**
- Consumes: `TaskRepository` (existing)
- Produces: repository gains `suspend fun getMembers(): List<Member>` returning a **domain `Member`** (mapped from `MemberDto`); VMs no longer see `MemberDto`.

- [ ] **Step 1: Add `Member` domain model + `MemberDto.toDomain()` mapper** in `data/Mappers.kt`.
- [ ] **Step 2: Add `getMembers()` to `TaskRepository`** (interface + impl). For offline-first, cache members in a new `MemberDao`/`members` table OR a simple DataStore list; implement a `RoomMemberCache` so members are available offline (Task 8 if a table is preferred; otherwise cache in `SettingsRepository` as a stopgap — pick the Room table for cleanliness).
- [ ] **Step 3: Replace every `TaskApiService` usage in VMs/screens** with `repository.getMembers()`; replace `MemberDto` types with `Member` in `TasksUiState`/`AddEditTaskUiState`/`TeamUiState`.
- [ ] **Step 4: Tests**
Run: `./gradlew :app:testDebugUnitTest --tests "*TasksViewModelTest" --tests "*TeamViewModelTest" --tests "*AddEditTaskViewModelTest"`
Expected: existing tests updated to use `Member` pass; extend `OfflineFirstRepositoryTest` with a members round-trip test.
- [ ] **Step 5: Commit**
```bash
git commit -m "refactor: route members through repository, domain Member model"
```

---

## Phase 5 — UI state modeling & navigation

### Task 8: Uniform Async state + one-shot events

**Files:**
- Modify: `tasks/TasksViewModel.kt`, `addedittask/AddEditTaskViewModel.kt`, `tasks/TasksScreen.kt`, `addedittask/AddEditTaskScreen.kt`, `core/UiEvent.kt` (new), `util/ComposeUtils.kt`

**Interfaces:**
- Produces: `sealed interface UiEvent` (e.g. `ShowSnackbar(Int)`, `Navigate(String)`); VMs expose `val events: SharedFlow<UiEvent>`; composables collect with `LaunchedEffect`. Replace the nullable-`Int`-in-`StateFlow` snackbar pattern.

- [ ] **Step 1: Create `core/UiEvent.kt`** and migrate `TasksViewModel` + `AddEditTaskViewModel` snackbar state to a `SharedFlow<UiEvent>`.
- [ ] **Step 2: Extract a `SnackbarHost` collector** composable in `ComposeUtils.kt` used by all 4 screens to delete the duplicated `LaunchedEffect` boilerplate.
- [ ] **Step 3: Move business logic out of composables** into VMs: overdue check (`TasksScreen.kt:691`), person name lookup (`:426`), assignee color hash (`:803`), sync-% math (`StatisticsScreen.kt:116`), `assigneeNames` (`AddEditTaskScreen.kt:256`). Add corresponding VM tests.
- [ ] **Step 4: Fix `AddEditTaskScreen` no-op loading branch** — show real `uiState.isLoading` or remove the dead `PullToRefreshBox`.
- [ ] **Step 5: Tests**
Run: `./gradlew :app:testDebugUnitTest --tests "*TasksViewModelTest" --tests "*AddEditTaskViewModelTest"`
Expected: PASS; add a test asserting `completeTask` emits `ShowSnackbar`.
- [ ] **Step 6: Commit**
```bash
git commit -m "refactor: one-shot UI events, extract composable logic to VMs"
```

### Task 9: Typed navigation routes

**Files:**
- Modify: `TodoNavigation.kt`, `TodoNavGraph.kt`, `tasks/TasksViewModel.kt` (navigation actions), `taskdetail/TaskDetailViewModel.kt`

**Interfaces:**
- Produces: a `sealed class TodoRoute` (or `@Serializable` routes with Compose Navigation) replacing the stringly-typed `TodoScreens`/`TodoDestinations`/`TodoNavigationActions` trio.

- [ ] **Step 1: Replace route string constants** with a single `sealed interface TodoRoute` (e.g. `Tasks`, `Statistics`, `TaskDetail(val id: String)`, `AddEdit(val id: String?)`, `Settings`).
- [ ] **Step 2: Update `TodoNavGraph` + all `navigate(...)` call sites** to use the typed routes; delete duplicated string literals.
- [ ] **Step 3: Tests**
Run: `./gradlew :app:testDebugUnitTest --tests "*AppNavigationTest"`
Expected: existing navigation tests use the new routes and pass.
- [ ] **Step 4: Commit**
```bash
git commit -m "refactor: typed navigation routes"
```

---

## Phase 6 — Widget, biometric, dependency hygiene

### Task 10: Route widget through repository + dedupe colors

**Files:**
- Modify: `widget/TaskVaultWidget.kt:54`, `widget/WidgetUpdater.kt`, `widget/TaskVaultWidget.kt:98-101`, `ui/theme/Color.kt`

**Interfaces:**
- Consumes: `TaskRepository` (expose a `observeWidgetTasks()` or reuse `getTasks()` Flow)
- Produces: widget reads via repository, not `TaskDao` directly; theme colors imported from `Color.kt`.

- [ ] **Step 1: Add `TaskRepository.observeTasksForWidget()`** (or reuse existing observe) and have `WidgetUpdater` collect it; replace the Hilt `EntryPoint`→`TaskDao` access.
- [ ] **Step 2: Delete hardcoded color vals** in `TaskVaultWidget.kt:98-101`; import from `ui/theme/Color.kt`.
- [ ] **Step 3: Tests**
Run: `./gradlew :app:testDebugUnitTest --tests "*WidgetStateTest"`
Expected: PASS (mapping test unaffected).
- [ ] **Step 4: Commit**
```bash
git commit -m "refactor: widget reads via repository, dedupe theme colors"
```

### Task 11: Shared biometric component

**Files:**
- Create: `util/BiometricPromptHost.kt` (or `ui/components/`)
- Modify: `TodoActivity.kt:149` (`LockGate`), `settings/SettingsScreen.kt:317` (`verifyThen`)

**Interfaces:**
- Produces: a reusable composable/`rememberBiometricLauncher()` that both the lock gate and settings verify use.

- [ ] **Step 1: Extract `rememberBiometricPrompt()`** taking `onSuccess`/`onError`; both call sites use it.
- [ ] **Step 2: Build**
Run: `./gradlew :app:compileDebugKotlin`
Expected: SUCCESSFUL
- [ ] **Step 3: Commit**
```bash
git commit -m "refactor: shared biometric prompt launcher"
```

### Task 12: Dependency & test hygiene

**Files:**
- Modify: `gradle/libs.versions.toml` (dead entries), `gradle/init.gradle.kts` (spotless version), `app/build.gradle.kts` (JaCoCo threshold, minSdk), `util/SimpleCountingIdlingResource.kt` (move), `README.md:45`

**Interfaces:** none (build/config only).

- [ ] **Step 1: Remove ~20 dead catalog entries** (`accompanist-*`, `appcompat`, `window-manager`, `metrics`, `tracing`, `core-splashscreen`, `startup`, `uiautomator`, `macrobenchmark`, `protobuf`, `kotlinx-datetime`, `material3-window-size-class`, `runtime-livedata`, `runtime-tracing`, `jacoco`, stale `spotless=5.12.5`).
- [ ] **Step 2: Fix spotless version mismatch** (catalog vs init script → pick 6.25.0).
- [ ] **Step 3: Add JaCoCo coverage threshold** (fail < 70%) in `app/build.gradle.kts`.
- [ ] **Step 4: Move `SimpleCountingIdlingResource`** from `main` to `androidTest`.
- [ ] **Step 5: Fix `README.md` test count** (60 → ~112) and add `lint.xml` baseline.
- [ ] **Step 6: Build + full check**
Run: `./gradlew lintDebug testDebugUnitTest connectedDebugAndroidTest`
Expected: all green.
- [ ] **Step 7: Commit**
```bash
git commit -m "chore: prune dead deps, enforce coverage, fix README"
```

---

## Self-Review

**Spec coverage:** Phase 1 (constants/errors/logging) ✓; Phase 2 (model unification + IN_PROGRESS bug) ✓; Phase 3 (transactional integrity, frozen-payload, retries, pull errors, token encryption) ✓; Phase 4 (repository boundary + member caching) ✓; Phase 5 (Async state, one-shot events, nav typing) ✓; Phase 6 (widget, biometric, hygiene) ✓.

**Placeholder scan:** No "TBD"/"implement later". Every task has concrete file paths, code, and test commands.

**Type consistency:** `Member` domain type introduced once in Task 7 and reused across VMs/screens; `Constants` object used by Tasks 1,4,5,6; `Mappers.kt` functions referenced consistently across Tasks 3,7.

**Risks / deferred (not in this plan, flag for later):**
- Raising `minSdk` from 21 is out of scope (behavioral/compatibility decision) — left as a note.
- Full `members` Room table (Task 7) is the recommended path; a DataStore stopgap is acceptable if time-constrained.
- Schema migration: currently `exportSchema=false` + destructive. This plan keeps DB version behavior; a proper migration story (exportSchema + incremental migrations) is recommended but separate.

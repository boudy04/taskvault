# TaskVault — Offline-First Android Client for Task API — Design

> Plan item #5 (CV-PLAN weeks 6–7). Kotlin Android app consuming the deployed FastAPI
> task-api. Built by cloning Google's `android/architecture-samples` and replacing its fake
> remote data source with a real Retrofit stack plus an offline-first mutation queue.
> Closes gaps G1, M1, M4; contributes to G5/G6.

## Decisions locked

| Topic | Decision |
|-------|----------|
| Sync strategy | Full offline queue — Room is source of truth, mutations queue and sync via WorkManager |
| Auth | In-app settings screen; server URL + Bearer token stored in DataStore |
| Model fidelity | Full API schema surfaced: 3-state status + priority in storage **and** UI |
| CI | Unit + lint job **and** emulator instrumented-test job |
| Branding | Full rebrand: name **TaskVault**, package/applicationId `dev.boudy04.taskvault`, new icon/colors |
| Base | Clone of `android/architecture-samples` (Apache-2.0), credit retained |

## Upstream contract (task-api)

- Base URL (default): `https://prject-cv-production.up.railway.app`
- Auth: `Authorization: Bearer <token>` on all `/api/tasks` calls (`401` otherwise); `/health` is public.
- Endpoints: `GET /api/tasks[?status=]`, `GET /api/tasks/{id}`, `POST`, `PUT/PATCH /api/tasks/{id}`, `DELETE /api/tasks/{id}` (204).
- Task JSON: `id:int, title, description, status ∈ todo|in_progress|done,
  priority ∈ low|medium|high, created_at, updated_at` (ISO timestamps).

## Architecture

Clone keeps the sample's shape: single activity, Compose UI, ViewModel per screen,
Hilt DI, repository behind interfaces, `mock`/`prod` product flavors.

### §1 Data layer

- `TaskEntity` (Room). PK stays the sample's `localId: String` (UUID) so offline-created
  tasks exist before the server knows them:
  - `localId: String PK`, `title`, `description`,
    `status ∈ {TODO, IN_PROGRESS, DONE}`, `priority ∈ {LOW, MEDIUM, HIGH}`,
    `serverId: Int?`, `createdAt/updatedAt: String?` (server ISO stamps)
- New `PendingOpEntity`:
  - `opId: Long PK auto`, `taskLocalId FK`, `opType ∈ {CREATE, UPDATE, DELETE}`,
    `payload: String` (JSON snapshot of full task at enqueue time),
    `state ∈ {PENDING, RUNNING}`, `attempts: Int`, `enqueuedAt`
- TypeConverters for enums; ISO-8601 strings pass through unchanged.
- Three-way mappers: domain ↔ entity ↔ DTO. Domain `Task` gains `status` + `priority`;
  computed `isCompleted = (status == DONE)` preserved so existing filters/UI keep working.
- DAOs: existing `TaskDao` extended as needed; new `PendingOpDao`:
  `insert`, `peekOldestPending`, `markRunning`, `deleteByIds`, `countFlow`, `clearByTaskId`.

### §2 Sync flow

Write path (every mutation):
1. Apply change to Room immediately (UI updates via Flow).
2. Insert `PendingOp` with full payload snapshot.
3. Enqueue unique work: `OneTimeWorkRequest` on `SyncWorker`, constraint
   `NetworkType.CONNECTED`, exponential backoff, `ExistingWorkPolicy.REPLACE`.

`SyncWorker` (HiltWorker) drains oldest-first while ops remain:

| Op | Call | On success |
|----|------|-----------|
| CREATE | `POST /api/tasks` | Write returned `id` → `serverId`; store server timestamps |
| UPDATE | `PUT /api/tasks/{serverId}` | Store updated timestamps |
| DELETE | `DELETE /api/tasks/{serverId}` | Delete local row |

Never-synced rows (`serverId == null`): CREATE always precedes UPDATE/DELETE in the
FIFO queue, so by the time those ops run the id exists. A DELETE for a still-null-id
row just drops the local row.

Failure handling:
- Network error / 5xx → rethrow; WorkManager retries with backoff; op returns to PENDING.
- `404` on UPDATE/DELETE → drop op, delete local row (deleted elsewhere/server reset).
- `401` → stop draining; surface "check settings" state (no infinite retry storm).

Pull sync (runs when queue drains empty, and on app open):
- `GET /api/tasks` → upsert by `serverId`.
- Delete local rows whose `serverId` no longer exists remotely **unless** a pending op
  references them (protects offline creates/edits).

Conflict policy: last-write-wins on `updatedAt`. Safe because single user + static token.
*Deliberate simplification* — per-user accounts would require real conflict resolution.

UI never blocks on network; rows with pending ops show an unsynced dot badge.
Sample's clear-completed action enqueues one DELETE op per completed task.

### §3 Network + settings

- Retrofit + kotlinx-serialization converter; OkHttp logging in debug builds only.
- `AuthInterceptor`: reads token from DataStore (memory-cached after first read),
  injects `Authorization` header.
- Base URL read from DataStore; default = Railway URL above.
- Settings screen: server URL field, token field, Save → DataStore,
  "Test connection" button pings `/health` and reports result. Entry: toolbar gear icon.
- Only the `prod` flavor wires the real stack behind the existing data-source
  interfaces; `mock` flavor untouched so unit/shared tests stay hermetic.

### §4 Rebrand & UI touches

- Package/applicationId refactor: `com.example.android.architecture.blueprints.todoapp`
  → `dev.boudy04.taskvault`; display name "TaskVault".
- Adaptive launcher icon + Material color scheme swap.
- Priority dropdown in task edit screen; colored priority chip per card;
  `IN_PROGRESS` tasks render under the Active filter with their own chip.
- Unsynced dot badge on rows having pending ops.
- Apache-2.0 LICENSE file kept verbatim + credit line in README
  ("built on android/architecture-samples").

### §5 CI

`.github/workflows/ci.yml`, triggers: push + PR to `main`. Two jobs:

1. `unit-lint` (ubuntu): `ktlintCheck`, `testProdDebugUnitTest`, `lintProdDebug`
2. `instrumented` (ubuntu, API-level emulator via reactivecircus/android-emulator-runner):
   `connectedProdDebugAndroidTest` (covers sharedTest suite)

README carries the Actions badge.

### §6 Testing & README (CV-PLAN part F)

Unit tests (JVM): repository queue behavior against fake remote source (enqueue → drain
→ retry → 404 drop paths), three-way mapper tests, `SyncWorker` via WorkManager testing
artifacts, settings store. Target ≥25 tests, presented as ≥5 runnable commands.

Instrumented/shared: extend existing sharedTest suite with prod-flavor repository path.

README rewrite: what it is (offline-first Android client for *my own* REST API),
architecture diagram (Compose → ViewModel → Repository → Room+Queue → WorkManager →
Retrofit → Railway), how-to-run both flavors incl. settings setup, screenshots,
quantified numbers (test count, sync behavior, CI badge).

## Non-goals

- No user accounts/multi-user auth (static bearer token by design of the API).
- No push notifications, no background periodic sync (sync on mutation/open/connectivity).
- No pagination (API list is unbounded for portfolio-scale data).

## Definition of done

Per CV-PLAN Part F: public pinned repo `boudy04/taskvault`, README w/ screenshots +
test commands, tests passing, ci.yml green, upstream credit intact. CV-PLAN tracking row
updated (estimate revised: ~3–4 weekends due to full offline queue).

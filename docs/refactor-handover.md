# TaskVault — `data/` + `sync/` Architecture Refactor — Handover

Date: 2026-08-29 · Branch: `main` (6 commits ahead of `origin/main`, **not pushed** — push is the maintainer's call)
Constraint honored throughout: **zero behavior change** (verified by characterization gates + full suite).

## 1. What was implemented (plan checklist)

| Plan step | Status | Evidence |
|---|---|---|
| Step 0 — Characterization baseline (tests first) | ✅ | Commit `79dfb86`: 2 HTTP wire-body canaries in `SyncEngineTest` (CREATE body = full `TaskDto` with `id=0`; UPDATE body carries `serverId` as `id`) + `IN_PROGRESS` mapping pin in `TaskMappingExtTest`. Existing suite already covered op-classification (member→STATUS / admin→UPDATE / personal→none) and the full reminder arm/cancel matrix |
| Step 1 — Consolidate mapping into one file | ✅ | Commit `7bbcfa1`: all 8 mappers (`Task⇄LocalTask`, `TaskDto.toLocal`, `LocalTask.toDto`, `toApi`/`toTaskStatus`/`toTaskPriority`) in [ModelMappingExt.kt](../app/src/main/java/dev/boudy04/taskvault/data/ModelMappingExt.kt); [TaskDto.kt](../app/src/main/java/dev/boudy04/taskvault/data/source/network/TaskDto.kt) is now a plain DTO. Pure relocation. Adding a task field now touches **one** mapping file |
| Step 2 — Collapse `TaskPayload` onto `TaskDto` | ✅ | Commit `7be9de7`: [DefaultTaskRepository.enqueue](../app/src/main/java/dev/boudy04/taskvault/data/DefaultTaskRepository.kt) serializes `TaskDto`; [SyncEngine.drain](../app/src/main/java/dev/boudy04/taskvault/sync/SyncEngine.kt) decodes `TaskDto`, resolves rows via `op.taskLocalId`, keeps `error("… without serverId")` semantics via `id == 0` guards. `TaskPayload.kt` + `toDtoWithoutServerId` deleted. HTTP request bodies byte-identical (canary-pinned) |
| Step 3 — Extract `PendingOpClassifier` | ✅ | Commit `bb2a85f`: [PendingOpClassifier.kt](../app/src/main/java/dev/boudy04/taskvault/data/PendingOpClassifier.kt) owns "which op does this mutation produce" (pure, `isMember` passed in); 4 inline branches in the repository replaced; 7-case [PendingOpClassifierTest](../app/src/test/java/dev/boudy04/taskvault/data/PendingOpClassifierTest.kt) added |
| Step 4 — Extract `ReminderEngine` + inject `Clock` | ✅ | Commit `a814b09`: [ReminderEngine.kt](../app/src/main/java/dev/boudy04/taskvault/data/ReminderEngine.kt) (parse ISO dueAt → future-check via injected `Clock` → arm/cancel behind `ReminderScheduler`); duplicated logic in the repository and `ReminderBootReceiver` removed; `TimeModule` in [DataModules.kt](../app/src/main/java/dev/boudy04/taskvault/di/DataModules.kt) provides `Clock.systemUTC()` (epoch-millis identical to the old `System.currentTimeMillis()` reads) |
| Step 5 — Thin repository + dead-code cleanup | ✅ | Commit `c93c99e`: `TaskDao.updateCompleted` (zero production callers) removed together with its `FakeTaskDao` override (see ISS-05); `println` debug leftover in `SyncEngine.pull()` removed |

## 2. Change record (commits, oldest → newest)

```
79dfb86 test: Step 0 characterization gates - sync wire bodies + IN_PROGRESS mapping pin
7bbcfa1 refactor(data): consolidate all Task model mappers into ModelMappingExt (pure relocation)
7be9de7 refactor(sync): queue TaskDto payloads directly; drop TaskPayload shape
bb2a85f refactor(data): extract PendingOpClassifier - one seam for mutation-to-op policy
a814b09 refactor(data): extract ReminderEngine with injected Clock; single reminder policy
c93c99e chore(data): drop dead updateCompleted DAO method, its fake override, and sync debug println
```

All six commits touch only `data/**`, `sync/**`, their tests, and `shared-test/FakeTaskDao.kt` (+ this `docs/` folder, untracked). The parallel UI effort's working-tree changes were never staged.

## 3. Regression verification matrix

| Run | When | Command | Result |
|---|---|---|---|
| R1 | Post Step 0/1 (pre-relocation baseline) | `gradlew :app:testDebugUnitTest` | BUILD SUCCESSFUL 52s — all PASS |
| R2 | Post Step 1 | same | BUILD SUCCESSFUL 46s — all PASS |
| R3 | Post Step 2 (interrupted session) | same | verified via Gradle `UP-TO-DATE` SUCCESSFUL + re-run |
| R4 | Post Step 2 (fresh confirmation) | same | BUILD SUCCESSFUL 13s, `testDebugUnitTest UP-TO-DATE` (prior green run with identical inputs) |
| R5 | Post Step 3 | same | BUILD SUCCESSFUL 52s — all PASS |
| R6 | Post Step 4 | same | BUILD SUCCESSFUL 35s — all PASS (Hilt `TimeModule` resolved) |
| R7 | Post Step 5 (caught ISS-05, fixed) | same | BUILD SUCCESSFUL 15s — all PASS |
| **R8 final** | **Final committed state** | `gradlew :app:compileDebugKotlin :app:testDebugUnitTest --rerun` | **BUILD SUCCESSFUL 16s — 16 suites, 145 tests, 0 failures, 0 errors, 0 skipped** (forced re-execution, no up-to-date shortcuts) |

`androidTest` was not modified or run (per plan). Working tree for `data/`, `sync/`, `app/src/test`, `shared-test` is clean after the final commit.

## 4. Behavioral equivalence notes (read before assuming "free" changes)

- Wire bodies sent to the server are byte-identical to pre-refactor (canary-pinned at the `FakeApi` level).
- Ops queued by a **pre-refactor build** may fail once on first drain (legacy JSON keys ignored → `id = 0`); accepted per the app's existing `fallbackToDestructiveMigration` practice — see issue log ISS-07.
- `setStatus` performs one extra cached `settings.session.first()` read for personal rows (ISS-08) — no observable change.
- `IN_PROGRESS` tasks still surface as `TODO` (pre-existing asymmetry, now pinned by a test) — fixing it is a product decision.
- `LocalTask.toDto()` currently has no production caller (kept: it completes the mapping seam and is test-covered).

## 5. Known limitations & recommended follow-ups (out of scope here)

1. **Widget bypasses the repository**: `WidgetUpdater.kt` / `TaskVaultWidget.kt` read `TaskDao` directly (same anti-pattern this refactor removed from `sync/`). Next bounded context to fix.
2. **`data → sync` dependencies are now interface-only** (`SyncScheduler`, `ReminderScheduler`, `ReminderEngine`→`ReminderScheduler`) — acceptable ports; full hexagonal isolation would move scheduling behind a data-owned interface.
3. **`AuthInterceptor` / `BaseUrlInterceptor` still `runBlocking` on DataStore** (pre-existing, marked `ponytail` in source).
4. **Push is pending**: 6 commits are local only; review and `git push` when ready.
5. Database schema untouched (`version = 6`); no migration needed for any of these changes.

## 6. How to re-run verification

```powershell
cd "D:\app\prject cv\p4"
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --rerun --console=plain
# expect: BUILD SUCCESSFUL — 16 suites / 145 tests / 0 failures
```

Per-suite XML lives in `app/build/test-results/testDebugUnitTest/`.

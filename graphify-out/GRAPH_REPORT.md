# Graph Report - p4  (2026-08-23)

## Corpus Check
- 100 files · ~106,409 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 719 nodes · 1333 edges · 45 communities (42 shown, 3 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 67 edges (avg confidence: 0.84)
- Token cost: 1,800 input · 1,400 output

## Community Hubs (Navigation)
- Room DAO Tests
- Add/Edit Screen Tests
- Add/Edit Task UI
- Pending Op DAO Tests
- Network Interceptors
- Settings UI
- Task API Service
- Activity & Navigation
- Tasks Screen Tests
- Settings DI Module
- Shared Test Fakes
- Tasks List ViewModel
- CI Pipeline & Contribution Docs
- Add/Edit Task ViewModel
- Task Detail ViewModel
- Default Task Repository
- Task Model & Statistics
- Offline-First Sync Design
- ViewModel Unit Tests
- Statistics ViewModel
- Tasks ViewModel Tests
- Dependency Renovation Config
- App Screenshots
- Espresso Idling Resource
- Custom Test Runner
- Application Class
- Sync Worker
- Launcher Icon xhdpi
- Launcher Icon xxhdpi
- Launcher Icon xxxhdpi
- App Demo GIF
- Trash Icon Assets
- Launcher Icon hdpi
- Launcher Icon mdpi
- Gradle Wrapper Scripts
- Brand Logo Assets
- Auth & Server Config

## God Nodes (most connected - your core abstractions)
1. `Task` - 51 edges
2. `LocalTask` - 39 edges
3. `FakeTaskRepository` - 38 edges
4. `TaskRepository` - 37 edges
5. `TaskPriority` - 24 edges
6. `AddEditTaskViewModel` - 22 edges
7. `TaskDao` - 22 edges
8. `FakeTaskDao` - 22 edges
9. `TodoTheme()` - 21 edges
10. `DefaultTaskRepository` - 21 edges

## Surprising Connections (you probably didn't know these)
- `TaskVault Offline-First Client Design Spec` --references--> `mock/prod Product Flavors`  [AMBIGUOUS]
  docs/superpowers/specs/2026-08-22-taskvault-offline-client-design.md → README.md
- `Contributor License Agreement (CLA)` --conceptually_related_to--> `Android Architecture Samples (Upstream Project)`  [INFERRED]
  CONTRIBUTING.md → README.md
- `Pull Request Code Review Requirement` --conceptually_related_to--> `Android Architecture Samples (Upstream Project)`  [INFERRED]
  CONTRIBUTING.md → README.md
- `toExternal()` --calls--> `Task`  [INFERRED]
  app/src/main/java/dev/boudy04/taskvault/data/ModelMappingExt.kt → app/src/main/java/dev/boudy04/taskvault/data/Task.kt
- `FakeTaskRepository` --references--> `Task`  [EXTRACTED]
  shared-test/src/main/java/dev/boudy04/taskvault/data/FakeTaskRepository.kt → app/src/main/java/dev/boudy04/taskvault/data/Task.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Offline-First Sync Pipeline (write-local -> enqueue -> drain -> pull)** — docs_superpowers_plans_2026_08_22_taskvault_offline_client_default_task_repository_write_path, docs_superpowers_plans_2026_08_22_taskvault_offline_client_pending_op_entity, docs_superpowers_plans_2026_08_22_taskvault_offline_client_pending_op_dao, docs_superpowers_plans_2026_08_22_taskvault_offline_client_sync_scheduler, docs_superpowers_plans_2026_08_22_taskvault_offline_client_sync_worker, docs_superpowers_plans_2026_08_22_taskvault_offline_client_sync_engine, docs_superpowers_plans_2026_08_22_taskvault_offline_client_task_api_service, docs_superpowers_plans_2026_08_22_taskvault_offline_client_local_task_entity [EXTRACTED 1.00]
- **CI: unit-lint Job + Emulator instrumented Job** — _github_workflows_ci_workflow, _github_workflows_ci_unit_lint_job, _github_workflows_ci_instrumented_job [EXTRACTED 1.00]
- **Retrofit Network Stack (dynamic base URL + Bearer auth + kotlinx-serialization)** — docs_superpowers_plans_2026_08_22_taskvault_offline_client_task_api_service, docs_superpowers_plans_2026_08_22_taskvault_offline_client_task_dto, docs_superpowers_plans_2026_08_22_taskvault_offline_client_auth_interceptor, docs_superpowers_plans_2026_08_22_taskvault_offline_client_base_url_interceptor, docs_superpowers_plans_2026_08_22_taskvault_offline_client_datastore_settings_repository [EXTRACTED 1.00]

## Communities (45 total, 3 thin omitted)

### Community 0 - "Room DAO Tests"
Cohesion: 0.06
Nodes (10): TaskDaoTest, LocalTask, Flow, TaskDao, TODO, TaskPayload, OfflineFirstRepositoryTest, RecordingSyncScheduler (+2 more)

### Community 1 - "Add/Edit Screen Tests"
Cohesion: 0.06
Nodes (12): AddEditTaskScreenTest, SemanticsNodeInteraction, StatisticsScreenTest, TaskDetailScreenTest, AppNavigationTest, SemanticsNodeInteraction, HiltTestActivity, ComponentActivity (+4 more)

### Community 2 - "Add/Edit Task UI"
Cohesion: 0.06
Nodes (29): AddEditTaskContent(), AddEditTaskScreen(), Modifier, SnackbarHostState, PriorityPicker(), copyFromDto(), toDtoWithoutServerId(), toExternal() (+21 more)

### Community 3 - "Pending Op DAO Tests"
Cohesion: 0.06
Nodes (15): PendingOpDaoTest, Flow, PendingOpDao, PendingOpEntity, PendingOpState, PENDING, RUNNING, ToDoDatabase (+7 more)

### Community 4 - "Network Interceptors"
Cohesion: 0.07
Nodes (21): AuthInterceptor, Interceptor, Response, BaseUrlInterceptor, Interceptor, Response, bindSyncScheduler(), NetworkModule (+13 more)

### Community 5 - "Settings UI"
Cohesion: 0.10
Nodes (34): Modifier, SnackbarHostState, SettingsContent(), SettingsContentPreview(), SettingsScreen(), Modifier, SnackbarHostState, StatisticsContent() (+26 more)

### Community 6 - "Task API Service"
Cohesion: 0.11
Nodes (12): Response, TaskApiService, TaskDto, SyncEngine, SyncOutcome, FAILURE, RETRY, SUCCESS (+4 more)

### Community 7 - "Activity & Navigation"
Cohesion: 0.12
Nodes (20): ComponentActivity, TodoActivity, CoroutineScope, DrawerState, Modifier, TodoNavGraph(), TodoDestinations, TodoNavigationActions (+12 more)

### Community 9 - "Settings DI Module"
Cohesion: 0.15
Nodes (11): Context, SettingsModule, ServerConfig, DataStoreSettingsRepository, Keys, Flow, SettingsRepository, DataStoreSettingsRepositoryTest (+3 more)

### Community 10 - "Shared Test Fakes"
Cohesion: 0.14
Nodes (3): FakeTaskRepository, Flow, StateFlow

### Community 11 - "Tasks List ViewModel"
Cohesion: 0.15
Nodes (10): TasksFilterType, ACTIVE_TASKS, ALL_TASKS, COMPLETED_TASKS, FilteringUiInfo, MutableStateFlow, StateFlow, ViewModel (+2 more)

### Community 12 - "CI Pipeline & Contribution Docs"
Cohesion: 0.12
Nodes (20): instrumented CI Job (Android Emulator, connectedDebugAndroidTest), unit-lint CI Job (lintDebug + testDebugUnitTest), TaskVault CI Workflow (.github/workflows/ci.yml), Google Sample Packaging Metadata (android/architecture-samples), Contributor License Agreement (CLA), Pull Request Code Review Requirement, Rebrand to dev.boudy04.taskvault / TaskVault, TaskVault Offline-First Client Implementation Plan (+12 more)

### Community 13 - "Add/Edit Task ViewModel"
Cohesion: 0.19
Nodes (5): AddEditTaskUiState, AddEditTaskViewModel, StateFlow, ViewModel, AddEditTaskViewModelTest

### Community 14 - "Task Detail ViewModel"
Cohesion: 0.14
Nodes (6): MutableStateFlow, StateFlow, ViewModel, TaskDetailUiState, TaskDetailViewModel, TaskDetailViewModelTest

### Community 15 - "Default Task Repository"
Cohesion: 0.17
Nodes (3): DefaultTaskRepository, Flow, bindTaskRepository()

### Community 16 - "Task Model & Statistics"
Cohesion: 0.18
Nodes (5): Task, getActiveAndCompletedStats(), StatsResult, TaskMappingExtTest, StatisticsUtilsTest

### Community 17 - "Offline-First Sync Design"
Cohesion: 0.13
Nodes (18): DefaultTaskRepository Write Path (write-local, enqueue-op, request-sync), LocalTask Room Entity (schema v2 with status/priority/serverId/timestamps), Offline-First Mutation Queue Pattern, PendingOpDao, PendingOpEntity (pending_ops table), Railway FastAPI task-api Backend (prject-cv-production.up.railway.app), Room as Single Source of Truth, SyncEngine (drain + pull) (+10 more)

### Community 18 - "ViewModel Unit Tests"
Cohesion: 0.17
Nodes (5): TodoDestinationsArgs, StatisticsViewModelTest, Description, MainCoroutineRule, TestWatcher

### Community 19 - "Statistics ViewModel"
Cohesion: 0.20
Nodes (9): StateFlow, ViewModel, StatisticsUiState, StatisticsViewModel, Async, Error, Loading, Success (+1 more)

### Community 21 - "Dependency Renovation Config"
Cohesion: 0.18
Nodes (10): config:base, :dependencyDashboard, group:all, main, schedule:daily, baseBranches, commitMessageExtra, extends (+2 more)

### Community 22 - "App Screenshots"
Cohesion: 0.31
Nodes (10): Checkbox Rows With Strikethrough For Completed Tasks, Jetpack Compose State-Learning Todo Sample App Context, Feather Illustration Empty-State Placeholder Pattern, Green Floating Action Button As Add/Create Action, Edit Task Form: Title Plus Bullet-List Notes And Save Checkmark FAB, Todo App Three-Panel Screenshot Collage, Snackbar Feedback 'Task marked complete', Edit Task Screen With Keyboard Open (+2 more)

### Community 24 - "Custom Test Runner"
Cohesion: 0.43
Nodes (5): AndroidJUnitRunner, ClassLoader, CustomTestRunner, Application, Context

### Community 25 - "Application Class"
Cohesion: 0.53
Nodes (4): Application, TaskVaultApplication, Configuration, HiltWorkerFactory

### Community 26 - "Sync Worker"
Cohesion: 0.50
Nodes (3): SyncWorker, CoroutineWorker, Result

### Community 27 - "Launcher Icon xhdpi"
Cohesion: 0.40
Nodes (5): Android App Launcher Icon, Launcher Icon Artwork (visual detail unresolved), ic_launcher.png (xhdpi), mipmap Resource Directory, xhdpi Density Variant (~96px)

### Community 28 - "Launcher Icon xxhdpi"
Cohesion: 0.60
Nodes (5): TaskVault Launcher Icon (xxhdpi), White Feather Quill Glyph, Green Rounded-Square Background, TaskVault App Identity Marker, xxhdpi Density Variant Role (48px bucket in mipmap set)

### Community 29 - "Launcher Icon xxxhdpi"
Cohesion: 0.50
Nodes (5): Android Studio Default Project Template Icon, Green Rounded-Square Background with Teal Corner Accents, Launcher Icon (mipmap-xxxhdpi), Quill / Feather Pen Glyph, Highest-Density Launcher Icon Variant (192dp xxxhdpi)

### Community 30 - "App Demo GIF"
Cohesion: 0.60
Nodes (5): 'You have no TO-DOs!' Quill Illustration, Green Add Floating Action Button, Main Screen - Empty State, TO-DO Notes App, Toolbar 'TO-DO Notes'

### Community 31 - "Trash Icon Assets"
Cohesion: 0.50
Nodes (4): Delete Item Action, Trash Icon PNG (green trash can with upward arrow), Restore From Trash Action, Rationale: serves as delete/remove visual affordance in app UI

### Community 32 - "Launcher Icon hdpi"
Cohesion: 0.50
Nodes (4): HDPI Launcher Icon, White Feather / Quill Motif, Green Rounded-Square Background, HDPI Density Variant Role (Android Resource Qualifier)

### Community 33 - "Launcher Icon mdpi"
Cohesion: 0.67
Nodes (4): Green Android Robot Head Visual, Android Studio Default Template Icon, Launcher Icon PNG (mdpi), MDPI Density Variant (~48x48px)

### Community 34 - "Gradle Wrapper Scripts"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 35 - "Brand Logo Assets"
Cohesion: 0.67
Nodes (3): App Branding Asset (Fill-Free Logo Variant for Light Backgrounds), Logo No Fill - Green Quill Feather Icon (PNG), Stylized Quill Feather Motif (Mint-to-Green Gradient)

### Community 36 - "Auth & Server Config"
Cohesion: 0.67
Nodes (3): AuthInterceptor (Bearer token injection), BaseUrlInterceptor (dynamic base URL rewrite), DataStore Settings Store (ServerConfig: baseUrl + token)

## Ambiguous Edges - Review These
- `mock/prod Product Flavors` → `TaskVault Offline-First Client Design Spec`  [AMBIGUOUS]
  docs/superpowers/specs/2026-08-22-taskvault-offline-client-design.md · relation: references
- `Trash Icon PNG (green trash can with upward arrow)` → `Restore From Trash Action`  [AMBIGUOUS]
  app/src/main/res/drawable/trash_icon.png · relation: semantically_similar_to
- `ic_launcher.png (xhdpi)` → `Launcher Icon Artwork (visual detail unresolved)`  [AMBIGUOUS]
  app/src/main/res/mipmap-xhdpi/ic_launcher.png · relation: references
- `White Feather Quill Glyph` → `TaskVault App Identity Marker`  [AMBIGUOUS]
  app/src/main/res/mipmap-xxhdpi/ic_launcher.png · relation: semantically_similar_to

## Knowledge Gaps
- **48 isolated node(s):** `TodoScreens`, `LOW`, `MEDIUM`, `HIGH`, `IN_PROGRESS` (+43 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **3 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `mock/prod Product Flavors` and `TaskVault Offline-First Client Design Spec`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `Trash Icon PNG (green trash can with upward arrow)` and `Restore From Trash Action`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `ic_launcher.png (xhdpi)` and `Launcher Icon Artwork (visual detail unresolved)`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `White Feather Quill Glyph` and `TaskVault App Identity Marker`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **Why does `TaskRepository` connect `Add/Edit Screen Tests` to `Add/Edit Task UI`, `Network Interceptors`, `Tasks Screen Tests`, `Shared Test Fakes`, `Tasks List ViewModel`, `Add/Edit Task ViewModel`, `Task Detail ViewModel`, `Default Task Repository`, `Statistics ViewModel`?**
  _High betweenness centrality (0.229) - this node is a cross-community bridge._
- **Why does `Task` connect `Task Model & Statistics` to `Add/Edit Screen Tests`, `Add/Edit Task UI`, `Settings UI`, `Shared Test Fakes`, `Tasks List ViewModel`, `Add/Edit Task ViewModel`, `Task Detail ViewModel`, `Default Task Repository`, `ViewModel Unit Tests`, `Statistics ViewModel`, `Tasks ViewModel Tests`?**
  _High betweenness centrality (0.092) - this node is a cross-community bridge._
- **Why does `FakeTaskRepository` connect `Shared Test Fakes` to `Add/Edit Screen Tests`, `Add/Edit Task ViewModel`, `Task Detail ViewModel`, `Task Model & Statistics`, `ViewModel Unit Tests`, `Tasks ViewModel Tests`?**
  _High betweenness centrality (0.069) - this node is a cross-community bridge._
# Graph Report - p4  (2026-08-30)

## Corpus Check
- 29 files · ~134,451 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1324 nodes · 2446 edges · 110 communities (68 shown, 42 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 89 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Task DTO & Sync Engine
- Offline-First Repo & Auth Models
- Default Task Repository
- Add/Edit Task ViewModel
- Alarm & Sync Schedulers
- Room DAO Tests
- Pending Op DAO Tests
- Room Type Converters
- Task Detail Screen Tests
- Tasks Screen Tests
- Tasks ViewModel & Filters
- Background Workers
- Team Session & View Tests
- Auth ViewModel
- Fake Task Repository
- Notes UI
- Task Repository Contract
- Clock Injection & Mapping
- DataStore Settings Repository
- Auth Requests
- Task Model & Notes
- Navigation Destinations
- Auth Models (Member/Me)
- Tasks Screen UI
- Offline-First Sync Design
- Login Screen & Activity
- Assignees & Team UI
- Team ViewModel
- Statistics & Detail Screen Tests
- Note Requests & API
- Connectivity Watcher
- Instrument Serif License
- Add/Edit Task Screen
- ViewModel Unit Tests
- Team ViewModel Tests
- Sync Stats ViewModel
- Theme Mode
- Settings ViewModel
- Task Detail ViewModel
- Task Filter Types
- Task Detail ViewModel Tests
- DataStore Settings Tests
- Widget & LocalTask Tests
- Task API Service
- Settings Screen UI
- Dependency Renovation Config
- Member DTO
- Legacy App Screenshots
- App Navigation Tests
- CI Pipeline & Project History
- Launcher Icons
- Custom Test Runner
- Form Fields & Pickers
- PendingOp Classifier
- Settings DI Module
- DueDates Utilities & Tests
- Design Docs (due_at/reminders)
- Add/Edit Screen Tests
- Task Status Update
- Note Results
- Sync Outcome Types
- Navigation Graph
- Auth Interceptor Class
- Base URL Interceptor Class
- Brand Logo Assets
- Issue Log: Widget/DAO
- Refactor Architecture Notes
- Legacy Demo GIF
- Launcher Icon hdpi
- Trash Icon Assets
- Gradle Wrapper Scripts
- Interceptors & Server Config
- Server Config File
- Issue ISS-02 Mapping
- Issue ISS-04 Sync Engine
- Edit Priority Screenshot
- Tasks List Screenshots
- Refactor: Frozen Payload Bug
- Component Activity A
- TaskPriority Enum
- TaskStatus Enum
- PendingOpDao Interface
- BroadcastReceiver
- Intent
- Network Layer
- MutableStateFlow Type
- Component Activity B
- TaskVault Launcher Icon
- Instrument Serif License Ref
- Response Type
- Application Node
- CLA Requirement
- Dev Label
- Server Settings Screenshot
- Drawer State
- FakeTaskDao Class
- ServerConfig Class
- Source Dir
- Task Domain Model
- TaskDto Model
- TaskPayload Model
- TodoNavigation Actions

## God Nodes (most connected - your core abstractions)
1. `TaskDto` - 63 edges
2. `Task` - 52 edges
3. `FakeTaskRepository` - 44 edges
4. `TasksViewModel` - 38 edges
5. `FakeSettingsRepository` - 38 edges
6. `TaskRepository` - 37 edges
7. `AddEditTaskViewModel` - 33 edges
8. `TasksViewModelTest` - 31 edges
9. `TaskApiService` - 29 edges
10. `SyncEngineTest` - 28 edges

## Surprising Connections (you probably didn't know these)
- `Transactional Room Mutations` --semantically_similar_to--> `Offline-First Mutation Queue`  [INFERRED] [semantically similar]
  docs/superpowers/plans/2026-08-25-taskvault-restructure.md → README.md
- `FakeSettingsRepository` --references--> `ThemeMode`  [EXTRACTED]
  shared-test/src/main/java/dev/boudy04/taskvault/settings/FakeSettingsRepository.kt → app/src/main/java/dev/boudy04/taskvault/settings/SettingsRepository.kt
- `FakeTaskRepository` --references--> `Task`  [EXTRACTED]
  shared-test/src/main/java/dev/boudy04/taskvault/data/FakeTaskRepository.kt → app/src/main/java/dev/boudy04/taskvault/data/Task.kt
- `TasksViewModelTest` --references--> `FakeTaskRepository`  [EXTRACTED]
  app/src/test/java/dev/boudy04/taskvault/tasks/TasksViewModelTest.kt → shared-test/src/main/java/dev/boudy04/taskvault/data/FakeTaskRepository.kt
- `TasksViewModelTest` --calls--> `MainCoroutineRule`  [EXTRACTED]
  app/src/test/java/dev/boudy04/taskvault/tasks/TasksViewModelTest.kt → shared-test/src/main/java/dev/boudy04/taskvault/MainCoroutineRule.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **CI: unit-lint Job + Emulator instrumented Job** — _github_workflows_ci_workflow, _github_workflows_ci_unit_lint_job, _github_workflows_ci_instrumented_job [EXTRACTED 1.00]
- **Due-At Reminder Flow** — docs_superpowers_specs_2026_08_23_due_at_reminders_design_due_at_data_model, docs_superpowers_specs_2026_08_23_due_at_reminders_design_exact_alarm_reminders [EXTRACTED 1.00]
- **Offline-First Architecture Concepts** — readme_offline_first_mutation_queue, readme_room_source_of_truth, readme_last_write_wins_reconcile, readme_architecture_pipeline [EXTRACTED 1.00]
- **Offline-First Sync Pipeline (write-local -> enqueue -> drain -> pull)** — docs_superpowers_plans_2026_08_22_taskvault_offline_client_default_task_repository_write_path, docs_superpowers_plans_2026_08_22_taskvault_offline_client_pending_op_entity, docs_superpowers_plans_2026_08_22_taskvault_offline_client_pending_op_dao, docs_superpowers_plans_2026_08_22_taskvault_offline_client_sync_scheduler, docs_superpowers_plans_2026_08_22_taskvault_offline_client_sync_worker, docs_superpowers_plans_2026_08_22_taskvault_offline_client_sync_engine, docs_superpowers_plans_2026_08_22_taskvault_offline_client_task_api_service, docs_superpowers_plans_2026_08_22_taskvault_offline_client_local_task_entity [EXTRACTED 1.00]
- **Retrofit Network Stack (dynamic base URL + Bearer auth + kotlinx-serialization)** — docs_superpowers_plans_2026_08_22_taskvault_offline_client_task_api_service, docs_superpowers_plans_2026_08_22_taskvault_offline_client_task_dto, docs_superpowers_plans_2026_08_22_taskvault_offline_client_auth_interceptor, docs_superpowers_plans_2026_08_22_taskvault_offline_client_base_url_interceptor, docs_superpowers_plans_2026_08_22_taskvault_offline_client_datastore_settings_repository [EXTRACTED 1.00]

## Communities (110 total, 42 thin omitted)

### Community 0 - "Task DTO & Sync Engine"
Cohesion: 0.06
Nodes (20): TaskDto, SyncEngine, TaskMappingExtTest, FakeApi, httpException(), AdminVerifyRequest, AuthRequest, FakePendingOpDao (+12 more)

### Community 1 - "Offline-First Repo & Auth Models"
Cohesion: 0.06
Nodes (19): AdminVerifyRequest, AuthRequest, FakePendingOpDao, LocalTask, MemberLoginRequest, MemberRequest, NoteRequest, ReminderScheduler (+11 more)

### Community 2 - "Default Task Repository"
Cohesion: 0.05
Nodes (35): DefaultTaskRepository, Flow, LocalTask, PendingOpType, Task, TaskPriority, TaskRepository, TaskStatus (+27 more)

### Community 3 - "Add/Edit Task ViewModel"
Cohesion: 0.07
Nodes (12): AddEditTaskUiState, AddEditTaskViewModel, StateFlow, TaskPriority, ViewModel, Session, AddEditTaskViewModelTest, FakeApi (+4 more)

### Community 4 - "Alarm & Sync Schedulers"
Cohesion: 0.06
Nodes (29): AlarmManager, AlarmReminderScheduler, bindReminderScheduler(), bindSyncScheduler(), bindTaskRepository(), DatabaseModule, Context, ReminderScheduler (+21 more)

### Community 5 - "Room DAO Tests"
Cohesion: 0.06
Nodes (17): TaskDaoTest, ReminderEngine, LocalTask, Flow, LocalTask, TaskDao, PendingOpDao, ToDoDatabase (+9 more)

### Community 6 - "Pending Op DAO Tests"
Cohesion: 0.05
Nodes (14): PendingOpDaoTest, Flow, PendingOpEntity, PendingOpState, PendingOpDao, NotificationHelper, BroadcastReceiver, Context (+6 more)

### Community 7 - "Room Type Converters"
Cohesion: 0.08
Nodes (18): Converters, PendingOpEntity, PendingOpState, PENDING, RUNNING, PendingOpType, CREATE, DELETE (+10 more)

### Community 8 - "Task Detail Screen Tests"
Cohesion: 0.12
Nodes (21): TaskDetailScreenTest, Modifier, SnackbarHostState, StatisticsContent(), StatisticsContentEmptyPreview(), StatisticsContentPreview(), StatisticsScreen(), EditTaskContent() (+13 more)

### Community 10 - "Tasks ViewModel & Filters"
Cohesion: 0.12
Nodes (9): FilteringUiInfo, FilterState, Flow, StateFlow, ViewModel, ListExtras, TaskLists, TasksUiState (+1 more)

### Community 11 - "Background Workers"
Cohesion: 0.14
Nodes (15): ReminderWorker, SyncWorker, TaskVaultWidget, TaskVaultWidgetEntryPoint, TaskVaultWidgetReceiver, toWidgetState(), WidgetState, WidgetUpdater (+7 more)

### Community 13 - "Auth ViewModel"
Cohesion: 0.12
Nodes (6): AuthViewModel, StateFlow, ViewModel, LoginUiState, AuthViewModelTest, httpException()

### Community 14 - "Fake Task Repository"
Cohesion: 0.13
Nodes (4): FakeTaskRepository, Flow, StateFlow, TaskPriority

### Community 15 - "Notes UI"
Cohesion: 0.15
Nodes (15): dev, Task, TaskNote, NoteRow(), NoteSheet(), AssigneeBadges(), DueChip(), dev (+7 more)

### Community 16 - "Task Repository Contract"
Cohesion: 0.11
Nodes (4): Flow, TaskPriority, TaskRepository, RepositoryTestModule

### Community 17 - "Clock Injection & Mapping"
Cohesion: 0.10
Nodes (22): Clock Injection, DefaultTaskRepository, ModelMappingExt, PendingOpClassifier, ReminderBootReceiver, ReminderEngine, SyncEngine, TaskDto (+14 more)

### Community 18 - "DataStore Settings Repository"
Cohesion: 0.14
Nodes (5): DataStoreSettingsRepository, Keys, Flow, ServerConfig, SettingsRepository

### Community 19 - "Auth Requests"
Cohesion: 0.16
Nodes (3): AuthRequest, AuthResponse, FakeApi

### Community 20 - "Task Model & Notes"
Cohesion: 0.16
Nodes (5): Task, TaskNote, getActiveAndCompletedStats(), StatsResult, StatisticsUtilsTest

### Community 21 - "Navigation Destinations"
Cohesion: 0.19
Nodes (12): TodoDestinations, TodoNavigationActions, TodoScreens, AppDrawer(), AppModalDrawer(), DrawerButton(), DrawerHeader(), CoroutineScope (+4 more)

### Community 22 - "Auth Models (Member/Me)"
Cohesion: 0.22
Nodes (5): AdminVerifyRequest, MemberLoginRequest, MemberLoginResponse, MeResponse, NoteDto

### Community 23 - "Tasks Screen UI"
Cohesion: 0.20
Nodes (17): dev, Modifier, SnackbarHostState, Task, TasksFilterType, TasksSort, RequestNotificationsIfNeeded(), SectionEmpty() (+9 more)

### Community 24 - "Offline-First Sync Design"
Cohesion: 0.13
Nodes (18): DefaultTaskRepository Write Path (write-local, enqueue-op, request-sync), LocalTask Room Entity (schema v2 with status/priority/serverId/timestamps), Offline-First Mutation Queue Pattern, PendingOpDao, PendingOpEntity (pending_ops table), Railway FastAPI task-api Backend (prject-cv-production.up.railway.app), Room as Single Source of Truth, SyncEngine (drain + pull) (+10 more)

### Community 25 - "Login Screen & Activity"
Cohesion: 0.21
Nodes (13): Modifier, LoginScreen(), StateFlow, ViewModel, LockGate(), MainContent(), TodoActivity, TodoViewModel (+5 more)

### Community 26 - "Assignees & Team UI"
Cohesion: 0.17
Nodes (13): androidx, AssigneesSheet(), MemberDto, Modifier, TeamSection(), FilterMenu(), FiltersSheet(), dev (+5 more)

### Community 27 - "Team ViewModel"
Cohesion: 0.21
Nodes (5): StateFlow, ViewModel, TeamUiState, TeamViewModel, TeamViewModelTest

### Community 28 - "Statistics & Detail Screen Tests"
Cohesion: 0.20
Nodes (4): StatisticsScreenTest, SemanticsNodeInteraction, HiltTestActivity, ComponentActivity

### Community 30 - "Connectivity Watcher"
Cohesion: 0.24
Nodes (8): ConnectivityWatcher, ConnectivityManager, Network, Application, TaskVaultApplication, Configuration, HiltWorkerFactory, WidgetUpdater

### Community 31 - "Instrument Serif License"
Cohesion: 0.18
Nodes (14): Condition 2: Bundling/Redistribution Requires Copyright Notice And License Copy, Condition 4: Holder/Author Names Not Used For Promotion Of Modified Versions, Condition 1: Font Software May Not Be Sold By Itself, Condition 3: Modified Versions May Not Use Reserved Font Names Without Permission, Condition 5: Font Must Remain Under OFL; Documents Created With It Are Exempt, Outfit Font License File, Definition: Modified Version, The Outfit Project Authors (Copyright Holder) (+6 more)

### Community 32 - "Add/Edit Task Screen"
Cohesion: 0.26
Nodes (9): AddEditTaskViewModel, AddEditTaskContent(), AddEditTaskScreen(), MemberDto, Modifier, SnackbarHostState, TaskPriority, SnackbarHostState (+1 more)

### Community 33 - "ViewModel Unit Tests"
Cohesion: 0.21
Nodes (4): StatisticsViewModelTest, Description, MainCoroutineRule, TestWatcher

### Community 35 - "Sync Stats ViewModel"
Cohesion: 0.26
Nodes (6): SyncStats, StateFlow, ViewModel, StatisticsUiState, StatisticsViewModel, Async

### Community 36 - "Theme Mode"
Cohesion: 0.23
Nodes (8): ThemeMode, DARK, LIGHT, SYSTEM, outfitTypography(), resolvesDark(), TaskVaultTheme(), Typography

### Community 37 - "Settings ViewModel"
Cohesion: 0.21
Nodes (4): StateFlow, ViewModel, SettingsUiState, SettingsViewModel

### Community 38 - "Task Detail ViewModel"
Cohesion: 0.24
Nodes (5): MutableStateFlow, StateFlow, ViewModel, TaskDetailUiState, TaskDetailViewModel

### Community 39 - "Task Filter Types"
Cohesion: 0.17
Nodes (9): TasksFilterType, ACTIVE_TASKS, ALL_TASKS, COMPLETED_TASKS, TasksSort, NEAREST_DUE, NEWEST, OLDEST (+1 more)

### Community 40 - "Task Detail ViewModel Tests"
Cohesion: 0.20
Nodes (3): TodoDestinationsArgs, TaskDetailViewModel, TaskDetailViewModelTest

### Community 42 - "Widget & LocalTask Tests"
Cohesion: 0.29
Nodes (3): WidgetStateTest, TaskPriority, TaskStatus

### Community 44 - "Settings Screen UI"
Cohesion: 0.36
Nodes (9): Modifier, SnackbarHostState, SectionCard(), SettingsContent(), SettingsContentPreview(), SettingsScreen(), verifyThen(), BiometricPrompt (+1 more)

### Community 45 - "Dependency Renovation Config"
Cohesion: 0.18
Nodes (10): config:base, :dependencyDashboard, group:all, main, schedule:daily, baseBranches, commitMessageExtra, extends (+2 more)

### Community 47 - "Legacy App Screenshots"
Cohesion: 0.31
Nodes (10): Checkbox Rows With Strikethrough For Completed Tasks, Jetpack Compose State-Learning Todo Sample App Context, Feather Illustration Empty-State Placeholder Pattern, Green Floating Action Button As Add/Create Action, Edit Task Form: Title Plus Bullet-List Notes And Save Checkmark FAB, Todo App Three-Panel Screenshot Collage, Snackbar Feedback 'Task marked complete', Edit Task Screen With Keyboard Open (+2 more)

### Community 49 - "CI Pipeline & Project History"
Cohesion: 0.29
Nodes (8): instrumented CI Job (Android Emulator, connectedDebugAndroidTest), unit-lint CI Job (lintDebug + testDebugUnitTest), TaskVault CI Workflow (.github/workflows/ci.yml), Rebrand to dev.boudy04.taskvault / TaskVault, TaskVault Offline-First Client Implementation Plan, TaskVault (boudy04/taskvault), Non-Goals (no multi-user auth, no periodic sync, no pagination), TaskVault Offline-First Client Design Spec

### Community 50 - "Launcher Icons"
Cohesion: 0.25
Nodes (8): Orange Feather/Quill Motif on Dark Rounded Square, App Launcher Icon (xhdpi), Writing/Note-Taking App Identity, App Launcher Icon (Quill Feather, xxhdpi), Dark Navy Rounded-Square Background, Orange-Red Quill Feather, Dark Rounded Square Background, Orange Feather Graphic

### Community 51 - "Custom Test Runner"
Cohesion: 0.43
Nodes (5): AndroidJUnitRunner, ClassLoader, CustomTestRunner, Application, Context

### Community 52 - "Form Fields & Pickers"
Cohesion: 0.43
Nodes (5): FieldRow(), Modifier, DuePicker(), TaskPriority, PriorityPicker()

### Community 54 - "Settings DI Module"
Cohesion: 0.48
Nodes (4): Context, SettingsModule, DataStore, Preferences

### Community 56 - "Design Docs (due_at/reminders)"
Cohesion: 0.33
Nodes (6): Google Samples Packaging Metadata, TaskVault Restructuring Implementation Plan, due_at Data Model, Due Dates & Reminders Design Addendum, Exact-Alarm Reminder Scheduling, TaskVault Project Overview

### Community 59 - "Note Results"
Cohesion: 0.33
Nodes (4): NoteResult, ADDED, FAILED, FORBIDDEN

### Community 60 - "Sync Outcome Types"
Cohesion: 0.33
Nodes (5): SyncOutcome, CONNECTIVITY_RETRY, FAILURE, RETRY, SUCCESS

### Community 61 - "Navigation Graph"
Cohesion: 0.60
Nodes (5): CoroutineScope, DrawerState, Modifier, TodoNavGraph(), NavHostController

### Community 62 - "Auth Interceptor Class"
Cohesion: 0.70
Nodes (3): AuthInterceptor, Interceptor, Response

### Community 63 - "Base URL Interceptor Class"
Cohesion: 0.70
Nodes (3): BaseUrlInterceptor, Interceptor, Response

### Community 64 - "Brand Logo Assets"
Cohesion: 0.40
Nodes (5): Android App Branding / Launcher-Adjacent Logo, logo_no_fill.png Logo Asset, Stylized Orange Feather (Quill) Glyph, Solid Orange Monochrome Palette on Transparent Background, Quill Feather as Writing / Note-Taking Metaphor

### Community 65 - "Issue Log: Widget/DAO"
Cohesion: 0.60
Nodes (5): TaskDao, updateCompleted Dead Code, Widget Bypasses Repository, FakeTaskDao, ISS-05

### Community 66 - "Refactor Architecture Notes"
Cohesion: 0.40
Nodes (5): Transactional Room Mutations, Compose to Railway Architecture Pipeline, Last-Write-Wins Reconcile, Offline-First Mutation Queue, Room as Source of Truth

### Community 67 - "Legacy Demo GIF"
Cohesion: 0.60
Nodes (5): 'You have no TO-DOs!' Quill Illustration, Green Add Floating Action Button, Main Screen - Empty State, TO-DO Notes App, Toolbar 'TO-DO Notes'

### Community 68 - "Launcher Icon hdpi"
Cohesion: 0.83
Nodes (4): Android Launcher Icon (mipmap-hdpi density), Application Visual Identity and Branding, Android Launcher Icon HDPI - Orange Feather on Dark Rounded Square, Orange-Red Feather Graphic on Dark Background

### Community 69 - "Trash Icon Assets"
Cohesion: 0.50
Nodes (4): Delete Item Action, Trash Icon PNG (green trash can with upward arrow), Restore From Trash Action, Rationale: serves as delete/remove visual affordance in app UI

### Community 70 - "Gradle Wrapper Scripts"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 71 - "Interceptors & Server Config"
Cohesion: 0.67
Nodes (3): AuthInterceptor (Bearer token injection), BaseUrlInterceptor (dynamic base URL rewrite), DataStore Settings Store (ServerConfig: baseUrl + token)

## Ambiguous Edges - Review These
- `Restore From Trash Action` → `Trash Icon PNG (green trash can with upward arrow)`  [AMBIGUOUS]
  app/src/main/res/drawable/trash_icon.png · relation: semantically_similar_to

## Knowledge Gaps
- **96 isolated node(s):** `TaskNote`, `NoteDto`, `ListExtras`, `TodoScreens`, `Keys` (+91 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **42 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Restore From Trash Action` and `Trash Icon PNG (green trash can with upward arrow)`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **Why does `TaskDto` connect `Task DTO & Sync Engine` to `Offline-First Repo & Auth Models`, `Default Task Repository`, `Add/Edit Task ViewModel`, `Alarm & Sync Schedulers`, `Team ViewModel Tests`, `Widget & LocalTask Tests`, `Task API Service`, `Auth Requests`, `Auth Models (Member/Me)`, `Task Status Update`, `Note Requests & API`?**
  _High betweenness centrality (0.148) - this node is a cross-community bridge._
- **Why does `SettingsRepository` connect `DataStore Settings Repository` to `Add/Edit Task ViewModel`, `Theme Mode`, `Tasks ViewModel & Filters`, `Background Workers`, `Auth ViewModel`, `Settings DI Module`, `Connectivity Watcher`, `Login Screen & Activity`, `Auth Interceptor Class`, `Base URL Interceptor Class`?**
  _High betweenness centrality (0.123) - this node is a cross-community bridge._
- **Why does `Task` connect `Task Model & Notes` to `ViewModel Unit Tests`, `Sync Stats ViewModel`, `Add/Edit Task ViewModel`, `Task Detail ViewModel`, `Task Filter Types`, `Task Detail Screen Tests`, `Task Detail ViewModel Tests`, `Tasks ViewModel & Filters`, `Widget & LocalTask Tests`, `Team Session & View Tests`, `Fake Task Repository`, `Task Repository Contract`, `Auth Models (Member/Me)`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **What connects `TaskNote`, `NoteDto`, `ListExtras` to the rest of the system?**
  _96 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Task DTO & Sync Engine` be split into smaller, more focused modules?**
  _Cohesion score 0.05898021308980213 - nodes in this community are weakly interconnected._
- **Should `Offline-First Repo & Auth Models` be split into smaller, more focused modules?**
  _Cohesion score 0.0594679186228482 - nodes in this community are weakly interconnected._
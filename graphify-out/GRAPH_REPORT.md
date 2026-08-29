# Graph Report - p4  (2026-08-25)

## Corpus Check
- 101 files · ~131,766 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1195 nodes · 2417 edges · 103 communities (61 shown, 42 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 81 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- UI Screens & Theming
- Task Repository Core
- Sync Engine & DAO Tests
- Activity Auth & Login UI
- Add Edit Task Flow
- Widget & Reminder Alerts
- Settings Repo & Interceptors
- Pending Op Queue Tests
- Team Management
- Local Task Entity & Tests
- Offline First Repo Tests
- Task Model & Filtering
- Auth ViewModel Tests
- Tasks List ViewModel
- Shared Test Fakes
- Repository Interface
- Auth API Models
- Settings Test Fakes
- Network Task DTOs
- Navigation Drawer Routes
- Offline Client Design Docs
- Tasks Screen UI Tests
- Auth Request Response
- Fake Task DAO
- DI Network Module
- Todotheme.Kt
- Sil Open Font License
- Tododatabase
- Recordingapi
- .Createtask()
- Taskstatus
- Memberdto
- Taskapiservice
- Statisticsviewmodel
- Datamodules.Kt
- Taskdetailviewmodel
- Taskdetailviewmodeltest
- .Repo()
- Tasksviewmodeltest.Kt
- Renovate.Json
- Reminderscheduler
- Syncoutcome
- Edit Task Screen With
- Appnavigationtest
- Taskssort
- Fakeapi
- Taskvault Ci Workflow (.Github
- Context
- Getactiveandcompletedstats()
- App Launcher Icon (Quill
- Customtestrunner.Kt
- Addedittaskscreentest
- Connectivitymanager
- Tasksfiltertype
- Taskvaultapplication
- Settingsviewmodeltest
- Duedatestest
- Due Dates & Reminders
- Noteresult
- Reminderbootreceiver.Kt
- Syncscheduler
- Maincoroutinerule
- Taskdetailscreentest
- Stylized Orange Feather (Quill)
- Statisticsviewmodeltest
- Offline First Mutation Queue
- Main Screen Empty State
- Application Visual Identity And
- Trash Icon Png (Green
- Gradlew
- Recordingsyncscheduler
- Recordingreminderscheduler
- Datastore Settings Store (Serverconfig:
- Serverconfig.Kt
- Edit Priority Screenshot
- Tasks List Screenshot (Dark
- Domain Local Remote Model
- Componentactivity
- Mutablestateflow
- Componentactivity
- Taskvault Launcher Icon
- Instrument Serif Font License
- Application
- Contributor License Agreement Requirement
- Server Settings Screenshot
- Drawerstate
- Faketaskdao
- Pendingoptype
- Serverconfig
- Task
- Taskapiservice
- Taskdto
- Taskpayload
- Tasksviewmodel
- Todonavigationactions

## God Nodes (most connected - your core abstractions)
1. `Task` - 66 edges
2. `TaskDto` - 57 edges
3. `LocalTask` - 49 edges
4. `FakeTaskRepository` - 44 edges
5. `FakeSettingsRepository` - 43 edges
6. `TaskRepository` - 40 edges
7. `TasksViewModel` - 39 edges
8. `TaskApiService` - 37 edges
9. `AddEditTaskViewModel` - 34 edges
10. `MemberDto` - 31 edges

## Surprising Connections (you probably didn't know these)
- `Transactional Room Mutations` --semantically_similar_to--> `Offline-First Mutation Queue`  [INFERRED] [semantically similar]
  docs/superpowers/plans/2026-08-25-taskvault-restructure.md → README.md
- `FakeTaskRepository` --references--> `Task`  [EXTRACTED]
  shared-test/src/main/java/dev/boudy04/taskvault/data/FakeTaskRepository.kt → app/src/main/java/dev/boudy04/taskvault/data/Task.kt
- `FakeTaskRepository` --implements--> `TaskRepository`  [EXTRACTED]
  shared-test/src/main/java/dev/boudy04/taskvault/data/FakeTaskRepository.kt → app/src/main/java/dev/boudy04/taskvault/data/TaskRepository.kt
- `FakeTaskDao` --references--> `LocalTask`  [EXTRACTED]
  shared-test/src/main/java/dev/boudy04/taskvault/data/source/local/FakeTaskDao.kt → app/src/main/java/dev/boudy04/taskvault/data/source/local/LocalTask.kt
- `FakeTaskDao` --implements--> `TaskDao`  [EXTRACTED]
  shared-test/src/main/java/dev/boudy04/taskvault/data/source/local/FakeTaskDao.kt → app/src/main/java/dev/boudy04/taskvault/data/source/local/TaskDao.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Offline-First Architecture Concepts** — readme_offline_first_mutation_queue, readme_room_source_of_truth, readme_last_write_wins_reconcile, readme_architecture_pipeline [EXTRACTED 1.00]
- **Due-At Reminder Flow** — docs_superpowers_specs_2026_08_23_due_at_reminders_design_due_at_data_model, docs_superpowers_specs_2026_08_23_due_at_reminders_design_exact_alarm_reminders [EXTRACTED 1.00]
- **CI: unit-lint Job + Emulator instrumented Job** — _github_workflows_ci_workflow, _github_workflows_ci_unit_lint_job, _github_workflows_ci_instrumented_job [EXTRACTED 1.00]
- **Offline-First Sync Pipeline (write-local -> enqueue -> drain -> pull)** — docs_superpowers_plans_2026_08_22_taskvault_offline_client_default_task_repository_write_path, docs_superpowers_plans_2026_08_22_taskvault_offline_client_pending_op_entity, docs_superpowers_plans_2026_08_22_taskvault_offline_client_pending_op_dao, docs_superpowers_plans_2026_08_22_taskvault_offline_client_sync_scheduler, docs_superpowers_plans_2026_08_22_taskvault_offline_client_sync_worker, docs_superpowers_plans_2026_08_22_taskvault_offline_client_sync_engine, docs_superpowers_plans_2026_08_22_taskvault_offline_client_task_api_service, docs_superpowers_plans_2026_08_22_taskvault_offline_client_local_task_entity [EXTRACTED 1.00]
- **Retrofit Network Stack (dynamic base URL + Bearer auth + kotlinx-serialization)** — docs_superpowers_plans_2026_08_22_taskvault_offline_client_task_api_service, docs_superpowers_plans_2026_08_22_taskvault_offline_client_task_dto, docs_superpowers_plans_2026_08_22_taskvault_offline_client_auth_interceptor, docs_superpowers_plans_2026_08_22_taskvault_offline_client_base_url_interceptor, docs_superpowers_plans_2026_08_22_taskvault_offline_client_datastore_settings_repository [EXTRACTED 1.00]

## Communities (103 total, 42 thin omitted)

### Community 0 - "UI Screens & Theming"
Cohesion: 0.05
Nodes (59): androidx, AddEditTaskContent(), AddEditTaskScreen(), AssigneesSheet(), DuePicker(), FieldRow(), Modifier, SnackbarHostState (+51 more)

### Community 1 - "Task Repository Core"
Cohesion: 0.06
Nodes (27): DefaultTaskRepository, Flow, TaskPriority, TaskStatus, copyFromDto(), joinIds(), joinNotes(), joinTags() (+19 more)

### Community 2 - "Sync Engine & DAO Tests"
Cohesion: 0.08
Nodes (21): Converters, PendingOpEntity, PendingOpState, PENDING, RUNNING, PendingOpType, CREATE, DELETE (+13 more)

### Community 3 - "Activity Auth & Login UI"
Cohesion: 0.08
Nodes (30): Modifier, LoginScreen(), Modifier, SnackbarHostState, SectionCard(), SettingsContent(), SettingsContentPreview(), SettingsScreen() (+22 more)

### Community 4 - "Add Edit Task Flow"
Cohesion: 0.09
Nodes (7): AddEditTaskUiState, AddEditTaskViewModel, StateFlow, TaskPriority, ViewModel, AddEditTaskViewModelTest, FakeApi

### Community 5 - "Widget & Reminder Alerts"
Cohesion: 0.08
Nodes (21): NotificationHelper, BroadcastReceiver, Context, Intent, ReminderReceiver, ReminderWorker, SyncWorker, TaskVaultWidget (+13 more)

### Community 6 - "Settings Repo & Interceptors"
Cohesion: 0.08
Nodes (15): AuthInterceptor, Interceptor, Response, BaseUrlInterceptor, Interceptor, Response, Context, SettingsModule (+7 more)

### Community 7 - "Pending Op Queue Tests"
Cohesion: 0.07
Nodes (9): PendingOpDaoTest, Flow, PendingOpEntity, PendingOpState, PendingOpDao, FakePendingOpDao, Flow, PendingOpEntity (+1 more)

### Community 8 - "Team Management"
Cohesion: 0.12
Nodes (7): StateFlow, ViewModel, TeamUiState, TeamViewModel, FakeMemberApi, Response, TeamViewModelTest

### Community 9 - "Local Task Entity & Tests"
Cohesion: 0.11
Nodes (4): TaskDaoTest, LocalTask, Flow, TaskDao

### Community 12 - "Auth ViewModel Tests"
Cohesion: 0.12
Nodes (6): AuthViewModel, StateFlow, ViewModel, LoginUiState, AuthViewModelTest, httpException()

### Community 13 - "Tasks List ViewModel"
Cohesion: 0.13
Nodes (8): FilterState, Flow, StateFlow, ViewModel, ListExtras, TaskLists, TasksUiState, TasksViewModel

### Community 14 - "Shared Test Fakes"
Cohesion: 0.13
Nodes (4): FakeTaskRepository, Flow, StateFlow, TaskPriority

### Community 15 - "Repository Interface"
Cohesion: 0.11
Nodes (4): Flow, TaskPriority, TaskRepository, RepositoryTestModule

### Community 16 - "Auth API Models"
Cohesion: 0.15
Nodes (3): AdminVerifyRequest, MeResponse, FakeApi

### Community 17 - "Settings Test Fakes"
Cohesion: 0.15
Nodes (5): Session, ViewOnlyRejections, FakeSettingsRepository, Flow, ServerConfig

### Community 18 - "Network Task DTOs"
Cohesion: 0.17
Nodes (4): TaskStatusUpdate, TaskDto, FakeApi, Response

### Community 19 - "Navigation Drawer Routes"
Cohesion: 0.19
Nodes (12): TodoDestinations, TodoNavigationActions, TodoScreens, AppDrawer(), AppModalDrawer(), DrawerButton(), DrawerHeader(), CoroutineScope (+4 more)

### Community 20 - "Offline Client Design Docs"
Cohesion: 0.13
Nodes (18): DefaultTaskRepository Write Path (write-local, enqueue-op, request-sync), LocalTask Room Entity (schema v2 with status/priority/serverId/timestamps), Offline-First Mutation Queue Pattern, PendingOpDao, PendingOpEntity (pending_ops table), Railway FastAPI task-api Backend (prject-cv-production.up.railway.app), Room as Single Source of Truth, SyncEngine (drain + pull) (+10 more)

### Community 24 - "DI Network Module"
Cohesion: 0.20
Nodes (6): NetworkModule, RetrofitNetworkDataSourceTest, Json, MockWebServer, OkHttpClient, Retrofit

### Community 25 - "Todotheme.Kt"
Cohesion: 0.20
Nodes (4): StatisticsScreenTest, SemanticsNodeInteraction, HiltTestActivity, ComponentActivity

### Community 26 - "Sil Open Font License"
Cohesion: 0.18
Nodes (14): Condition 2: Bundling/Redistribution Requires Copyright Notice And License Copy, Condition 4: Holder/Author Names Not Used For Promotion Of Modified Versions, Condition 1: Font Software May Not Be Sold By Itself, Condition 3: Modified Versions May Not Use Reserved Font Names Without Permission, Condition 5: Font Must Remain Under OFL; Documents Created With It Are Exempt, Outfit Font License File, Definition: Modified Version, The Outfit Project Authors (Copyright Holder) (+6 more)

### Community 27 - "Tododatabase"
Cohesion: 0.23
Nodes (6): PendingOpDao, ToDoDatabase, RoomDatabase, DatabaseTestModule, Context, PendingOpDao

### Community 30 - "Taskstatus"
Cohesion: 0.24
Nodes (3): WidgetStateTest, TaskPriority, TaskStatus

### Community 31 - "Memberdto"
Cohesion: 0.30
Nodes (3): MemberDto, MemberRequest, Response

### Community 33 - "Statisticsviewmodel"
Cohesion: 0.26
Nodes (6): SyncStats, StateFlow, ViewModel, StatisticsUiState, StatisticsViewModel, Async

### Community 34 - "Datamodules.Kt"
Cohesion: 0.23
Nodes (8): bindSyncScheduler(), bindTaskRepository(), DatabaseModule, PendingOpDao, SyncScheduler, AuthInterceptor, BaseUrlInterceptor, WorkManagerSyncScheduler

### Community 35 - "Taskdetailviewmodel"
Cohesion: 0.24
Nodes (5): MutableStateFlow, StateFlow, ViewModel, TaskDetailUiState, TaskDetailViewModel

### Community 36 - "Taskdetailviewmodeltest"
Cohesion: 0.20
Nodes (3): TodoDestinationsArgs, TaskDetailViewModel, TaskDetailViewModelTest

### Community 39 - "Renovate.Json"
Cohesion: 0.18
Nodes (10): config:base, :dependencyDashboard, group:all, main, schedule:daily, baseBranches, commitMessageExtra, extends (+2 more)

### Community 40 - "Reminderscheduler"
Cohesion: 0.29
Nodes (3): bindReminderScheduler(), AlarmReminderScheduler, ReminderScheduler

### Community 41 - "Syncoutcome"
Cohesion: 0.27
Nodes (6): SyncEngine, SyncOutcome, CONNECTIVITY_RETRY, FAILURE, RETRY, SUCCESS

### Community 42 - "Edit Task Screen With"
Cohesion: 0.31
Nodes (10): Checkbox Rows With Strikethrough For Completed Tasks, Jetpack Compose State-Learning Todo Sample App Context, Feather Illustration Empty-State Placeholder Pattern, Green Floating Action Button As Add/Create Action, Edit Task Form: Title Plus Bullet-List Notes And Save Checkmark FAB, Todo App Three-Panel Screenshot Collage, Snackbar Feedback 'Task marked complete', Edit Task Screen With Keyboard Open (+2 more)

### Community 44 - "Taskssort"
Cohesion: 0.22
Nodes (5): TasksSort, NEAREST_DUE, NEWEST, OLDEST, PRIORITY

### Community 46 - "Taskvault Ci Workflow (.Github"
Cohesion: 0.29
Nodes (8): instrumented CI Job (Android Emulator, connectedDebugAndroidTest), unit-lint CI Job (lintDebug + testDebugUnitTest), TaskVault CI Workflow (.github/workflows/ci.yml), Rebrand to dev.boudy04.taskvault / TaskVault, TaskVault Offline-First Client Implementation Plan, TaskVault (boudy04/taskvault), Non-Goals (no multi-user auth, no periodic sync, no pagination), TaskVault Offline-First Client Design Spec

### Community 47 - "Context"
Cohesion: 0.32
Nodes (4): AlarmManager, Context, WorkManagerModule, WorkManager

### Community 48 - "Getactiveandcompletedstats()"
Cohesion: 0.39
Nodes (3): getActiveAndCompletedStats(), StatsResult, StatisticsUtilsTest

### Community 49 - "App Launcher Icon (Quill"
Cohesion: 0.25
Nodes (8): Orange Feather/Quill Motif on Dark Rounded Square, App Launcher Icon (xhdpi), Writing/Note-Taking App Identity, App Launcher Icon (Quill Feather, xxhdpi), Dark Navy Rounded-Square Background, Orange-Red Quill Feather, Dark Rounded Square Background, Orange Feather Graphic

### Community 50 - "Customtestrunner.Kt"
Cohesion: 0.43
Nodes (5): AndroidJUnitRunner, ClassLoader, CustomTestRunner, Application, Context

### Community 52 - "Connectivitymanager"
Cohesion: 0.48
Nodes (3): ConnectivityWatcher, ConnectivityManager, Network

### Community 53 - "Tasksfiltertype"
Cohesion: 0.29
Nodes (5): TasksFilterType, ACTIVE_TASKS, ALL_TASKS, COMPLETED_TASKS, FilteringUiInfo

### Community 54 - "Taskvaultapplication"
Cohesion: 0.48
Nodes (5): Application, TaskVaultApplication, Configuration, HiltWorkerFactory, WidgetUpdater

### Community 57 - "Due Dates & Reminders"
Cohesion: 0.33
Nodes (6): Google Samples Packaging Metadata, TaskVault Restructuring Implementation Plan, due_at Data Model, Due Dates & Reminders Design Addendum, Exact-Alarm Reminder Scheduling, TaskVault Project Overview

### Community 58 - "Noteresult"
Cohesion: 0.33
Nodes (4): NoteResult, ADDED, FAILED, FORBIDDEN

### Community 59 - "Reminderbootreceiver.Kt"
Cohesion: 0.53
Nodes (4): BroadcastReceiver, Context, Intent, ReminderBootReceiver

### Community 61 - "Maincoroutinerule"
Cohesion: 0.53
Nodes (3): Description, MainCoroutineRule, TestWatcher

### Community 63 - "Stylized Orange Feather (Quill)"
Cohesion: 0.40
Nodes (5): Android App Branding / Launcher-Adjacent Logo, logo_no_fill.png Logo Asset, Stylized Orange Feather (Quill) Glyph, Solid Orange Monochrome Palette on Transparent Background, Quill Feather as Writing / Note-Taking Metaphor

### Community 65 - "Offline First Mutation Queue"
Cohesion: 0.40
Nodes (5): Transactional Room Mutations, Compose to Railway Architecture Pipeline, Last-Write-Wins Reconcile, Offline-First Mutation Queue, Room as Source of Truth

### Community 66 - "Main Screen Empty State"
Cohesion: 0.60
Nodes (5): 'You have no TO-DOs!' Quill Illustration, Green Add Floating Action Button, Main Screen - Empty State, TO-DO Notes App, Toolbar 'TO-DO Notes'

### Community 67 - "Application Visual Identity And"
Cohesion: 0.83
Nodes (4): Android Launcher Icon (mipmap-hdpi density), Application Visual Identity and Branding, Android Launcher Icon HDPI - Orange Feather on Dark Rounded Square, Orange-Red Feather Graphic on Dark Background

### Community 68 - "Trash Icon Png (Green"
Cohesion: 0.50
Nodes (4): Delete Item Action, Trash Icon PNG (green trash can with upward arrow), Restore From Trash Action, Rationale: serves as delete/remove visual affordance in app UI

### Community 69 - "Gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 72 - "Datastore Settings Store (Serverconfig:"
Cohesion: 0.67
Nodes (3): AuthInterceptor (Bearer token injection), BaseUrlInterceptor (dynamic base URL rewrite), DataStore Settings Store (ServerConfig: baseUrl + token)

## Ambiguous Edges - Review These
- `Restore From Trash Action` → `Trash Icon PNG (green trash can with upward arrow)`  [AMBIGUOUS]
  app/src/main/res/drawable/trash_icon.png · relation: semantically_similar_to

## Knowledge Gaps
- **82 isolated node(s):** `ServerConfig`, `TODO`, `DONE`, `IN_PROGRESS`, `HIGH` (+77 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **42 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Restore From Trash Action` and `Trash Icon PNG (green trash can with upward arrow)`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **Why does `SettingsRepository` connect `Settings Repo & Interceptors` to `UI Screens & Theming`, `Task Repository Core`, `Activity Auth & Login UI`, `Add Edit Task Flow`, `Widget & Reminder Alerts`, `Auth ViewModel Tests`, `Tasks List ViewModel`, `Settings Test Fakes`, `Connectivitymanager`?**
  _High betweenness centrality (0.117) - this node is a cross-community bridge._
- **Why does `TaskRepository` connect `Repository Interface` to `Task Repository Core`, `Statisticsviewmodel`, `Datamodules.Kt`, `Add Edit Task Flow`, `Taskdetailviewmodel`, `Appnavigationtest`, `Tasks List ViewModel`, `Shared Test Fakes`, `Addedittaskscreentest`, `Tasks Screen UI Tests`, `Todotheme.Kt`, `Noteresult`, `.Createtask()`, `Taskdetailscreentest`?**
  _High betweenness centrality (0.108) - this node is a cross-community bridge._
- **Why does `TaskApiService` connect `Taskapiservice` to `Task Repository Core`, `Datamodules.Kt`, `Sync Engine & DAO Tests`, `Add Edit Task Flow`, `Tasksviewmodeltest.Kt`, `Team Management`, `Auth ViewModel Tests`, `Tasks List ViewModel`, `Fakeapi`, `Auth API Models`, `Network Task DTOs`, `Auth Request Response`, `DI Network Module`, `Recordingapi`, `Memberdto`?**
  _High betweenness centrality (0.103) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Task` (e.g. with `toExternal()` and `.`completed status round trip`()`) actually correct?**
  _`Task` has 6 INFERRED edges - model-reasoned connections that need verification._
- **Are the 7 inferred relationships involving `LocalTask` (e.g. with `.deleteCompletedTasksAndGettingTasks()` and `.deleteTaskByIdAndGettingTasks()`) actually correct?**
  _`LocalTask` has 7 INFERRED edges - model-reasoned connections that need verification._
- **What connects `ServerConfig`, `TODO`, `DONE` to the rest of the system?**
  _82 weakly-connected nodes found - possible documentation gaps or missing edges._
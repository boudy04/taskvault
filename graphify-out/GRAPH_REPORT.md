# Graph Report - p4  (2026-08-23)

## Corpus Check
- 33 files · ~113,468 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 856 nodes · 1509 edges · 54 communities (46 shown, 8 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 86 edges (avg confidence: 0.85)
- Token cost: 3,050 input · 2,520 output

## Community Hubs (Navigation)
- Repository & Mapping Layer
- Room DAO Tests
- Sync Engine
- Add/Edit Task UI
- Add/Edit Screen Tests
- Settings DI & Repository
- LocalTask Entity & Sync Worker
- Statistics UI
- Pending Op DAO Tests
- Settings Screen
- Tasks Screen Tests
- Shared Test Fakes
- Add/Edit Task ViewModel
- Tasks List ViewModel
- CI Pipeline & Contribution Docs
- Task Status & Destinations
- Task Detail ViewModel
- Task Model & Statistics
- Offline-First Sync Design
- Statistics ViewModel
- Font License Terms
- Tasks Light Theme Screenshot
- Tasks ViewModel Tests
- Tasks Dark Theme Screenshot
- Navigation Drawer
- Dependency Renovation Config
- Settings ViewModel
- Legacy App Screenshots
- Edit Priority Screenshot
- Settings Screenshot
- Espresso Idling Resource
- Launcher Icons xhdpi+xxhdpi
- Custom Test Runner
- Coroutines DI Module
- Brand Logo Assets
- Legacy Demo GIF
- Launcher Icon hdpi
- Trash Icon Assets
- Launcher Icon mdpi
- Gradle Wrapper Scripts
- Auth Interceptors Concept
- Server Config
- Application Node
- Component Activity
- Drawer State
- Response Type

## God Nodes (most connected - your core abstractions)
1. `Task` - 47 edges
2. `FakeTaskRepository` - 38 edges
3. `LocalTask` - 37 edges
4. `TaskRepository` - 37 edges
5. `AddEditTaskViewModel` - 22 edges
6. `TaskPriority` - 22 edges
7. `TaskDao` - 21 edges
8. `DefaultTaskRepository` - 21 edges
9. `FakeTaskDao` - 19 edges
10. `PendingOpDao` - 19 edges

## Surprising Connections (you probably didn't know these)
- `TaskVault Offline-First Client Design Spec` --references--> `mock/prod Product Flavors`  [AMBIGUOUS]
  docs/superpowers/specs/2026-08-22-taskvault-offline-client-design.md → README.md
- `Contributor License Agreement (CLA)` --conceptually_related_to--> `Android Architecture Samples (Upstream Project)`  [INFERRED]
  CONTRIBUTING.md → README.md
- `Pull Request Code Review Requirement` --conceptually_related_to--> `Android Architecture Samples (Upstream Project)`  [INFERRED]
  CONTRIBUTING.md → README.md
- `OfflineFirstRepositoryTest` --references--> `FakePendingOpDao`  [EXTRACTED]
  app/src/test/java/dev/boudy04/taskvault/data/OfflineFirstRepositoryTest.kt → shared-test/src/main/java/dev/boudy04/taskvault/data/source/local/FakePendingOpDao.kt
- `OfflineFirstRepositoryTest` --calls--> `MainCoroutineRule`  [EXTRACTED]
  app/src/test/java/dev/boudy04/taskvault/data/OfflineFirstRepositoryTest.kt → shared-test/src/main/java/dev/boudy04/taskvault/MainCoroutineRule.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **CI: unit-lint Job + Emulator instrumented Job** — _github_workflows_ci_workflow, _github_workflows_ci_unit_lint_job, _github_workflows_ci_instrumented_job [EXTRACTED 1.00]
- **Offline-First Sync Pipeline (write-local -> enqueue -> drain -> pull)** — docs_superpowers_plans_2026_08_22_taskvault_offline_client_default_task_repository_write_path, docs_superpowers_plans_2026_08_22_taskvault_offline_client_pending_op_entity, docs_superpowers_plans_2026_08_22_taskvault_offline_client_pending_op_dao, docs_superpowers_plans_2026_08_22_taskvault_offline_client_sync_scheduler, docs_superpowers_plans_2026_08_22_taskvault_offline_client_sync_worker, docs_superpowers_plans_2026_08_22_taskvault_offline_client_sync_engine, docs_superpowers_plans_2026_08_22_taskvault_offline_client_task_api_service, docs_superpowers_plans_2026_08_22_taskvault_offline_client_local_task_entity [EXTRACTED 1.00]
- **Retrofit Network Stack (dynamic base URL + Bearer auth + kotlinx-serialization)** — docs_superpowers_plans_2026_08_22_taskvault_offline_client_task_api_service, docs_superpowers_plans_2026_08_22_taskvault_offline_client_task_dto, docs_superpowers_plans_2026_08_22_taskvault_offline_client_auth_interceptor, docs_superpowers_plans_2026_08_22_taskvault_offline_client_base_url_interceptor, docs_superpowers_plans_2026_08_22_taskvault_offline_client_datastore_settings_repository [EXTRACTED 1.00]

## Communities (54 total, 8 thin omitted)

### Community 0 - "Repository & Mapping Layer"
Cohesion: 0.06
Nodes (30): copyFromDto(), toDtoWithoutServerId(), toExternal(), toLocal(), AuthInterceptor, Interceptor, Response, BaseUrlInterceptor (+22 more)

### Community 1 - "Room DAO Tests"
Cohesion: 0.06
Nodes (10): TaskDaoTest, LocalTask, Flow, TaskDao, TODO, TaskPayload, OfflineFirstRepositoryTest, RecordingSyncScheduler (+2 more)

### Community 2 - "Sync Engine"
Cohesion: 0.07
Nodes (19): SyncEngine, SyncOutcome, CONNECTIVITY_RETRY, FAILURE, RETRY, SUCCESS, FakeApi, httpException() (+11 more)

### Community 3 - "Add/Edit Task UI"
Cohesion: 0.06
Nodes (22): AddEditTaskContent(), AddEditTaskScreen(), Modifier, SnackbarHostState, PriorityPicker(), DefaultTaskRepository, Flow, Converters (+14 more)

### Community 4 - "Add/Edit Screen Tests"
Cohesion: 0.07
Nodes (12): AddEditTaskScreenTest, SemanticsNodeInteraction, StatisticsScreenTest, TaskDetailScreenTest, AppNavigationTest, SemanticsNodeInteraction, HiltTestActivity, ComponentActivity (+4 more)

### Community 5 - "Settings DI & Repository"
Cohesion: 0.08
Nodes (26): Context, SettingsModule, DataStoreSettingsRepository, Keys, Flow, SettingsRepository, ThemeMode, DARK (+18 more)

### Community 6 - "LocalTask Entity & Sync Worker"
Cohesion: 0.09
Nodes (20): SyncWorker, TaskVaultApplication, TaskVaultWidget, TaskVaultWidgetEntryPoint, TaskVaultWidgetReceiver, toWidgetState(), WidgetState, WidgetUpdater (+12 more)

### Community 7 - "Statistics UI"
Cohesion: 0.09
Nodes (27): Modifier, SnackbarHostState, StatisticsContent(), StatisticsContentEmptyPreview(), StatisticsContentPreview(), StatisticsScreen(), EditTaskContent(), EditTaskContentEmptyPreview() (+19 more)

### Community 8 - "Pending Op DAO Tests"
Cohesion: 0.08
Nodes (10): PendingOpDaoTest, Flow, PendingOpEntity, PendingOpState, PendingOpDao, ToDoDatabase, DatabaseModule, RoomDatabase (+2 more)

### Community 9 - "Settings Screen"
Cohesion: 0.11
Nodes (22): Modifier, SnackbarHostState, SettingsContent(), SettingsContentPreview(), SettingsScreen(), verifyThen(), BiometricPrompt, NotificationHelper (+14 more)

### Community 11 - "Shared Test Fakes"
Cohesion: 0.12
Nodes (4): FakeTaskRepository, Flow, StateFlow, RepositoryTestModule

### Community 12 - "Add/Edit Task ViewModel"
Cohesion: 0.18
Nodes (5): AddEditTaskUiState, AddEditTaskViewModel, StateFlow, ViewModel, AddEditTaskViewModelTest

### Community 13 - "Tasks List ViewModel"
Cohesion: 0.15
Nodes (10): TasksFilterType, ACTIVE_TASKS, ALL_TASKS, COMPLETED_TASKS, FilteringUiInfo, MutableStateFlow, StateFlow, ViewModel (+2 more)

### Community 14 - "CI Pipeline & Contribution Docs"
Cohesion: 0.12
Nodes (20): instrumented CI Job (Android Emulator, connectedDebugAndroidTest), unit-lint CI Job (lintDebug + testDebugUnitTest), TaskVault CI Workflow (.github/workflows/ci.yml), Google Sample Packaging Metadata (android/architecture-samples), Contributor License Agreement (CLA), Pull Request Code Review Requirement, Rebrand to dev.boudy04.taskvault / TaskVault, TaskVault Offline-First Client Implementation Plan (+12 more)

### Community 15 - "Task Status & Destinations"
Cohesion: 0.14
Nodes (8): TaskStatus, DONE, IN_PROGRESS, TodoDestinationsArgs, StatisticsViewModelTest, Description, MainCoroutineRule, TestWatcher

### Community 16 - "Task Detail ViewModel"
Cohesion: 0.14
Nodes (6): MutableStateFlow, StateFlow, ViewModel, TaskDetailUiState, TaskDetailViewModel, TaskDetailViewModelTest

### Community 17 - "Task Model & Statistics"
Cohesion: 0.18
Nodes (5): Task, getActiveAndCompletedStats(), StatsResult, TaskMappingExtTest, StatisticsUtilsTest

### Community 18 - "Offline-First Sync Design"
Cohesion: 0.13
Nodes (18): DefaultTaskRepository Write Path (write-local, enqueue-op, request-sync), LocalTask Room Entity (schema v2 with status/priority/serverId/timestamps), Offline-First Mutation Queue Pattern, PendingOpDao, PendingOpEntity (pending_ops table), Railway FastAPI task-api Backend (prject-cv-production.up.railway.app), Room as Single Source of Truth, SyncEngine (drain + pull) (+10 more)

### Community 19 - "Statistics ViewModel"
Cohesion: 0.20
Nodes (9): StateFlow, ViewModel, StatisticsUiState, StatisticsViewModel, Async, Error, Loading, Success (+1 more)

### Community 20 - "Font License Terms"
Cohesion: 0.18
Nodes (14): Condition 2: Bundling/Redistribution Requires Copyright Notice And License Copy, Condition 4: Holder/Author Names Not Used For Promotion Of Modified Versions, Condition 1: Font Software May Not Be Sold By Itself, Condition 3: Modified Versions May Not Use Reserved Font Names Without Permission, Condition 5: Font Must Remain Under OFL; Documents Created With It Are Exempt, Outfit Font License File, Definition: Modified Version, The Outfit Project Authors (Copyright Holder) (+6 more)

### Community 21 - "Tasks Light Theme Screenshot"
Cohesion: 0.20
Nodes (14): Rounded-Square Orange Plus Floating Action Button (Add Task), 'All Tasks' Section Header, Card-Based List Layout Pattern (White Rounded Cards on Tinted Background), Task Card 'test 101' with Red High Priority Chip, Rationale: Light Warm Theme Prioritizes Readability and Low-Eye-Strain Daily Task Use, Task Card 'hmm' with Green Low Priority Chip, Color-Coded Priority Chips (High=Red, Low=Green), Small Orange Status Dot at Right Edge of Each Task Card (+6 more)

### Community 23 - "Tasks Dark Theme Screenshot"
Cohesion: 0.21
Nodes (13): 'All Tasks' View Heading, TaskVault App Bar with Hamburger Menu and Toolbar Icons, Design Rationale: Color-Coded Priority Chips Enable At-a-Glance Triage (Red=Urgent, Green=Low), Unchecked Completion Checkbox Per Task, Design Rationale: Near-Black Background With Slightly Lighter Navy Cards for Contrast, Orange Rounded-Square '+' FAB Bottom-Right for Adding Tasks, High Priority Chip (Red), Low Priority Chip (Green) (+5 more)

### Community 24 - "Navigation Drawer"
Cohesion: 0.38
Nodes (10): AppDrawer(), AppModalDrawer(), DrawerButton(), DrawerHeader(), CoroutineScope, Modifier, PreviewAppDrawer(), DrawerState (+2 more)

### Community 25 - "Dependency Renovation Config"
Cohesion: 0.18
Nodes (10): config:base, :dependencyDashboard, group:all, main, schedule:daily, baseBranches, commitMessageExtra, extends (+2 more)

### Community 26 - "Settings ViewModel"
Cohesion: 0.27
Nodes (4): StateFlow, ViewModel, SettingsUiState, SettingsViewModel

### Community 27 - "Legacy App Screenshots"
Cohesion: 0.31
Nodes (10): Checkbox Rows With Strikethrough For Completed Tasks, Jetpack Compose State-Learning Todo Sample App Context, Feather Illustration Empty-State Placeholder Pattern, Green Floating Action Button As Add/Create Action, Edit Task Form: Title Plus Bullet-List Notes And Save Checkmark FAB, Todo App Three-Panel Screenshot Collage, Snackbar Feedback 'Task marked complete', Edit Task Screen With Keyboard Open (+2 more)

### Community 28 - "Edit Priority Screenshot"
Cohesion: 0.25
Nodes (9): Back Arrow Navigation, Dark Theme Design Decision, Open Dropdown Menu (Low/Medium/High), Edit Task Screen, Numeric Field ('1'), Orange Accent Color Scheme, Priority Dropdown (Focused), Orange Checkmark Save FAB (+1 more)

### Community 29 - "Settings Screenshot"
Cohesion: 0.31
Nodes (9): App Lock Toggle (Disabled), Auth Token Input Field (Masked), Back Arrow Navigation, Mobile Client Points At Configurable Self-Hosted Server, Dark Theme Minimalist Mobile UI Design, Production Backend Deployed On Railway (prject-cv-production.up.railway.app), Save Button, Server Settings Screen (+1 more)

### Community 31 - "Launcher Icons xhdpi+xxhdpi"
Cohesion: 0.25
Nodes (8): Orange Feather/Quill Motif on Dark Rounded Square, App Launcher Icon (xhdpi), Writing/Note-Taking App Identity, App Launcher Icon (Quill Feather, xxhdpi), Dark Navy Rounded-Square Background, Orange-Red Quill Feather, Dark Rounded Square Background, Orange Feather Graphic

### Community 32 - "Custom Test Runner"
Cohesion: 0.43
Nodes (5): AndroidJUnitRunner, ClassLoader, CustomTestRunner, Application, Context

### Community 33 - "Coroutines DI Module"
Cohesion: 0.48
Nodes (3): CoroutinesModule, CoroutineScope, CoroutineDispatcher

### Community 34 - "Brand Logo Assets"
Cohesion: 0.40
Nodes (5): Android App Branding / Launcher-Adjacent Logo, logo_no_fill.png Logo Asset, Stylized Orange Feather (Quill) Glyph, Solid Orange Monochrome Palette on Transparent Background, Quill Feather as Writing / Note-Taking Metaphor

### Community 35 - "Legacy Demo GIF"
Cohesion: 0.60
Nodes (5): 'You have no TO-DOs!' Quill Illustration, Green Add Floating Action Button, Main Screen - Empty State, TO-DO Notes App, Toolbar 'TO-DO Notes'

### Community 36 - "Launcher Icon hdpi"
Cohesion: 0.83
Nodes (4): Android Launcher Icon (mipmap-hdpi density), Application Visual Identity and Branding, Android Launcher Icon HDPI - Orange Feather on Dark Rounded Square, Orange-Red Feather Graphic on Dark Background

### Community 37 - "Trash Icon Assets"
Cohesion: 0.50
Nodes (4): Delete Item Action, Trash Icon PNG (green trash can with upward arrow), Restore From Trash Action, Rationale: serves as delete/remove visual affordance in app UI

### Community 38 - "Launcher Icon mdpi"
Cohesion: 0.67
Nodes (4): Green Android Robot Head Visual, Android Studio Default Template Icon, Launcher Icon PNG (mdpi), MDPI Density Variant (~48x48px)

### Community 39 - "Gradle Wrapper Scripts"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 40 - "Auth Interceptors Concept"
Cohesion: 0.67
Nodes (3): AuthInterceptor (Bearer token injection), BaseUrlInterceptor (dynamic base URL rewrite), DataStore Settings Store (ServerConfig: baseUrl + token)

## Ambiguous Edges - Review These
- `TaskVault Offline-First Client Design Spec` → `mock/prod Product Flavors`  [AMBIGUOUS]
  docs/superpowers/specs/2026-08-22-taskvault-offline-client-design.md · relation: references
- `Restore From Trash Action` → `Trash Icon PNG (green trash can with upward arrow)`  [AMBIGUOUS]
  app/src/main/res/drawable/trash_icon.png · relation: semantically_similar_to
- `Save Button` → `Mobile Client Points At Configurable Self-Hosted Server`  [AMBIGUOUS]
  docs/screenshots/settings.png · relation: rationale_for
- `Task Card Row Component (Checkbox + Priority Pill + Title + Indicator Dot)` → `Yellow/Orange Status Dot on Card Right Edge (Reminder or Unread Marker)`  [AMBIGUOUS]
  docs/screenshots/tasks_dark.png · relation: conceptually_related_to
- `Task Card 'test 101' with Red High Priority Chip` → `Small Orange Status Dot at Right Edge of Each Task Card`  [AMBIGUOUS]
  docs/screenshots/tasks_light.png · relation: conceptually_related_to

## Knowledge Gaps
- **71 isolated node(s):** `Error`, `Loading`, `TodoDestinations`, `TodoScreens`, `ServerConfig` (+66 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **8 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `TaskVault Offline-First Client Design Spec` and `mock/prod Product Flavors`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `Restore From Trash Action` and `Trash Icon PNG (green trash can with upward arrow)`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `Save Button` and `Mobile Client Points At Configurable Self-Hosted Server`?**
  _Edge tagged AMBIGUOUS (relation: rationale_for) - confidence is low._
- **What is the exact relationship between `Task Card Row Component (Checkbox + Priority Pill + Title + Indicator Dot)` and `Yellow/Orange Status Dot on Card Right Edge (Reminder or Unread Marker)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Task Card 'test 101' with Red High Priority Chip` and `Small Orange Status Dot at Right Edge of Each Task Card`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `TaskRepository` connect `Add/Edit Screen Tests` to `Repository & Mapping Layer`, `Add/Edit Task UI`, `Tasks Screen Tests`, `Shared Test Fakes`, `Add/Edit Task ViewModel`, `Tasks List ViewModel`, `Task Detail ViewModel`, `Statistics ViewModel`?**
  _High betweenness centrality (0.172) - this node is a cross-community bridge._
- **Why does `Task` connect `Task Model & Statistics` to `Repository & Mapping Layer`, `Add/Edit Task UI`, `Add/Edit Screen Tests`, `Statistics UI`, `Shared Test Fakes`, `Add/Edit Task ViewModel`, `Tasks List ViewModel`, `Task Status & Destinations`, `Task Detail ViewModel`, `Statistics ViewModel`, `Tasks ViewModel Tests`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
# TaskVault + Task API — Due Dates & Reminders — Design Addendum

> Extends 2026-08-22-taskvault-offline-client-design.md and the task-api service.
> User-selected UX: quick chips (Today / Tomorrow / Pick date) + Material3 TimePicker (R20).
> Reminders are silent shade notifications viewable anywhere; tapping opens the app.

## Data model

- API `tasks.due_at TIMESTAMPTZ NULL` (SQLAlchemy `Mapped[datetime | None]`,
  `DateTime(timezone=True)`). JSON field `due_at`: ISO-8601 UTC string or null.
  Accepted by POST / PUT / PATCH (any subset), returned by every task representation.
- Migration: idempotent startup step after `create_all`:
  `ALTER TABLE tasks ADD COLUMN IF NOT EXISTS due_at TIMESTAMPTZ`.
  Existing rows keep NULL = no reminder. Backward compatible with older clients.
- App: `LocalTask.dueAt: String?` (ISO-8601 UTC, same convention as created_at),
  domain `Task.dueAt: String?`, DTO `@SerialName("due_at") val dueAt: String? = null`,
  `TaskPayload.dueAt`. Mappers thread it through all three conversions.

## App UI

- Edit screen "Due" row: formatted local value ("Sat, Aug 23 · 14:30") or "None";
  tap opens bottom sheet/dialog flow: chips Today / Tomorrow / Pick date…
  then Material3 TimePicker; Clear action removes.
- List rows: clock icon + relative/localized due text when set; overdue = error-red text.
- ViewModel holds `dueAt: MutableStateFlow<String?>`; save passes it through
  repository (`createTask/updateTask` gain optional `dueAt: String? = null`).

## Reminders

- `ReminderScheduler` interface: `schedule(localId, title, dueAtMillis)`, `cancel(localId)`;
  impl `AlarmReminderScheduler` uses `AlarmManager.setExactAndAllowWhileIdle`
  (falls back to `WorkManager` one-time delay when `!canScheduleExactAlarms()`, API 31+).
- `ReminderReceiver` (BroadcastReceiver): posts silent notification on channel
  `reminders` (IMPORTANCE_LOW): title "Task due", text = task title; contentIntent opens app.
- Reschedule on create/update-with-due; cancel on complete/delete/bulk-clear;
  `BOOT_COMPLETED` receiver re-schedules all rows with future `due_at`.
- Manifest adds `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, receivers.

## API plan (T17)

Branch off p3 worktree `task-api`; model + schemas + routers accept/return `due_at`;
startup migration line; ~6 new test cases (null default, create w/ due, patch due,
clear due, invalid format 422, list ordering unaffected). Deploy only after user OK.

## Non-goals

No recurring tasks, no timezone picker (device local time ↔ stored UTC), no calendar import.

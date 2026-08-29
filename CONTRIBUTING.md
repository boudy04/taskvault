# Contributing to TaskVault

This is a personal portfolio project, but issues and pull requests are welcome.

- **License:** Apache-2.0 (see `LICENSE`). By opening a pull request you agree that your contribution is licensed under the same terms.
- **Baseline credit:** the app is built on [android/architecture-samples](https://github.com/android/architecture-samples) (Copyright The Android Open Source Project). Keep that credit intact when touching shared code.
- **Before you push:**
  - `.\gradlew :app:testDebugUnitTest` — keep the unit suite green (145 tests across 16 suites at the time of writing)
  - `.\gradlew lintDebug` — no new lint issues
- **Code layout:** follow the existing structure — feature packages under `app/src/main/java/dev/boudy04/taskvault/`, with `data/` (Room + repository) and `sync/` (queue drain, reminders, notifications) as the core seams. Keep Room as the single source of truth and route every mutation through the offline-first pending-op queue.
- **Large changes:** open a GitHub issue with reproduction steps or logs before starting a refactor, and update the relevant plan/spec docs under `docs/superpowers/`.

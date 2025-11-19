# Repository Guidelines

## Project Structure & Module Organization
- `admin-dashboard/` hosts the SvelteKit 5 backend + UI. Place endpoints under `src/routes/api`, server utilities under `src/lib/server`, and dashboard pages in `src/routes`. SQLite schema and migrations live in `drizzle.config.ts` and the `sqlite.db` file; avoid editing the database directly. 
- `android-launcher/` is a Kotlin Android app. Core launcher code sits in `app/src/main/java`, Compose UI in `.../ui`, and WorkManager/telemetry classes in `.../telemetry`. Shared resources live under `app/src/main/res`.
- Keep docs (e.g., `PLAN.md`, `CHANGELOG.md`, `AGENTS.md`) at repo root for quick onboarding.

## Build, Test, and Development Commands
- Backend/UI: `cd admin-dashboard && npm install` once, then `npm run dev` for Vite dev server, `npm run build` for production bundles, `npm run preview` to sanity-check built assets, `npm run check` for type + Svelte diagnostics, `npm run lint` for ESLint.
- Android: `cd android-launcher && ./gradlew tasks` to explore, `./gradlew :app:assembleDebug` for APK, `./gradlew :app:installDebug` to deploy to a connected device, and `./gradlew testDebugUnitTest` for JVM tests. Run `adb reverse tcp:5173 tcp:5173` before manual telemetry tests.

## Coding Style & Naming Conventions
- TypeScript/Svelte follows ESLint config in `admin-dashboard/eslint.config.js`; use 2-space indent, kebab-case route files (`+page.svelte`, `+page.server.ts`), and snake_case database columns.
- Kotlin uses 4-space indent, UpperCamelCase for classes/composables, lowerCamelCase for functions/variables, and SCREAMING_SNAKE_CASE for constants. Compose screens end with `Screen`, workers end with `Worker`.
- Prefer descriptive commit prefixes (`backend:`, `android:`) to highlight touchpoints.

## Testing Guidelines
- Web: `npm run test` (Vitest) for unit tests; add files under `src/lib/**/__tests__` or `*.test.ts`. Use `npm run check` to catch type + Svelte issues before PRs.
- Android: place JVM tests in `app/src/test`, instrumentation in `app/src/androidTest`. Run `./gradlew testDebugUnitTest` for logic and `./gradlew connectedDebugAndroidTest` when emulator/device is available. Provide telemetry replay scripts if a new sensor path is added.

## Commit & Pull Request Guidelines
- Write imperative, component-scoped commits (e.g., `android: fix telemetry JSON parsing`). Reference issue IDs or PLAN tasks when relevant.
- PRs should include: summary, test evidence (`npm run test`, `./gradlew :app:assembleDebug`, screenshots for UI), rollout considerations, and database notes (migrations, seed changes). Tag reviewers covering both Android and dashboard when changes cross phases.

## Security & Configuration Tips
- Never commit `.env`, `sqlite.db`, or emulator keystores. Use `local.properties` for SDK paths and `.env` files ignored by Git for secrets.
- For local HTTPS testing, rely on adb reverse instead of exposing backend ports. Rotate API credentials when sharing builds outside the team.

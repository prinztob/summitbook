# AGENTS.md

Single-module Android app (`:app`), namespace `de.drtobiasprinz.summitbook`. Kotlin + Jetpack Compose (mixed with View-based code; both `viewBinding` and `compose` are enabled), Hilt, Room, WorkManager, Glance widgets, osmdroid/MapsForge offline maps. Heavy analysis logic runs in embedded Python via Chaquopy.

## Commands

```bash
./gradlew assembleDebug                        # build APK
./gradlew :app:testDebugUnitTest               # all unit tests (Robolectric + ArchUnit)
./gradlew :app:testDebugUnitTest --tests "de.drtobiasprinz.summitbook.data.maps.OfflineMapAnalyzerTest"        # single class
./gradlew :app:testDebugUnitTest --tests "de.drtobiasprinz.summitbook.data.maps.OfflineMapAnalyzerTest.testE2EOnlyAsphalt"  # single method
./gradlew :app:testDebugUnitTest --tests "de.drtobiasprinz.summitbook.architecture.*"  # layering/cycle/package/naming rules
./gradlew :app:lintDebug                       # Android lint
./gradlew :app:connectedDebugAndroidTest       # instrumented tests (needs device/emulator)
```

- Gradle daemon requires JDK 21 (foojay toolchain resolves it; see `gradle/gradle-daemon-jvm.properties`). Java/Kotlin compile target is 17.
- No CI, no formatter config, no Fastfile — `fastlane/` holds store metadata/screenshots only.

## Setup gotchas

- **Chaquopy `buildPython` is hardcoded** to `/home/prinzt/.pyenv/shims/python` in `app/build.gradle`. Builds fail on machines without that path; point it at a local Python 3.10.
- Python pip dependencies are pinned in the `python { pip { ... } } }` block of `app/build.gradle` — that block is the source of truth for the embedded Python environment (no requirements.txt).
- Repos include Jitpack; `RepositoriesMode.FAIL_ON_PROJECT_REPOS` means never add repos inside modules.
- All dependency coordinates/versions live in `gradle/libs.versions.toml` — add new libraries there and reference them as `libs.<alias>` in `app/build.gradle`, never inline `implementation "group:artifact:version"` strings.

## Architecture

Code is organized into layer packages under `de.drtobiasprinz.summitbook` (enforced by ArchUnit tests in `app/src/test/java/.../architecture/` — run them via `--tests "de.drtobiasprinz.summitbook.architecture.*"`):

- `core/` — constants, shared utils, preferences, color theme, `WidgetUpdater` interface. May not depend on any other layer.
- `data/` — Room database (`db/AppDatabase.kt`, entities, DAOs), models, repository (`repository/DatabaseRepository.kt`), analytics, offline-map helpers (`maps/`), backup/zip, app-state singleton (`appstate/AppState.kt`), Garmin JSON parsing. May only depend on `core`.
- `sync/` — Garmin Connect download + GPX analysis executors (`GpxPyExecutor.kt`, `GarminPythonExecutor.kt`, `GarminDataUpdater.kt`). May depend on `data`, `core`.
- `work/` — WorkManager workers (`@HiltWorker`; `MyApp` implements `Configuration.Provider` with `HiltWorkerFactory`). May depend on `data`, `sync`, `core`.
- `widget/` — Glance widget + receiver; uses a Hilt `@EntryPoint` for the repository. May depend on `data`, `core` (plus `ui.activities.MainActivityCompose` for launching the app and `ui.activities.SummitEntryDetailsComposeActivity` for row deep links — the only allowed widget→ui references).
- `ui/` — activities, Compose screens, views, viewmodels. May depend on `data`, `sync`, `work`, `core`.
- `di/` — Hilt modules. May access all layers.

Other invariants: no layer may reference `MyApp` (inject dependencies instead of casting); no new top-level packages beyond the list above (plus generated `databinding`); no dependency cycles between top-level packages.

- Entry: `MyApp` (Hilt) → `MainActivityCompose` (launcher). `ReceiverActivityCompose` receives shared/opened GPX files. UI is activity-based (no fragments): `SummitEntryDetailsComposeActivity`, `SegmentEntryDetailsComposeActivity`, `GarminLoginActivity` (Compose MFA login; `PythonActivity` raw console kept as Chaquopy debug tool).
- **Navigation**: `MainActivityCompose` uses Navigation-Compose (`NavHost`, routes = `Destination` enum names, start = `Summits`). A bottom bar holds the 5 primary destinations (Summits, Segments, Statistics, Map, Settings); the drawer holds all of them plus export/import. The activity property `currentDestination` is synced from the NavController via an `OnDestinationChangedListener` — never write it directly; call the activity's `navigateTo(Destination)` helper instead (it routes through the NavController with popUpTo/saveState/restoreState).
- **Python bridge**: Kotlin calls `entry_point.py` via `Python.getInstance().getModule("entry_point")` (see `sync/GpxPyExecutor.kt`, `sync/GarminPythonExecutor.kt`). Python code lives in `app/src/main/python/` and handles GPX/TCX analysis, heatmaps, and Garmin Connect download. API changes require updating both sides (Python function + Kotlin caller).
- Global mutable app state (storage dirs, `Python` instance, executor, `peaks`) lives in `data/appstate/AppState.kt` — not in activity companions.

## UI conventions

- **Theming**: `ui/theme/Theme.kt` → `SummitBookTheme` uses the fixed brand palette (`core/theme/ColorSchemes.kt`: `LightColors`/`DarkColors`). `dynamicColor` is off by default — do not re-enable Material You unless asked. Chart/record colors are named tokens in `ui/theme/ChartColors.kt`; do not reintroduce raw `Color(0x…)` literals in screens.
- **State retention**: all form/dialog screens keep their input in `rememberSaveable` (custom savers in `ui/compose/FormStateSavers.kt` — Gson/enum/date/list/range-slider). New form fields must be saveable too; never put large objects (track points, full lists of summits) into the saved-state Bundle.
- **Strings**: no hardcoded user-visible English — everything goes through `strings.xml` (+ `values-de/`), including icon `contentDescription`s.
- **Feedback**: use Snackbars (with Retry action where applicable) instead of Toasts for operation results; blocking operations keep the LoadingPanel; pull-to-refresh on the summits list triggers a Garmin sync.

## Room schema / migrations

- `exportSchema = true`; schema JSONs are committed under `app/schemas/de.drtobiasprinz.summitbook.data.db.AppDatabase/`.
- When changing an entity: bump `version` in `@Database`, add an `AutoMigration(from = N, to = N+1)` (with an `AutoMigrationSpec` for renames/deletes — see existing specs in `AppDatabase.kt`), and build so the new schema JSON is generated. Commit the schema JSON together with the entity change.

## Testing quirks

- Robolectric is pinned to SDK 28 via `app/src/test/resources/robolectric.properties`.
- Fixtures (GPX/JSON/CSV/YAML) are read from `app/src/test/resources` via `classLoader.getResource`.
- `Bayern_oam.osm.map` (large MapsForge test map) and `OAM-World-1-10-J80.sqlitedb` are **gitignored**. Tests in `OfflineMapAnalyzerTest` guard on the resource being present and silently skip otherwise — those tests passing does not mean the map-dependent assertions ran.

## Kotlin setup

The project uses AGP 9 **built-in Kotlin** (no `org.jetbrains.kotlin.android` plugin applied; `kotlin.compilerOptions.jvmTarget` defaults to `compileOptions.targetCompatibility`). Do not re-add the `kotlin-android` plugin or the `android.builtInKotlin=false` / `android.newDsl=false` flags — the `kotlin-android` plugin is incompatible with AGP 9's new DSL.

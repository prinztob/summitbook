# AGENTS.md

Single-module Android app (`:app`), namespace `de.drtobiasprinz.summitbook`. Kotlin + Jetpack Compose (mixed with View-based code; both `viewBinding` and `compose` are enabled), Hilt, Room, WorkManager, Glance widgets, osmdroid/MapsForge offline maps. Heavy analysis logic runs in embedded Python via Chaquopy.

## Commands

```bash
./gradlew assembleDebug                        # build APK
./gradlew :app:testDebugUnitTest               # all unit tests (Robolectric)
./gradlew :app:testDebugUnitTest --tests "de.drtobiasprinz.summitbook.utils.OfflineMapAnalyzerTest"        # single class
./gradlew :app:testDebugUnitTest --tests "de.drtobiasprinz.summitbook.utils.OfflineMapAnalyzerTest.testE2EOnlyAsphalt"  # single method
./gradlew :app:lintDebug                       # Android lint
./gradlew :app:connectedDebugAndroidTest       # instrumented tests (needs device/emulator)
```

- Gradle daemon requires JDK 21 (foojay toolchain resolves it; see `gradle/gradle-daemon-jvm.properties`). Java/Kotlin compile target is 17.
- No CI, no formatter config, no Fastfile — `fastlane/` holds store metadata/screenshots only.

## Setup gotchas

- **Chaquopy `buildPython` is hardcoded** to `/home/prinzt/.pyenv/shims/python` in `app/build.gradle`. Builds fail on machines without that path; point it at a local Python 3.10.
- Python pip dependencies are pinned in the `python { pip { ... } } }` block of `app/build.gradle` — that block is the source of truth for the embedded Python environment (no requirements.txt).
- Repos include Jitpack; `RepositoriesMode.FAIL_ON_PROJECT_REPOS` means never add repos inside modules.

## Architecture

- Entry: `MyApp` (Hilt) → `MainActivityCompose` (launcher). `ReceiverActivityCompose` receives shared/opened GPX files. UI is activity-based (no fragments): `SummitEntryDetailsComposeActivity`, `SegmentEntryDetailsComposeActivity`, `PythonActivity`.
- **Python bridge**: Kotlin calls `entry_point.py` via `Python.getInstance().getModule("entry_point")` (see `GpxPyExecutor.kt`, `GarminPythonExecutor.kt`). Python code lives in `app/src/main/python/` and handles GPX/TCX analysis, heatmaps, and Garmin Connect download. API changes require updating both sides (Python function + Kotlin caller).
- `db/AppDatabase.kt` is the single Room database.
- `repository/DatabaseRepository.kt` is the data-access layer.

## Room schema / migrations

- `exportSchema = true`; schema JSONs are committed under `app/schemas/de.drtobiasprinz.summitbook.db.AppDatabase/`.
- When changing an entity: bump `version` in `@Database`, add an `AutoMigration(from = N, to = N+1)` (with an `AutoMigrationSpec` for renames/deletes — see existing specs in `AppDatabase.kt`), and build so the new schema JSON is generated. Commit the schema JSON together with the entity change.

## Testing quirks

- Robolectric is pinned to SDK 28 via `app/src/test/resources/robolectric.properties`.
- Fixtures (GPX/JSON/CSV/YAML) are read from `app/src/test/resources` via `classLoader.getResource`.
- `Bayern_oam.osm.map` (large MapsForge test map) and `OAM-World-1-10-J80.sqlitedb` are **gitignored**. Tests in `OfflineMapAnalyzerTest` guard on the resource being present and silently skip otherwise — those tests passing does not mean the map-dependent assertions ran.

## Kotlin setup

The project uses AGP 9 **built-in Kotlin** (no `org.jetbrains.kotlin.android` plugin applied; `kotlin.compilerOptions.jvmTarget` defaults to `compileOptions.targetCompatibility`). Do not re-add the `kotlin-android` plugin or the `android.builtInKotlin=false` / `android.newDsl=false` flags — the `kotlin-android` plugin is incompatible with AGP 9's new DSL.

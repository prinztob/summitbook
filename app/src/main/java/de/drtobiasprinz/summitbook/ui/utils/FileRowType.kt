package de.drtobiasprinz.summitbook.ui.utils

import android.util.Log
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.pythonInstance
import java.io.File


enum class FileRowType(
    val getFile: (Summit) -> File,
    val shouldUpdateRoadInfos: Boolean = false,
    val checkAction: (Summit) -> Boolean = { _ -> true },
    val updateAction: (Summit, File) -> Unit = { _, _ -> }
) {
    GPX_TRACK(
        { it.getGpsTrackPath(simplified = false).toFile() },
        shouldUpdateRoadInfos = true,
        checkAction = { s ->
            s.getGpsTrackPath().toFile().readText().contains(":TrackPointExtension>")
        },
        updateAction = { summit, backupFile ->
            try {
                pythonInstance?.let { python ->
                    GpxPyExecutor(python).removeExtensionsFromGpxTracks(
                        backupFile, summit.getGpsTrackPath().toFile()
                    )
                }
                Log.i(
                    "FileRowType",
                    "Successfully removeExtensionsFromGpxTracks for ${summit.getDateAsString()}_${summit.name}."
                )
            } catch (e: RuntimeException) {
                Log.e(
                    "FileRowType",
                    "RemoveExtensionsFromGpxTracks failed for ${summit.getDateAsString()}_${summit.name}.",
                    e
                )
            }
        }),
    GPX_SIMPLIFIED(
        { it.getGpsTrackPath(simplified = true).toFile() },
        updateAction = { summit, _ ->
            pythonInstance?.let { GpxPyExecutor(it).createSimplifiedGpxTrack(summit.getGpsTrackPath()) }
        }),
    YAML_EXTENSIONS(
        { it.getYamlExtensionsFile() },
        shouldUpdateRoadInfos = true,
        checkAction = { s ->
            s.getGpsTrackPath().toFile().readText().contains(":TrackPointExtension>")
        },
        updateAction = { summit, backupFile ->
            try {
                pythonInstance?.let { python ->
                    GpxPyExecutor(python).removeExtensionsFromGpxTracks(
                        backupFile, summit.getGpsTrackPath().toFile()
                    )
                }
                Log.i(
                    "FileRowType",
                    "Successfully removeExtensionsFromGpxTracks for ${summit.getDateAsString()}_${summit.name}."
                )
            } catch (e: RuntimeException) {
                Log.e(
                    "FileRowType",
                    "RemoveExtensionsFromGpxTracks failed for ${summit.getDateAsString()}_${summit.name}.",
                    e
                )
            }
        }),
    GPXPY_JSON(
        { it.getGpxPyPath().toFile() },
        updateAction = { summit, _ ->
            pythonInstance?.let { python ->
                GpxPyExecutor(python).analyzeGpxTrackAndCreateGpxPyDataFile(summit)
            }
        });
}

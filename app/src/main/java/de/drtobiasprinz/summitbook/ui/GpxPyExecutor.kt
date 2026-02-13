package de.drtobiasprinz.summitbook.ui

import android.util.Log
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.Summit.Companion.subDirForGpsTrackExtensions
import de.drtobiasprinz.summitbook.db.entities.Summit.Companion.subDirForGpsTracksBookmarkExtensions
import de.drtobiasprinz.summitbook.db.entities.Summit.Companion.subDirForGpsTracksSimplified
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.activitiesDir
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.storage
import java.io.File
import java.nio.file.Path

class GpxPyExecutor(private var pythonInstance: Python) {
    private lateinit var pythonModule: PyObject

    fun analyzeGpxTrackAndCreateGpxPyDataFile(summit: Summit) {
        val targetFolder = File(
            storage,
            if (!summit.isBookmark) subDirForGpsTrackExtensions else subDirForGpsTracksBookmarkExtensions
        )
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }

        val splitFiles = (summit.garminData?.activityIds ?: emptyList())
            .map { File(activitiesDir, "activity_${it}_splits.json") }
            .filter { it.exists() }
            .map { it.absolutePath }
        Log.i(TAG, "Found ${splitFiles.size} files with split data: $splitFiles")
        pythonModule = pythonInstance.getModule("entry_point")
        val result =
            pythonModule.callAttr(
                "analyze_gpx_track",
                summit.getGpsTrackPath().toFile().absolutePath,
                summit.getYamlExtensionsFile().absolutePath,
                targetFolder.absolutePath,
                splitFiles.toTypedArray()
            )
        checkOutput(result)
    }

    fun createSimplifiedGpxTrack(originalGpxTrackPath: Path) {
        val targetFolder = File(storage, subDirForGpsTracksSimplified)
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }
        pythonModule = pythonInstance.getModule("entry_point")
        val result =
            pythonModule.callAttr(
                "simplify_gpx_track",
                originalGpxTrackPath.toFile().absolutePath,
                targetFolder.absolutePath
            )
        checkOutput(result)
    }

    fun mergeGpxTracks(
        tracksToMerge: List<File>,
        yamlExtensionsToMerge: List<File>,
        gpxFile: File,
        name: String,
        yamlExtensionsFile: File?
    ) {
        pythonModule = pythonInstance.getModule("entry_point")
        val result = pythonModule.callAttr(
            "merge_tracks",
            tracksToMerge.map { it.absolutePath }.toTypedArray(),
            yamlExtensionsToMerge.map { it.absolutePath }.toTypedArray(),
            gpxFile.absolutePath,
            name,
            yamlExtensionsFile?.absolutePath
        )
        checkOutput(result)
    }

    fun removeExtensionsFromGpxTracks(input: File, export: File) {
        pythonModule = pythonInstance.getModule("entry_point")
        val result = pythonModule.callAttr(
            "remove_extensions_from_gpx_track",
            input.absolutePath,
            export.absolutePath
        )
        checkOutput(result)
    }

    private fun checkOutput(result: PyObject?) {
        if (result == null || result.toString() == "") {
            throw RuntimeException("Execution failed")
        }
        if (result.toString().startsWith("return code: 1")) {
            throw RuntimeException(result.toString().replace("return code: 1", ""))
        }
    }

    companion object {
        const val TAG = "GpxPyExecutor"
    }
}
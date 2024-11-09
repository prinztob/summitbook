package de.drtobiasprinz.summitbook.ui

import com.chaquo.python.PyObject
import com.chaquo.python.Python
import de.drtobiasprinz.summitbook.db.entities.Summit.Companion.subDirForGpsTracks
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.cache
import java.io.File
import java.nio.file.Path

class GpxPyExecutor(private var pythonInstance: Python) {
    private lateinit var pythonModule: PyObject

    fun analyzeGpxTrackAndCreateGpxPyDataFile(originalGpxTrackPath: Path) {
        val targetFolder = File(cache, subDirForGpsTracks)
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }
        pythonModule = pythonInstance.getModule("entry_point")
        val result =
            pythonModule.callAttr(
                "analyze_gpx_track",
                originalGpxTrackPath.toFile().absolutePath,
                targetFolder.absolutePath
            )
        checkOutput(result)
    }

    fun createSimplifiedGpxTrack(originalGpxTrackPath: Path) {
        val targetFolder = File(cache, subDirForGpsTracks)
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

    fun mergeGpxTracks(tracksToMerge: List<File>, output: File, name: String) {
        pythonModule = pythonInstance.getModule("entry_point")
        val result = pythonModule.callAttr(
            "merge_tracks",
            tracksToMerge.map { it.absolutePath }.toTypedArray(),
            output.absolutePath,
            name
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
}
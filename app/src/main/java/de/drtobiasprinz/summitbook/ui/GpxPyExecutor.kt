package de.drtobiasprinz.summitbook.ui

import com.chaquo.python.PyObject
import com.chaquo.python.Python
import java.io.File
import java.nio.file.Path

class GpxPyExecutor(private var pythonInstance: Python) {
    private lateinit var pythonModule: PyObject

    fun createSimplifiedGpxTrack(originalGpxTrackPath: Path) {
        pythonModule = pythonInstance.getModule("entry_point")
        val result =
            pythonModule.callAttr("analyze_gpx_track", originalGpxTrackPath.toFile().absolutePath)
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
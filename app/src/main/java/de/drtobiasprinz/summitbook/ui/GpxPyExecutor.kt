package de.drtobiasprinz.summitbook.ui

import com.chaquo.python.PyObject
import com.chaquo.python.Python
import java.nio.file.Path

class GpxPyExecutor(var pythonInstance: Python) {
    lateinit var pythonModule: PyObject

    fun createSimplifiedGpxTrack(originalGpxTrackPath: Path) {
        pythonModule = pythonInstance.getModule("start")
        val result = pythonModule.callAttr("analyze_gpx_track", originalGpxTrackPath.toFile().absolutePath)
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
package de.drtobiasprinz.summitbook.data.appstate

import android.content.Context
import android.content.SharedPreferences
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import de.drtobiasprinz.summitbook.data.db.entities.Peak
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import java.io.File

/**
 * Process-wide application state that used to live in the MainActivityCompose companion object.
 * Initialized by the launcher activities (MainActivityCompose/ReceiverActivityCompose).
 */
object AppState {
    var peaks: MutableList<Peak> = mutableListOf()
    var storage: File? = null
    var cache: File? = null
    var activitiesDir: File? = null
    var heatmapDir: File? = null
    var segmentScreenshotDir: File? = null
    var pythonInstance: Python? = null
    var activitiesWithPowerRecordsFiltered: List<Long> = emptyList()
    var activitiesWithPowerRecordsLast5Years: List<Long> = emptyList()
    var activitiesWithPowerRecordsAll: List<Long> = emptyList()
    var activitiesWithVerticalVelocityRecordsFiltered: List<Long> = emptyList()
    var activitiesWithVerticalVelocityRecordsLast5Years: List<Long> = emptyList()
    var activitiesWithVerticalVelocityRecordsAll: List<Long> = emptyList()
    var activitiesWithAverageVelocityRecordsFiltered: List<Long> = emptyList()
    var activitiesWithAverageVelocityRecordsLast5Years: List<Long> = emptyList()
    var activitiesWithAverageVelocityRecordsAll: List<Long> = emptyList()
    var activitiesWithSegmentsRecord: MutableList<Pair<Long, Int>> = mutableListOf()
    lateinit var sharedPreferences: SharedPreferences
    var latestFilteredSummits: List<Summit> = emptyList()

    fun getOrCreatePython(context: Context): Python? {
        if (pythonInstance == null) {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
            pythonInstance = Python.getInstance()
        }
        return pythonInstance
    }
}

package de.drtobiasprinz.summitbook.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment.Companion.activitiesDir
import de.drtobiasprinz.summitbook.models.ElevationData
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.models.VelocityData
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.utils.JsonUtils
import de.drtobiasprinz.summitbook.ui.utils.MaxVelocitySummit
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import de.drtobiasprinz.summitbook.ui.utils.SummitSlope
import java.io.File
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt


class AddAdditionalDataFromExternalResourcesDialog(private val summitEntry: Summit, private val pythonExecutor: GarminPythonExecutor, val sortFilterHelper: SortFilterHelper, val button: ImageButton) : DialogFragment() {

    private lateinit var currentContext: Context
    private lateinit var updateDataButton: Button
    private lateinit var deleteDataButton: Button
    private lateinit var backButton: Button
    private lateinit var tableLayout: TableLayout
    private var tableEntries: MutableList<TableEntry> = mutableListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setRetainInstance(true)
        return inflater.inflate(R.layout.dialog_add_additional_data_from_external_resources, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentContext = requireContext()
        updateDataButton = view.findViewById(R.id.update_data)
        updateDataButton.setOnClickListener {
            val copyElevationData = summitEntry.elevationData.clone()
            val copyVelocityData = summitEntry.velocityData.clone()
            tableEntries.forEach { it.update() }
            if (copyElevationData != summitEntry.elevationData || copyVelocityData != summitEntry.velocityData) sortFilterHelper.database.summitDao()?.updateSummit(summitEntry)
            if (summitEntry.elevationData.hasAdditionalData() || summitEntry.velocityData.hasAdditionalData()) {
                button.setImageResource(R.drawable.ic_baseline_speed_24)
            } else {
                button.setImageResource(R.drawable.ic_baseline_more_time_24)
            }
            dialog?.cancel()
        }
        deleteDataButton = view.findViewById(R.id.delete_data)
        deleteDataButton.setOnClickListener {
            summitEntry.velocityData = VelocityData(summitEntry.velocityData.avgVelocity, summitEntry.velocityData.maxVelocity, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
            summitEntry.elevationData = ElevationData(summitEntry.elevationData.maxElevation, summitEntry.elevationData.elevationGain)
            sortFilterHelper.database.summitDao()?.updateSummit(summitEntry)
            dialog?.cancel()
        }
        backButton = view.findViewById(R.id.back)
        backButton.setOnClickListener {
            dialog?.cancel()
        }
        if (summitEntry.hasGpsTrack()) {
            summitEntry.setGpsTrack(useSimplifiedTrack = false, updateTrack = true)
            val gpsTrack = summitEntry.gpsTrack
            if (gpsTrack != null) {
                if (gpsTrack.hasNoTrackPoints()) {
                    gpsTrack.parseTrack(useSimplifiedIfExists = false)
                }
                val slopeCalculator = SummitSlope(gpsTrack.trackPoints)
                tableEntries.add(TableEntry(getString(R.string.max_slope),
                        if (summitEntry.elevationData.maxSlope > 0.0) summitEntry.elevationData.maxSlope else slopeCalculator.calculateMaxSlope(),
                        "%", summitEntry.elevationData.maxSlope > 0.0,
                        { e -> summitEntry.elevationData.maxSlope = e }))
                tableEntries.add(TableEntry(getString(R.string.max_verticalVelocity_1Min),
                        if (summitEntry.elevationData.maxVerticalVelocity1Min > 0.0) summitEntry.elevationData.maxVerticalVelocity1Min else slopeCalculator.calculateMaxVerticalVelocity( 60.0),
                        "m/s", summitEntry.elevationData.maxVerticalVelocity1Min > 0.0,
                        { e -> summitEntry.elevationData.maxVerticalVelocity1Min = e }))
                tableEntries.add(TableEntry(getString(R.string.max_verticalVelocity_10Min),
                        if (summitEntry.elevationData.maxVerticalVelocity10Min > 0.0) summitEntry.elevationData.maxVerticalVelocity10Min else slopeCalculator.calculateMaxVerticalVelocity( 600.0),
                        "m/s", summitEntry.elevationData.maxVerticalVelocity10Min > 0.0,
                        { e -> summitEntry.elevationData.maxVerticalVelocity10Min = e }))
                tableEntries.add(TableEntry(getString(R.string.max_verticalVelocity_1h),
                        if (summitEntry.elevationData.maxVerticalVelocity1h > 0.0) summitEntry.elevationData.maxVerticalVelocity1h else slopeCalculator.calculateMaxVerticalVelocity( 3600.0),
                        "m/s", summitEntry.elevationData.maxVerticalVelocity1h > 0.0,
                        { e -> summitEntry.elevationData.maxVerticalVelocity1h = e }))
            }
        }
        if (summitEntry.garminData != null && summitEntry.garminData?.activityId != null) {
            val splitsFile = File("${activitiesDir.absolutePath}/activity_${summitEntry.garminData?.activityId}_splits.json")
            if (splitsFile.exists()) {
                val json = JsonParser().parse(JsonUtils.getJsonData(splitsFile)) as JsonObject
                val maxVelocitySummit = MaxVelocitySummit()
                val velocityEntries = maxVelocitySummit.parseFomGarmin(json)
                tableEntries.add(TableEntry(getString(R.string.top_speed_1km_hint),
                        if (summitEntry.velocityData.oneKilometer > 0.0) summitEntry.velocityData.oneKilometer else maxVelocitySummit.getAverageVelocityForKilometers(1.0, velocityEntries),
                        "km/h", summitEntry.velocityData.oneKilometer > 0.0, { e -> summitEntry.velocityData.oneKilometer = e }))
                if (tableEntries.last().value > 0) {
                    tableEntries.add(TableEntry(getString(R.string.top_speed_5km_hint),
                            if (summitEntry.velocityData.fiveKilometer > 0.0) summitEntry.velocityData.fiveKilometer else maxVelocitySummit.getAverageVelocityForKilometers(5.0, velocityEntries),
                            "km/h", summitEntry.velocityData.fiveKilometer > 0.0, { e -> summitEntry.velocityData.fiveKilometer = e }))
                }
                if (tableEntries.last().value > 0) {
                    tableEntries.add(TableEntry(getString(R.string.top_speed_10km_hint),
                            if (summitEntry.velocityData.tenKilometers > 0.0) summitEntry.velocityData.tenKilometers else maxVelocitySummit.getAverageVelocityForKilometers(10.0, velocityEntries),
                            "km/h", summitEntry.velocityData.tenKilometers > 0.0, { e -> summitEntry.velocityData.tenKilometers = e }))
                }
                if (tableEntries.last().value > 0) {
                    tableEntries.add(TableEntry(getString(R.string.top_speed_15km_hint),
                            if (summitEntry.velocityData.fifteenKilometers > 0.0) summitEntry.velocityData.fifteenKilometers else maxVelocitySummit.getAverageVelocityForKilometers(15.0, velocityEntries),
                            "km/h", summitEntry.velocityData.fifteenKilometers > 0.0, { e -> summitEntry.velocityData.fifteenKilometers = e }))
                }
                if (tableEntries.last().value > 0) {
                    tableEntries.add(TableEntry(getString(R.string.top_speed_20km_hint),
                            if (summitEntry.velocityData.twentyKilometers > 0.0) summitEntry.velocityData.twentyKilometers else maxVelocitySummit.getAverageVelocityForKilometers(20.0, velocityEntries),
                            "km/h", summitEntry.velocityData.twentyKilometers > 0.0, { e -> summitEntry.velocityData.twentyKilometers = e }))
                }
                if (tableEntries.last().value > 0) {
                    tableEntries.add(TableEntry(getString(R.string.top_speed_30km_hint),
                            if (summitEntry.velocityData.thirtyKilometers > 0.0) summitEntry.velocityData.thirtyKilometers else maxVelocitySummit.getAverageVelocityForKilometers(30.0, velocityEntries),
                            "km/h", summitEntry.velocityData.thirtyKilometers > 0.0, { e -> summitEntry.velocityData.thirtyKilometers = e }))
                }
                if (tableEntries.last().value > 0) {
                    tableEntries.add(TableEntry(getString(R.string.top_speed_40km_hint),
                            if (summitEntry.velocityData.fortyKilometers > 0.0) summitEntry.velocityData.fortyKilometers else maxVelocitySummit.getAverageVelocityForKilometers(40.0, velocityEntries),
                            "km/h", summitEntry.velocityData.fortyKilometers > 0.0, { e -> summitEntry.velocityData.fortyKilometers = e }))
                }
                if (tableEntries.last().value > 0) {
                    tableEntries.add(TableEntry(getString(R.string.top_speed_50km_hint),
                            if (summitEntry.velocityData.fiftyKilometers > 0.0) summitEntry.velocityData.fiftyKilometers else maxVelocitySummit.getAverageVelocityForKilometers(50.0, velocityEntries),
                            "km/h", summitEntry.velocityData.fiftyKilometers > 0.0, { e -> summitEntry.velocityData.fiftyKilometers = e }))
                }
                if (tableEntries.last().value > 0) {
                    tableEntries.add(TableEntry(getString(R.string.top_speed_75km_hint),
                            if (summitEntry.velocityData.seventyFiveKilometers > 0.0) summitEntry.velocityData.seventyFiveKilometers else maxVelocitySummit.getAverageVelocityForKilometers(75.0, velocityEntries),
                            "km/h", summitEntry.velocityData.seventyFiveKilometers > 0.0, { e -> summitEntry.velocityData.seventyFiveKilometers = e }))
                }
                if (tableEntries.last().value > 0) {
                    tableEntries.add(TableEntry(getString(R.string.top_speed_100km_hint),
                            if (summitEntry.velocityData.hundredKilometers > 0.0) summitEntry.velocityData.hundredKilometers else maxVelocitySummit.getAverageVelocityForKilometers(100.0, velocityEntries),
                            "km/h", summitEntry.velocityData.hundredKilometers > 0.0, { e -> summitEntry.velocityData.hundredKilometers = e }))
                }
            } else {
                AsyncDownloadSpeedDataForActivity(summitEntry, pythonExecutor, sortFilterHelper, button).execute()
            }
        }
        val gpxPyJsonFile = summitEntry.getGpxPyPath().toFile()
        if (gpxPyJsonFile.exists()) {
            val gpxPyJson = JsonParser().parse(JsonUtils.getJsonData(gpxPyJsonFile)) as JsonObject

            val elevationGain = gpxPyJson.getAsJsonPrimitive("elevation_gain").asDouble.roundToInt()
            if (elevationGain > 0) {
                tableEntries.add(TableEntry(getString(R.string.height_meter_hint),
                        elevationGain.toDouble(),
                        "hm", summitEntry.elevationData.elevationGain == elevationGain,
                        { e -> summitEntry.elevationData.elevationGain = e.toInt() },
                        summitEntry.elevationData.elevationGain.toDouble())
                )
            }

            val maxElevation = gpxPyJson.getAsJsonPrimitive("max_elevation").asDouble.roundToInt()
            if (maxElevation > 0) {
                tableEntries.add(TableEntry(getString(R.string.top_elevation_hint),
                        maxElevation.toDouble(),
                        "hm", summitEntry.elevationData.maxElevation == maxElevation,
                        { e -> summitEntry.elevationData.maxElevation = e.toInt() },
                        summitEntry.elevationData.maxElevation.toDouble())
                )
            }

            val distance = gpxPyJson.getAsJsonPrimitive("moving_distance").asDouble / 1000
            if (distance > 0) {
                tableEntries.add(TableEntry(getString(R.string.kilometers_hint),
                        distance,
                        "km", abs(summitEntry.kilometers - distance) < 0.05,
                        { e -> summitEntry.kilometers = e },
                        summitEntry.kilometers)
                )
            }

            val movingDuration = gpxPyJson.getAsJsonPrimitive("moving_time").asDouble
            if (movingDuration > 0) {
                val pace = distance / movingDuration * 3600
                tableEntries.add(TableEntry(getString(R.string.pace_hint),
                        pace,
                        "hm/h", abs(summitEntry.velocityData.avgVelocity - pace) < 0.05,
                        { e -> summitEntry.velocityData.avgVelocity = e },
                        summitEntry.velocityData.avgVelocity)
                )
            }

            val maxVelocity = gpxPyJson.getAsJsonPrimitive("max_speed").asDouble * 3.6
            if (maxVelocity > 0) {
                tableEntries.add(TableEntry(getString(R.string.top_speed),
                        maxVelocity,
                        "hm/h", abs(summitEntry.velocityData.maxVelocity - maxVelocity) < 0.05,
                        { e -> summitEntry.velocityData.maxVelocity = e },
                        summitEntry.velocityData.maxVelocity)
                )
            }
        }
        tableLayout = view.findViewById(R.id.tableSummits) as TableLayout
        drawTable(view)

    }

    private fun drawTable(view: View) {
        addHeader(view, tableLayout)
        tableEntries.forEachIndexed { index, entry ->
            if (entry.value > 0.0) {
                addSummitToTable(entry, view, index, tableLayout)
            } else {
                entry.isSet = false
            }
        }
    }

    private fun addSummitToTable(entry: TableEntry, view: View, i: Int, tl: TableLayout) {
        val name: String = entry.name.chunked(20).joinToString("\n")
        val tr = TableRow(view.context)
        tr.setBackgroundColor(Color.GRAY)
        tr.id = 100 + i
        tr.layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
        addLabel(view, tr, 200 + i, name, padding = 2)
        addLabel(view, tr, 201 + i, String.format(Locale.ENGLISH, "%.1f", entry.value), padding = 2, aligment = View.TEXT_ALIGNMENT_TEXT_END)
        val defaultValueAsString = if (abs(entry.defaultValue) < 0.05 || abs(entry.value - entry.defaultValue) < 0.05) "-" else String.format(Locale.ENGLISH, "%.1f", entry.defaultValue)
        addLabel(view, tr, 202 + i, defaultValueAsString, padding = 2, aligment = View.TEXT_ALIGNMENT_TEXT_END)
        addLabel(view, tr, 203 + i, entry.unit, padding = 2, aligment = View.TEXT_ALIGNMENT_TEXT_END)
        val box = CheckBox(view.context)
        box.isChecked = entry.isChecked
        box.setOnCheckedChangeListener { _, arg1 ->
            entry.isChecked = arg1
        }

        tr.addView(box)
        tl.addView(tr, TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun addHeader(view: View, tl: TableLayout) {
        val tableRowHead = TableRow(view.context)
        10.also { tableRowHead.id = it }
        tableRowHead.setBackgroundColor(Color.WHITE)
        tableRowHead.layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
        addLabel(view, tableRowHead, 20, "Entry", Color.GRAY)
        //TODO: i8
        addLabel(view, tableRowHead, 21, "update", Color.GRAY)
        addLabel(view, tableRowHead, 22, "current", Color.GRAY)
        addLabel(view, tableRowHead, 23, "unit", Color.GRAY)
        addLabel(view, tableRowHead, 24, "", Color.GRAY)

        tl.addView(tableRowHead, TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun addLabel(view: View, tr: TableRow, id: Int, text: String, color: Int = Color.WHITE, padding: Int = 5, aligment: Int = View.TEXT_ALIGNMENT_CENTER) {
        val label = TextView(view.context)
        label.id = id
        label.text = text
        label.gravity = aligment
        label.setTextColor(color)
        label.setPadding(padding, padding, padding, padding)
        tr.addView(label)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        class AsyncDownloadSpeedDataForActivity(private val summit: Summit, private val pythonExecutor: GarminPythonExecutor, var sortFilterHelper: SortFilterHelper, val addVelocityData: ImageButton) : AsyncTask<Void?, Void?, Void?>() {
            var json: JsonObject? = null
            override fun doInBackground(vararg params: Void?): Void? {
                try {
                    summit.garminData?.activityId?.let { json = pythonExecutor.downloadSpeedDataForActivity(activitiesDir, it) }
                } catch (e: java.lang.RuntimeException) {
                    Log.e("AsyncDownloadActivities", e.message ?: "")
                }
                return null
            }

            override fun onPostExecute(param: Void?) {
                val jsonLocal = json
                if (jsonLocal != null) {
                    setVelocityData(jsonLocal, summit, sortFilterHelper, addVelocityData)
                }
            }
        }

        fun setVelocityData(jsonLocal: JsonObject, summit: Summit, sortFilterHelper: SortFilterHelper, addVelocityData: ImageButton? = null) {
            val maxVelocitySummit = MaxVelocitySummit()
            val velocityEntries = maxVelocitySummit.parseFomGarmin(jsonLocal)
            summit.velocityData.oneKilometer = maxVelocitySummit.getAverageVelocityForKilometers(1.0, velocityEntries)
            if (summit.velocityData.oneKilometer > 0) summit.velocityData.fiveKilometer = maxVelocitySummit.getAverageVelocityForKilometers(5.0, velocityEntries)
            if (summit.velocityData.fiveKilometer > 0) summit.velocityData.tenKilometers = maxVelocitySummit.getAverageVelocityForKilometers(10.0, velocityEntries)
            if (summit.velocityData.tenKilometers > 0) summit.velocityData.fifteenKilometers = maxVelocitySummit.getAverageVelocityForKilometers(15.0, velocityEntries)
            if (summit.velocityData.fifteenKilometers > 0) summit.velocityData.twentyKilometers = maxVelocitySummit.getAverageVelocityForKilometers(20.0, velocityEntries)
            if (summit.velocityData.twentyKilometers > 0) summit.velocityData.thirtyKilometers = maxVelocitySummit.getAverageVelocityForKilometers(30.0, velocityEntries)
            if (summit.velocityData.thirtyKilometers > 0) summit.velocityData.fortyKilometers = maxVelocitySummit.getAverageVelocityForKilometers(40.0, velocityEntries)
            if (summit.velocityData.fortyKilometers > 0) summit.velocityData.fiftyKilometers = maxVelocitySummit.getAverageVelocityForKilometers(50.0, velocityEntries)
            if (summit.velocityData.fiftyKilometers > 0) summit.velocityData.seventyFiveKilometers = maxVelocitySummit.getAverageVelocityForKilometers(75.0, velocityEntries)
            if (summit.velocityData.seventyFiveKilometers > 0) summit.velocityData.hundredKilometers = maxVelocitySummit.getAverageVelocityForKilometers(100.0, velocityEntries)
            sortFilterHelper.database.summitDao()?.updateSummit(summit)
            addVelocityData?.setImageResource(R.drawable.ic_baseline_speed_24)
        }

    }

    class TableEntry(var name: String, var value: Double, var unit: String, var isChecked: Boolean, var f: (Double) -> Unit, var defaultValue: Double = 0.0) {
        var isSet: Boolean = true
        fun update() {
            if (isSet) {
                if (isChecked) {
                    f(value)
                } else {
                    f(defaultValue)
                }
            }
        }
    }

}

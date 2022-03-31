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
import de.drtobiasprinz.summitbook.MainActivity.Companion.activitiesDir
import de.drtobiasprinz.summitbook.models.ElevationData
import de.drtobiasprinz.summitbook.models.FragmentResultReceiver
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.models.VelocityData
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.utils.JsonUtils
import de.drtobiasprinz.summitbook.ui.utils.MaxVelocitySummit
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import de.drtobiasprinz.summitbook.ui.utils.SummitSlope
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt


class AddAdditionalDataFromExternalResourcesDialog : DialogFragment() {

    private lateinit var currentContext: Context
    private lateinit var updateDataButton: Button
    private lateinit var deleteDataButton: Button
    private lateinit var backButton: Button
    private lateinit var tableLayout: TableLayout
    private var tableEntries: MutableList<TableEntry> = mutableListOf()
    private lateinit var resultReceiver: FragmentResultReceiver
    private var summitEntry: Summit? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        resultReceiver = context as FragmentResultReceiver
        return inflater.inflate(R.layout.dialog_add_additional_data_from_external_resources, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentContext = requireContext()
        updateDataButton = view.findViewById(R.id.update_data)
        deleteDataButton = view.findViewById(R.id.delete_data)
        backButton = view.findViewById(R.id.back)
        if (savedInstanceState != null &&  summitEntry == null) {
            val summitEntryId = savedInstanceState.getLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER)
            summitEntry = resultReceiver.getSortFilterHelper().database.summitDao()?.getSummit(summitEntryId)
        }
        val localSummitEntry = summitEntry
        if (localSummitEntry != null) {
            updateDataButton.setOnClickListener {
                val copyElevationData = localSummitEntry.elevationData.clone()
                val copyVelocityData = localSummitEntry.velocityData.clone()
                tableEntries.forEach { it.update() }
                if (copyElevationData != localSummitEntry.elevationData || copyVelocityData != localSummitEntry.velocityData) resultReceiver.getSortFilterHelper().database.summitDao()?.updateSummit(localSummitEntry)
                dialog?.cancel()
            }
            deleteDataButton.setOnClickListener {
                localSummitEntry.velocityData = VelocityData(localSummitEntry.velocityData.avgVelocity, localSummitEntry.velocityData.maxVelocity, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
                localSummitEntry.elevationData = ElevationData(localSummitEntry.elevationData.maxElevation, localSummitEntry.elevationData.elevationGain)
                resultReceiver.getSortFilterHelper().database.summitDao()?.updateSummit(localSummitEntry)
                dialog?.cancel()
            }
            backButton.setOnClickListener {
                dialog?.cancel()
            }
            if (localSummitEntry.hasGpsTrack()) {
                localSummitEntry.setGpsTrack(useSimplifiedTrack = false, updateTrack = true)
                val gpsTrack = localSummitEntry.gpsTrack
                if (gpsTrack != null) {
                    if (gpsTrack.hasNoTrackPoints()) {
                        gpsTrack.parseTrack(useSimplifiedIfExists = false)
                    }
                    val slopeCalculator = SummitSlope(gpsTrack.trackPoints)
                    tableEntries.add(TableEntry(getString(R.string.max_slope),
                            if (localSummitEntry.elevationData.maxSlope > 0.0) localSummitEntry.elevationData.maxSlope else slopeCalculator.calculateMaxSlope(),
                            "%", localSummitEntry.elevationData.maxSlope > 0.0,
                            { e -> localSummitEntry.elevationData.maxSlope = e }))
                    tableEntries.add(TableEntry(getString(R.string.max_verticalVelocity_1Min),
                            if (localSummitEntry.elevationData.maxVerticalVelocity1Min > 0.0) localSummitEntry.elevationData.maxVerticalVelocity1Min else slopeCalculator.calculateMaxVerticalVelocity(60.0),
                            "m/s", localSummitEntry.elevationData.maxVerticalVelocity1Min > 0.0,
                            { e -> localSummitEntry.elevationData.maxVerticalVelocity1Min = e }))
                    tableEntries.add(TableEntry(getString(R.string.max_verticalVelocity_10Min),
                            if (localSummitEntry.elevationData.maxVerticalVelocity10Min > 0.0) localSummitEntry.elevationData.maxVerticalVelocity10Min else slopeCalculator.calculateMaxVerticalVelocity(600.0),
                            "m/s", localSummitEntry.elevationData.maxVerticalVelocity10Min > 0.0,
                            { e -> localSummitEntry.elevationData.maxVerticalVelocity10Min = e }))
                    tableEntries.add(TableEntry(getString(R.string.max_verticalVelocity_1h),
                            if (localSummitEntry.elevationData.maxVerticalVelocity1h > 0.0) localSummitEntry.elevationData.maxVerticalVelocity1h else slopeCalculator.calculateMaxVerticalVelocity(3600.0),
                            "m/s", localSummitEntry.elevationData.maxVerticalVelocity1h > 0.0,
                            { e -> localSummitEntry.elevationData.maxVerticalVelocity1h = e }))
                }
            }
            if (localSummitEntry.garminData != null && localSummitEntry.garminData?.activityId != null) {
                val splitsFile = File("${activitiesDir?.absolutePath}/activity_${localSummitEntry.garminData?.activityId}_splits.json")
                if (splitsFile.exists()) {
                    val json = JsonParser().parse(JsonUtils.getJsonData(splitsFile)) as JsonObject
                    val maxVelocitySummit = MaxVelocitySummit()
                    val velocityEntries = maxVelocitySummit.parseFomGarmin(json)
                    tableEntries.add(TableEntry(getString(R.string.top_speed_1km_hint),
                            if (localSummitEntry.velocityData.oneKilometer > 0.0) localSummitEntry.velocityData.oneKilometer else maxVelocitySummit.getAverageVelocityForKilometers(1.0, velocityEntries),
                            "km/h", localSummitEntry.velocityData.oneKilometer > 0.0, { e -> localSummitEntry.velocityData.oneKilometer = e }))
                    if (tableEntries.last().value > 0) {
                        tableEntries.add(TableEntry(getString(R.string.top_speed_5km_hint),
                                if (localSummitEntry.velocityData.fiveKilometer > 0.0) localSummitEntry.velocityData.fiveKilometer else maxVelocitySummit.getAverageVelocityForKilometers(5.0, velocityEntries),
                                "km/h", localSummitEntry.velocityData.fiveKilometer > 0.0, { e -> localSummitEntry.velocityData.fiveKilometer = e }))
                    }
                    if (tableEntries.last().value > 0) {
                        tableEntries.add(TableEntry(getString(R.string.top_speed_10km_hint),
                                if (localSummitEntry.velocityData.tenKilometers > 0.0) localSummitEntry.velocityData.tenKilometers else maxVelocitySummit.getAverageVelocityForKilometers(10.0, velocityEntries),
                                "km/h", localSummitEntry.velocityData.tenKilometers > 0.0, { e -> localSummitEntry.velocityData.tenKilometers = e }))
                    }
                    if (tableEntries.last().value > 0) {
                        tableEntries.add(TableEntry(getString(R.string.top_speed_15km_hint),
                                if (localSummitEntry.velocityData.fifteenKilometers > 0.0) localSummitEntry.velocityData.fifteenKilometers else maxVelocitySummit.getAverageVelocityForKilometers(15.0, velocityEntries),
                                "km/h", localSummitEntry.velocityData.fifteenKilometers > 0.0, { e -> localSummitEntry.velocityData.fifteenKilometers = e }))
                    }
                    if (tableEntries.last().value > 0) {
                        tableEntries.add(TableEntry(getString(R.string.top_speed_20km_hint),
                                if (localSummitEntry.velocityData.twentyKilometers > 0.0) localSummitEntry.velocityData.twentyKilometers else maxVelocitySummit.getAverageVelocityForKilometers(20.0, velocityEntries),
                                "km/h", localSummitEntry.velocityData.twentyKilometers > 0.0, { e -> localSummitEntry.velocityData.twentyKilometers = e }))
                    }
                    if (tableEntries.last().value > 0) {
                        tableEntries.add(TableEntry(getString(R.string.top_speed_30km_hint),
                                if (localSummitEntry.velocityData.thirtyKilometers > 0.0) localSummitEntry.velocityData.thirtyKilometers else maxVelocitySummit.getAverageVelocityForKilometers(30.0, velocityEntries),
                                "km/h", localSummitEntry.velocityData.thirtyKilometers > 0.0, { e -> localSummitEntry.velocityData.thirtyKilometers = e }))
                    }
                    if (tableEntries.last().value > 0) {
                        tableEntries.add(TableEntry(getString(R.string.top_speed_40km_hint),
                                if (localSummitEntry.velocityData.fortyKilometers > 0.0) localSummitEntry.velocityData.fortyKilometers else maxVelocitySummit.getAverageVelocityForKilometers(40.0, velocityEntries),
                                "km/h", localSummitEntry.velocityData.fortyKilometers > 0.0, { e -> localSummitEntry.velocityData.fortyKilometers = e }))
                    }
                    if (tableEntries.last().value > 0) {
                        tableEntries.add(TableEntry(getString(R.string.top_speed_50km_hint),
                                if (localSummitEntry.velocityData.fiftyKilometers > 0.0) localSummitEntry.velocityData.fiftyKilometers else maxVelocitySummit.getAverageVelocityForKilometers(50.0, velocityEntries),
                                "km/h", localSummitEntry.velocityData.fiftyKilometers > 0.0, { e -> localSummitEntry.velocityData.fiftyKilometers = e }))
                    }
                    if (tableEntries.last().value > 0) {
                        tableEntries.add(TableEntry(getString(R.string.top_speed_75km_hint),
                                if (localSummitEntry.velocityData.seventyFiveKilometers > 0.0) localSummitEntry.velocityData.seventyFiveKilometers else maxVelocitySummit.getAverageVelocityForKilometers(75.0, velocityEntries),
                                "km/h", localSummitEntry.velocityData.seventyFiveKilometers > 0.0, { e -> localSummitEntry.velocityData.seventyFiveKilometers = e }))
                    }
                    if (tableEntries.last().value > 0) {
                        tableEntries.add(TableEntry(getString(R.string.top_speed_100km_hint),
                                if (localSummitEntry.velocityData.hundredKilometers > 0.0) localSummitEntry.velocityData.hundredKilometers else maxVelocitySummit.getAverageVelocityForKilometers(100.0, velocityEntries),
                                "km/h", localSummitEntry.velocityData.hundredKilometers > 0.0, { e -> localSummitEntry.velocityData.hundredKilometers = e }))
                    }
                } else {
                    AsyncDownloadSpeedDataForActivity(localSummitEntry, resultReceiver.getPythonExecutor(), resultReceiver.getSortFilterHelper()).execute()
                }
            }
            val gpxPyJsonFile = localSummitEntry.getGpxPyPath().toFile()
            if (gpxPyJsonFile.exists()) {
                val gpxPyJson = JsonParser().parse(JsonUtils.getJsonData(gpxPyJsonFile)) as JsonObject

                val elevationGain = gpxPyJson.getAsJsonPrimitive("elevation_gain").asDouble.roundToInt()
                if (elevationGain > 0) {
                    tableEntries.add(TableEntry(getString(R.string.height_meter_hint),
                            elevationGain.toDouble(),
                            "hm", localSummitEntry.elevationData.elevationGain == elevationGain,
                            { e -> localSummitEntry.elevationData.elevationGain = e.toInt() },
                            localSummitEntry.elevationData.elevationGain.toDouble())
                    )
                }

                val maxElevation = gpxPyJson.getAsJsonPrimitive("max_elevation").asDouble.roundToInt()
                if (maxElevation > 0) {
                    tableEntries.add(TableEntry(getString(R.string.top_elevation_hint),
                            maxElevation.toDouble(),
                            "hm", localSummitEntry.elevationData.maxElevation == maxElevation,
                            { e -> localSummitEntry.elevationData.maxElevation = e.toInt() },
                            localSummitEntry.elevationData.maxElevation.toDouble())
                    )
                }

                val distance = gpxPyJson.getAsJsonPrimitive("moving_distance").asDouble / 1000
                if (distance > 0) {
                    tableEntries.add(TableEntry(getString(R.string.kilometers_hint),
                            distance,
                            "km", abs(localSummitEntry.kilometers - distance) < 0.05,
                            { e -> localSummitEntry.kilometers = e },
                            localSummitEntry.kilometers)
                    )
                }

                val movingDuration = gpxPyJson.getAsJsonPrimitive("moving_time").asDouble
                if (movingDuration > 0) {
                    val pace = distance / movingDuration * 3600
                    tableEntries.add(TableEntry(getString(R.string.pace_hint),
                            pace,
                            "km/h", abs(localSummitEntry.velocityData.avgVelocity - pace) < 0.05,
                            { e -> localSummitEntry.velocityData.avgVelocity = e },
                            localSummitEntry.velocityData.avgVelocity)
                    )
                }

                val maxVelocity = gpxPyJson.getAsJsonPrimitive("max_speed").asDouble * 3.6
                if (maxVelocity > 0) {
                    tableEntries.add(TableEntry(getString(R.string.top_speed),
                            maxVelocity,
                            "hm/h", abs(localSummitEntry.velocityData.maxVelocity - maxVelocity) < 0.05,
                            { e -> localSummitEntry.velocityData.maxVelocity = e },
                            localSummitEntry.velocityData.maxVelocity)
                    )
                }
            }
            tableLayout = view.findViewById(R.id.tableSummits) as TableLayout
            drawTable(view)
        }

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
        addLabel(view, tr, 201 + i, String.format(requireContext().resources.configuration.locales[0], "%.1f", entry.value), padding = 2, alignment = View.TEXT_ALIGNMENT_TEXT_END)
        val defaultValueAsString = if (abs(entry.defaultValue) < 0.05 || abs(entry.value - entry.defaultValue) < 0.05) "-" else String.format(requireContext().resources.configuration.locales[0], "%.1f", entry.defaultValue)
        addLabel(view, tr, 202 + i, defaultValueAsString, padding = 2, alignment = View.TEXT_ALIGNMENT_TEXT_END)
        addLabel(view, tr, 203 + i, entry.unit, padding = 2, alignment = View.TEXT_ALIGNMENT_TEXT_END)
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
        addLabel(view, tableRowHead, 20, getString(R.string.entry), Color.GRAY)
        addLabel(view, tableRowHead, 21, getString(R.string.update), Color.GRAY)
        addLabel(view, tableRowHead, 22, getString(R.string.current), Color.GRAY)
        addLabel(view, tableRowHead, 23, getString(R.string.unit), Color.GRAY)
        addLabel(view, tableRowHead, 24, "", Color.GRAY)

        tl.addView(tableRowHead, TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun addLabel(view: View, tr: TableRow, id: Int, text: String, color: Int = Color.WHITE,
                         padding: Int = 5, alignment: Int = View.TEXT_ALIGNMENT_CENTER) {
        val label = TextView(view.context)
        label.id = id
        label.text = text
        label.gravity = alignment
        label.setTextColor(color)
        label.setPadding(padding, padding, padding, padding)
        tr.addView(label)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        summitEntry?.id?.let { outState.putLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, it) }
        super.onSaveInstanceState(outState)
    }

    companion object {

        fun getInstance(entry: Summit): AddAdditionalDataFromExternalResourcesDialog {
            val dialog = AddAdditionalDataFromExternalResourcesDialog()
            dialog.summitEntry = entry
            return dialog
        }

        @SuppressLint("StaticFieldLeak")
        class AsyncDownloadSpeedDataForActivity(private val summit: Summit, private val pythonExecutor: GarminPythonExecutor?, private var sortFilterHelper: SortFilterHelper) : AsyncTask<Void?, Void?, Void?>() {
            private var json: JsonObject? = null
            override fun doInBackground(vararg params: Void?): Void? {
                try {
                    summit.garminData?.activityId?.let { json = pythonExecutor?.downloadSpeedDataForActivity(it) }
                } catch (e: java.lang.RuntimeException) {
                    Log.e("AsyncDownloadActivities", e.message ?: "")
                }
                return null
            }

            override fun onPostExecute(param: Void?) {
                val jsonLocal = json
                if (jsonLocal != null) {
                    setVelocityData(jsonLocal, summit, sortFilterHelper)
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

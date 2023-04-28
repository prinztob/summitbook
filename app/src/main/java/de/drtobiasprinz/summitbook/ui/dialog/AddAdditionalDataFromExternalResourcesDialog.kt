package de.drtobiasprinz.summitbook.ui.dialog

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.SummitsAdapter
import de.drtobiasprinz.summitbook.databinding.DialogAddAdditionalDataFromExternalResourcesBinding
import de.drtobiasprinz.summitbook.db.entities.ElevationData
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.VelocityData
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.activitiesDir
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonInstance
import de.drtobiasprinz.summitbook.ui.utils.JsonUtils
import de.drtobiasprinz.summitbook.ui.utils.MaxVelocitySummit
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@AndroidEntryPoint
class AddAdditionalDataFromExternalResourcesDialog : DialogFragment() {
    private val viewModel: DatabaseViewModel by activityViewModels()

    @Inject
    lateinit var summitsAdapter: SummitsAdapter
    private lateinit var binding: DialogAddAdditionalDataFromExternalResourcesBinding

    private lateinit var currentContext: Context
    private var tableEntries: MutableList<TableEntry> = mutableListOf()
    private var summitEntry: Summit? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogAddAdditionalDataFromExternalResourcesBinding.inflate(
            layoutInflater,
            container,
            false
        )
        currentContext = requireContext()
        binding.back.setOnClickListener {
            dialog?.cancel()
        }
        val localSummit = summitEntry
        if (localSummit != null) {
            setView(localSummit)
        }
        if (savedInstanceState != null && summitEntry == null) {
            val summitEntryId = savedInstanceState.getLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER)
            viewModel.getDetailsSummit(summitEntryId)
            viewModel.summitDetails.observe(this) { itData ->
                itData.data.let { summit ->
                    if (summit != null) {
                        setView(summit)
                    }
                }
            }
        }
        return binding.root
    }

    private fun setView(summit: Summit) {
        binding.updateData.setOnClickListener {
            val copyElevationData = summit.elevationData.clone()
            val copyVelocityData = summit.velocityData.clone()
            tableEntries.forEach { it.update() }
            if (copyElevationData != summit.elevationData || copyVelocityData != summit.velocityData) {
                viewModel.saveSummit(true, summit)
            }
            dialog?.cancel()
        }
        binding.deleteData.setOnClickListener {
            summit.velocityData = VelocityData(
                summit.velocityData.avgVelocity,
                summit.velocityData.maxVelocity
            )
            summit.elevationData = ElevationData(
                summit.elevationData.maxElevation,
                summit.elevationData.elevationGain
            )
            val gpxPyFile = summit.getGpxPyPath().toFile()
            if (gpxPyFile.exists()) {
                gpxPyFile.delete()
            }
            val trackFile = summit.getGpsTrackPath(simplified = true).toFile()
            if (trackFile.exists()) {
                trackFile.delete()
            }
            val instance = pythonInstance
            if (instance != null) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        GpxPyExecutor(instance).createSimplifiedGpxTrack(
                            summit.getGpsTrackPath(
                                simplified = false
                            )
                        )
                    }
                }
            }
            viewModel.saveSummit(true, summit)
            dialog?.cancel()
        }
        if (summit.garminData != null && summit.garminData?.activityId != null) {
            val splitsFile =
                File("${activitiesDir?.absolutePath}/activity_${summit.garminData?.activityId}_splits.json")
            if (splitsFile.exists()) {
                setSpeedDataFromSplitsJson(splitsFile, summit)
            } else if (pythonExecutor != null) {
                try {
                    summit.garminData?.activityId?.let {
                        val json = pythonExecutor?.downloadSpeedDataForActivity(it)
                        if (json != null) {
                            setVelocityData(json, summit)
                        }
                    }
                } catch (e: RuntimeException) {
                    Log.e("Download speed data for activity", e.message ?: "")
                }
            } else {
                Toast.makeText(
                    context,
                    getString(R.string.set_user_pwd), Toast.LENGTH_LONG
                ).show()
            }
        }
        val gpxPyJsonFile = summit.getGpxPyPath().toFile()
        if (gpxPyJsonFile.exists()) {
            extractGpxPyJson(gpxPyJsonFile, summit)
        }
        drawTable(binding.root)
    }

    private fun extractGpxPyJson(
        gpxPyJsonFile: File,
        summit: Summit
    ) {
        val gpxPyJson =
            JsonParser.parseString(JsonUtils.getJsonData(gpxPyJsonFile)) as JsonObject

        val elevationGain =
            gpxPyJson.getAsJsonPrimitive("elevation_gain").asDouble.roundToInt()
        if (elevationGain > 0) {
            tableEntries.add(
                TableEntry(
                    getString(R.string.height_meter_hint),
                    elevationGain.toDouble(),
                    "hm",
                    summit.elevationData.elevationGain == elevationGain,
                    { e ->
                        summit.elevationData.elevationGain = e.toInt()
                    },
                    summit.elevationData.elevationGain.toDouble()
                )
            )
        }

        val maxElevation =
            gpxPyJson.getAsJsonPrimitive("max_elevation").asDouble.roundToInt()
        if (maxElevation > 0) {
            tableEntries.add(
                TableEntry(
                    getString(R.string.top_elevation_hint),
                    maxElevation.toDouble(),
                    "hm",
                    summit.elevationData.maxElevation == maxElevation,
                    { e ->
                        summit.elevationData.maxElevation = e.toInt()
                    },
                    summit.elevationData.maxElevation.toDouble()
                )
            )
        }

        val distance =
            gpxPyJson.getAsJsonPrimitive("moving_distance").asDouble / 1000
        if (distance > 0) {
            tableEntries.add(
                TableEntry(
                    getString(R.string.kilometers_hint),
                    distance,
                    "km", abs(summit.kilometers - distance) < 0.05,
                    { e -> summit.kilometers = e },
                    summit.kilometers
                )
            )
        }

        val movingDuration =
            gpxPyJson.getAsJsonPrimitive("moving_time").asDouble
        if (movingDuration > 0) {
            val pace = distance / movingDuration * 3600
            tableEntries.add(
                TableEntry(
                    getString(R.string.pace_hint),
                    pace,
                    "km/h",
                    abs(summit.velocityData.avgVelocity - pace) < 0.05,
                    { e -> summit.velocityData.avgVelocity = e },
                    summit.velocityData.avgVelocity
                )
            )
        }

        val maxVelocity =
            gpxPyJson.getAsJsonPrimitive("max_speed").asDouble * 3.6
        if (maxVelocity > 0) {
            tableEntries.add(
                TableEntry(
                    getString(R.string.top_speed),
                    maxVelocity, "hm/h",
                    abs(summit.velocityData.maxVelocity - maxVelocity) < 0.05,
                    { e -> summit.velocityData.maxVelocity = e },
                    summit.velocityData.maxVelocity
                )
            )
        }
        if (gpxPyJson.has("slope_100")) {
            val value = gpxPyJson.getAsJsonPrimitive("slope_100").asDouble
            tableEntries.add(
                TableEntry(
                    getString(R.string.max_slope),
                    value, "%",
                    abs(summit.elevationData.maxSlope - value) < 0.05,
                    { e -> summit.elevationData.maxSlope = e },
                    0.0
                )
            )
        }
        if (gpxPyJson.has("vertical_velocities_60s")) {
            val value =
                gpxPyJson.getAsJsonPrimitive("vertical_velocities_60s").asDouble
            tableEntries.add(
                TableEntry(
                    getString(R.string.max_verticalVelocity_1Min),
                    value, "m",
                    summit.elevationData.maxVerticalVelocity1Min != 0.0 && abs(
                        summit.elevationData.maxVerticalVelocity1Min - value
                    ) * 60 < 0.05,
                    { e ->
                        summit.elevationData.maxVerticalVelocity1Min =
                            e
                    },
                    0.0, scaleFactor = 60
                )
            )
        }
        if (gpxPyJson.has("vertical_velocities_600s")) {
            val value =
                gpxPyJson.getAsJsonPrimitive("vertical_velocities_600s").asDouble
            tableEntries.add(
                TableEntry(
                    getString(R.string.max_verticalVelocity_10Min),
                    value, "m",
                    summit.elevationData.maxVerticalVelocity10Min != 0.0 && abs(
                        summit.elevationData.maxVerticalVelocity10Min - value
                    ) * 600 < 0.05,
                    { e ->
                        summit.elevationData.maxVerticalVelocity10Min =
                            e
                    },
                    0.0, scaleFactor = 600
                )
            )
        }
        if (gpxPyJson.has("vertical_velocities_3600s")) {
            val value =
                gpxPyJson.getAsJsonPrimitive("vertical_velocities_3600s").asDouble
            tableEntries.add(
                TableEntry(
                    getString(R.string.max_verticalVelocity_1h),
                    value, "m",
                    summit.elevationData.maxVerticalVelocity1h != 0.0 && abs(
                        summit.elevationData.maxVerticalVelocity1h - value
                    ) * 3600 < 0.05,
                    { e ->
                        summit.elevationData.maxVerticalVelocity1h = e
                    },
                    0.0, scaleFactor = 3600
                )
            )
        }
    }

    private fun setSpeedDataFromSplitsJson(
        splitsFile: File,
        summit: Summit
    ) {
        val json =
            JsonParser.parseString(JsonUtils.getJsonData(splitsFile)) as JsonObject
        val maxVelocitySummit = MaxVelocitySummit()
        val velocityEntries = maxVelocitySummit.parseFomGarmin(json)
        tableEntries.add(
            TableEntry(getString(R.string.top_speed_1km_hint),
                if (summit.velocityData.oneKilometer > 0.0) summit.velocityData.oneKilometer else maxVelocitySummit.getAverageVelocityForKilometers(
                    1.0,
                    velocityEntries
                ),
                "km/h",
                summit.velocityData.oneKilometer > 0.0,
                { e -> summit.velocityData.oneKilometer = e })
        )
        if (tableEntries.last().value > 0) {
            tableEntries.add(
                TableEntry(getString(R.string.top_speed_5km_hint),
                    if (summit.velocityData.fiveKilometer > 0.0) summit.velocityData.fiveKilometer else maxVelocitySummit.getAverageVelocityForKilometers(
                        5.0,
                        velocityEntries
                    ),
                    "km/h",
                    summit.velocityData.fiveKilometer > 0.0,
                    { e ->
                        summit.velocityData.fiveKilometer = e
                    })
            )
        }
        if (tableEntries.last().value > 0) {
            tableEntries.add(
                TableEntry(getString(R.string.top_speed_10km_hint),
                    if (summit.velocityData.tenKilometers > 0.0) summit.velocityData.tenKilometers else maxVelocitySummit.getAverageVelocityForKilometers(
                        10.0,
                        velocityEntries
                    ),
                    "km/h",
                    summit.velocityData.tenKilometers > 0.0,
                    { e ->
                        summit.velocityData.tenKilometers = e
                    })
            )
        }
        if (tableEntries.last().value > 0) {
            tableEntries.add(
                TableEntry(getString(R.string.top_speed_15km_hint),
                    if (summit.velocityData.fifteenKilometers > 0.0) summit.velocityData.fifteenKilometers else maxVelocitySummit.getAverageVelocityForKilometers(
                        15.0,
                        velocityEntries
                    ),
                    "km/h",
                    summit.velocityData.fifteenKilometers > 0.0,
                    { e ->
                        summit.velocityData.fifteenKilometers = e
                    })
            )
        }
        if (tableEntries.last().value > 0) {
            tableEntries.add(
                TableEntry(getString(R.string.top_speed_20km_hint),
                    if (summit.velocityData.twentyKilometers > 0.0) summit.velocityData.twentyKilometers else maxVelocitySummit.getAverageVelocityForKilometers(
                        20.0,
                        velocityEntries
                    ),
                    "km/h",
                    summit.velocityData.twentyKilometers > 0.0,
                    { e ->
                        summit.velocityData.twentyKilometers = e
                    })
            )
        }
        if (tableEntries.last().value > 0) {
            tableEntries.add(
                TableEntry(getString(R.string.top_speed_30km_hint),
                    if (summit.velocityData.thirtyKilometers > 0.0) summit.velocityData.thirtyKilometers else maxVelocitySummit.getAverageVelocityForKilometers(
                        30.0,
                        velocityEntries
                    ),
                    "km/h",
                    summit.velocityData.thirtyKilometers > 0.0,
                    { e ->
                        summit.velocityData.thirtyKilometers = e
                    })
            )
        }
        if (tableEntries.last().value > 0) {
            tableEntries.add(
                TableEntry(getString(R.string.top_speed_40km_hint),
                    if (summit.velocityData.fortyKilometers > 0.0) summit.velocityData.fortyKilometers else maxVelocitySummit.getAverageVelocityForKilometers(
                        40.0,
                        velocityEntries
                    ),
                    "km/h",
                    summit.velocityData.fortyKilometers > 0.0,
                    { e ->
                        summit.velocityData.fortyKilometers = e
                    })
            )
        }
        if (tableEntries.last().value > 0) {
            tableEntries.add(
                TableEntry(getString(R.string.top_speed_50km_hint),
                    if (summit.velocityData.fiftyKilometers > 0.0) summit.velocityData.fiftyKilometers else maxVelocitySummit.getAverageVelocityForKilometers(
                        50.0,
                        velocityEntries
                    ),
                    "km/h",
                    summit.velocityData.fiftyKilometers > 0.0,
                    { e ->
                        summit.velocityData.fiftyKilometers = e
                    })
            )
        }
        if (tableEntries.last().value > 0) {
            tableEntries.add(
                TableEntry(getString(R.string.top_speed_75km_hint),
                    if (summit.velocityData.seventyFiveKilometers > 0.0) summit.velocityData.seventyFiveKilometers else maxVelocitySummit.getAverageVelocityForKilometers(
                        75.0,
                        velocityEntries
                    ),
                    "km/h",
                    summit.velocityData.seventyFiveKilometers > 0.0,
                    { e ->
                        summit.velocityData.seventyFiveKilometers =
                            e
                    })
            )
        }
        if (tableEntries.last().value > 0) {
            tableEntries.add(
                TableEntry(getString(R.string.top_speed_100km_hint),
                    if (summit.velocityData.hundredKilometers > 0.0) summit.velocityData.hundredKilometers else maxVelocitySummit.getAverageVelocityForKilometers(
                        100.0,
                        velocityEntries
                    ),
                    "km/h",
                    summit.velocityData.hundredKilometers > 0.0,
                    { e ->
                        summit.velocityData.hundredKilometers = e
                    })
            )
        }
    }

    private fun setVelocityData(
        jsonLocal: JsonObject,
        summit: Summit
    ) {
        val maxVelocitySummit = MaxVelocitySummit()
        val velocityEntries = maxVelocitySummit.parseFomGarmin(jsonLocal)
        summit.velocityData.oneKilometer =
            maxVelocitySummit.getAverageVelocityForKilometers(1.0, velocityEntries)
        if (summit.velocityData.oneKilometer > 0) summit.velocityData.fiveKilometer =
            maxVelocitySummit.getAverageVelocityForKilometers(5.0, velocityEntries)
        if (summit.velocityData.fiveKilometer > 0) summit.velocityData.tenKilometers =
            maxVelocitySummit.getAverageVelocityForKilometers(10.0, velocityEntries)
        if (summit.velocityData.tenKilometers > 0) summit.velocityData.fifteenKilometers =
            maxVelocitySummit.getAverageVelocityForKilometers(15.0, velocityEntries)
        if (summit.velocityData.fifteenKilometers > 0) summit.velocityData.twentyKilometers =
            maxVelocitySummit.getAverageVelocityForKilometers(20.0, velocityEntries)
        if (summit.velocityData.twentyKilometers > 0) summit.velocityData.thirtyKilometers =
            maxVelocitySummit.getAverageVelocityForKilometers(30.0, velocityEntries)
        if (summit.velocityData.thirtyKilometers > 0) summit.velocityData.fortyKilometers =
            maxVelocitySummit.getAverageVelocityForKilometers(40.0, velocityEntries)
        if (summit.velocityData.fortyKilometers > 0) summit.velocityData.fiftyKilometers =
            maxVelocitySummit.getAverageVelocityForKilometers(50.0, velocityEntries)
        if (summit.velocityData.fiftyKilometers > 0) summit.velocityData.seventyFiveKilometers =
            maxVelocitySummit.getAverageVelocityForKilometers(75.0, velocityEntries)
        if (summit.velocityData.seventyFiveKilometers > 0) summit.velocityData.hundredKilometers =
            maxVelocitySummit.getAverageVelocityForKilometers(100.0, velocityEntries)
        viewModel.saveSummit(true, summit)
    }


    private fun drawTable(view: View) {
        addHeader(view, binding.tableSummits)
        tableEntries.forEachIndexed { index, entry ->
            if (entry.value > 0.0) {
                addSummitToTable(entry, view, index, binding.tableSummits)
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
            TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT
        )
        addLabel(view, tr, 200 + i, name, padding = 2)
        addLabel(
            view,
            tr,
            201 + i,
            String.format(
                requireContext().resources.configuration.locales[0],
                "%.1f",
                entry.value * entry.scaleFactor
            ),
            padding = 2,
            alignment = View.TEXT_ALIGNMENT_TEXT_END
        )
        val defaultValueAsString =
            if (abs(entry.defaultValue) < 0.05 || abs(entry.value - entry.defaultValue) < 0.05) "-" else String.format(
                requireContext().resources.configuration.locales[0],
                "%.1f",
                entry.defaultValue * entry.scaleFactor
            )
        addLabel(
            view,
            tr,
            202 + i,
            defaultValueAsString,
            padding = 2,
            alignment = View.TEXT_ALIGNMENT_TEXT_END
        )
        addLabel(
            view,
            tr,
            203 + i,
            entry.unit,
            padding = 2,
            alignment = View.TEXT_ALIGNMENT_TEXT_END
        )
        val box = CheckBox(view.context)
        box.isChecked = entry.isChecked
        box.setOnCheckedChangeListener { _, arg1 ->
            entry.isChecked = arg1
        }

        tr.addView(box)
        tl.addView(
            tr, TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun addHeader(view: View, tl: TableLayout) {
        val tableRowHead = TableRow(view.context)
        10.also { tableRowHead.id = it }
        tableRowHead.setBackgroundColor(Color.WHITE)
        tableRowHead.layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT
        )
        addLabel(view, tableRowHead, 20, getString(R.string.entry), Color.GRAY)
        addLabel(view, tableRowHead, 21, getString(R.string.update), Color.GRAY)
        addLabel(view, tableRowHead, 22, getString(R.string.current), Color.GRAY)
        addLabel(view, tableRowHead, 23, getString(R.string.unit), Color.GRAY)
        addLabel(view, tableRowHead, 24, "", Color.GRAY)

        tl.addView(
            tableRowHead, TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun addLabel(
        view: View, tr: TableRow, id: Int, text: String, color: Int = Color.WHITE,
        padding: Int = 5, alignment: Int = View.TEXT_ALIGNMENT_CENTER
    ) {
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

//        @SuppressLint("StaticFieldLeak")
//        class AsyncDownloadSpeedDataForActivity(
//            private val summit: Summit,
//            var database: AppDatabase
//        ) : AsyncTask<Void?, Void?, Void?>() {
//            private var json: JsonObject? = null
//            override fun doInBackground(vararg params: Void?): Void? {
//                try {
//                    summit.garminData?.activityId?.let {
//                        json = pythonExecutor?.downloadSpeedDataForActivity(it)
//                    }
//                } catch (e: java.lang.RuntimeException) {
//                    Log.e("AsyncDownloadActivities", e.message ?: "")
//                }
//                return null
//            }
//
//            override fun onPostExecute(param: Void?) {
//                val jsonLocal = json
//                if (jsonLocal != null) {
//                    setVelocityData(jsonLocal, summit, database)
//                }
//            }
//        }


    }

    class TableEntry(
        var name: String,
        var value: Double,
        var unit: String,
        var isChecked: Boolean,
        var f: (Double) -> Unit,
        var defaultValue: Double = 0.0,
        var scaleFactor: Int = 1
    ) {
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

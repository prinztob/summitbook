package de.drtobiasprinz.summitbook.ui.dialog

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
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
import de.drtobiasprinz.summitbook.models.AdditionalDataTableEntry
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonInstance
import de.drtobiasprinz.summitbook.ui.utils.JsonUtils
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
                summit.updated += 1
                viewModel.saveSummit(true, summit)
            }
            dialog?.cancel()
        }
        binding.recalculate.setOnClickListener {
            binding.loading.visibility = View.VISIBLE
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        Log.i(
                            "AsyncSimplifyGpsTracks",
                            "Simplifying track ${summit.getDateAsString()}_${summit.name}."
                        )
                        pythonInstance?.let { it1 ->
                            GpxPyExecutor(it1).createSimplifiedGpxTrackAndGpxPyDataFile(
                                summit.getGpsTrackPath(
                                    simplified = false
                                )
                            )
                        }
                    } catch (ex: RuntimeException) {
                        Log.e(
                            "AsyncSimplifyGpsTracks",
                            "Error in simplify track for ${summit.getDateAsString()}_${summit.name}: ${ex.message}"
                        )
                    }
                }
                binding.loading.visibility = View.GONE
                extractDataFromFilesAndPutIntoView(summit)
            }
        }
        binding.deleteData.setOnClickListener {
            summit.velocityData = VelocityData(
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
                    Log.i(
                        "AddAdditionalDataFromExternalResourcesDialog.asyncAnalyzeGpsTracks",
                        "Entry ${summit.name} will be simplified again in order to obtain newest data"
                    )
                    withContext(Dispatchers.IO) {
                        GpxPyExecutor(instance).createSimplifiedGpxTrackAndGpxPyDataFile(
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
        extractDataFromFilesAndPutIntoView(summit)
    }

    private fun extractDataFromFilesAndPutIntoView(summit: Summit) {
        tableEntries.clear()
        val gpxPyJsonFile = summit.getGpxPyPath().toFile()
        if (gpxPyJsonFile.exists()) {
            extractGpxPyJson(gpxPyJsonFile, summit)
        }
        binding.tableSummits.removeAllViews()
        drawTable(binding.root)
    }

    private fun extractGpxPyJson(
        gpxPyJsonFile: File,
        summit: Summit
    ) {
        val gpxPyJson =
            JsonParser.parseString(JsonUtils.getJsonData(gpxPyJsonFile)) as JsonObject

        AdditionalDataTableEntry.entries.filter { it.jsonKey != "" }.forEach {
            if (gpxPyJson.has(it.jsonKey)) {
                val value =
                    gpxPyJson.getAsJsonPrimitive(it.jsonKey).asDouble * it.scaleFactorForJson(
                        gpxPyJson
                    )
                if (value > 0) {
                    tableEntries.add(TableEntry(value, summit, it))
                }
            }
        }
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
        var name = ""
        val localSummit = summitEntry
        if (localSummit != null) {

            val splitName = getString(entry.tableEntry.nameId).split(" ")
            splitName.forEachIndexed { index, element ->
                name += if (index == splitName.size - 1) {
                    element
                } else if (index % 2 == 0 && splitName.size > 2) {
                    "$element "
                } else {
                    "$element \n"
                }
            }
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
                    if (entry.tableEntry.isInt) "%.0f" else "%.1f",
                    entry.value * entry.tableEntry.scaleFactorView
                ),
                padding = 2,
                alignment = View.TEXT_ALIGNMENT_TEXT_END
            )
            val currentValue = entry.tableEntry.getValue(localSummit)
            val defaultValueAsString =
                if (abs(currentValue) < 0.05 || abs(entry.value - currentValue) < (if (entry.tableEntry.isInt) 0.51 else 0.05)) {
                    "-"
                } else {
                    String.format(
                        requireContext().resources.configuration.locales[0],
                        if (entry.tableEntry.isInt) "%.0f" else "%.1f",
                        currentValue * entry.tableEntry.scaleFactorView
                    )
                }
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
                getString(entry.tableEntry.unitId),
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
    }

    class TableEntry(
        var value: Double,
        var summit: Summit,
        var tableEntry: AdditionalDataTableEntry,
    ) {
        var isSet: Boolean = true
        var isChecked: Boolean = if (tableEntry.isInt) {
            tableEntry.getValue(summit)
                .roundToInt() == (value).roundToInt()
        } else {
            abs(tableEntry.getValue(summit) - value) * tableEntry.scaleFactorView < 0.05
        }

        fun update() {
            if (isSet) {
                val valueToSet = if (isChecked) value else tableEntry.getValue(summit)
                tableEntry.setValue(summit, valueToSet)
            }
        }
    }

}

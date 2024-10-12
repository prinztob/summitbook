package de.drtobiasprinz.summitbook.ui.dialog

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.adapter.AddAdditionalDataAdapter
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
        binding.save.setOnClickListener {
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
            binding.loadingPanel.visibility = View.VISIBLE
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
                binding.loadingPanel.visibility = View.GONE
                extractDataFromFilesAndPutIntoView(summit)
            }
        }
        binding.ignore.setOnClickListener {
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

        val addAdditionalDataAdapter = AddAdditionalDataAdapter(summit)
        addAdditionalDataAdapter.differ.submitList(
            tableEntries
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = addAdditionalDataAdapter
        }

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
        var isChecked: Boolean = if (tableEntry.isInt) {
            tableEntry.getValue(summit)
                .roundToInt() == (value).roundToInt()
        } else {
            abs(tableEntry.getValue(summit) - value) * tableEntry.scaleFactorView < 0.05
        }

        fun update() {
            if (value > 0.0) {
                val valueToSet = if (isChecked) value else tableEntry.getValue(summit)
                tableEntry.setValue(summit, valueToSet)
            }
        }
    }

}

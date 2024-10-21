package de.drtobiasprinz.summitbook.ui.dialog

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.AddNewSummitsAdapter
import de.drtobiasprinz.summitbook.databinding.DialogShowNewSummitFromGarminBinding
import de.drtobiasprinz.summitbook.db.entities.IgnoredActivity
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.activitiesDir
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonExecutor
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class ShowNewSummitsFromGarminDialog : DialogFragment(), BaseDialog {

    private val viewModel: DatabaseViewModel by viewModels()

    private lateinit var binding: DialogShowNewSummitFromGarminBinding

    private lateinit var currentContext: Context
    private var entriesWithoutIgnored: MutableList<Summit> = mutableListOf()

    private var showAllButtonEnabled = false
    private var activitiesIdIgnored: List<String> = emptyList()
    private var ignoredActivities: List<IgnoredActivity> = emptyList()
    var save: (List<Summit>, Boolean) -> Unit = { _, _ -> }
    var summits: List<Summit> = emptyList()
    private lateinit var addNewSummitsAdapter: AddNewSummitsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogShowNewSummitFromGarminBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    @Override
    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentContext = requireContext()
        val sharedPreferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }

        viewModel.ignoredActivityList.observe(viewLifecycleOwner) {
            it.data.let { entries ->
                ignoredActivities = entries ?: emptyList()
                updateEntriesWithoutIgnored(summits)
                val width = Resources.getSystem().displayMetrics.widthPixels
                addNewSummitsAdapter = AddNewSummitsAdapter()
                addNewSummitsAdapter.differ.submitList(
                    entriesWithoutIgnored.sortedBy { summit -> summit.getDateAsString() }.reversed()
                )
                addNewSummitsAdapter.ignoredActivities = ignoredActivities
                addNewSummitsAdapter.updateButtons = {
                    binding.addSummitMerge.isEnabled = canSelectedSummitsBeMerged()
                    binding.save.isEnabled = areEntriesChecked()
                    binding.ignore.isEnabled = areEntriesChecked()
                }
                binding.recyclerView.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = addNewSummitsAdapter

                }
                binding.recyclerView.minimumWidth = (width * 0.97).toInt()
                binding.save.setOnClickListener {
                    if (pythonExecutor != null) {
                        val startDate =
                            sharedPreferences?.getString(Keys.PREF_THIRD_PARTY_START_DATE, null)
                                ?: ""
                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        val endDate = current.format(formatter)
                        val startDateForSync = (current.minusDays(1)).format(formatter)
                        binding.loadingPanel.visibility = View.VISIBLE
                        asyncDownloadActivities(
                            summits,
                            pythonExecutor,
                            startDate,
                            endDate,
                            startDateForSync
                        )
                    } else {
                        Toast.makeText(
                            context,
                            getString(R.string.set_user_pwd), Toast.LENGTH_LONG
                        ).show()
                    }
                }
                binding.showAll.setOnClickListener {
                    if (showAllButtonEnabled) {
                        binding.showAll.alpha = .5f
                    } else {
                        binding.showAll.alpha = 1f
                    }
                    showAllButtonEnabled = !showAllButtonEnabled
                    updateEntriesWithoutIgnored(
                        summits,
                        showAllButtonEnabled
                    )
                    addNewSummitsAdapter.differ.submitList(entriesWithoutIgnored)
                }
                binding.save.isEnabled = false
                binding.save.setOnClickListener {
                    if (areEntriesChecked()) {
                        save(
                            entriesWithoutIgnored.filter { summit -> summit.isSelected },
                            false
                        )
                        dialog?.dismiss()
                    }
                }
                binding.addSummitMerge.isEnabled = false
                binding.addSummitMerge.setOnClickListener {
                    if (canSelectedSummitsBeMerged()) {
                        save(
                            entriesWithoutIgnored.filter { summit -> summit.isSelected },
                            true
                        )
                        dialog?.dismiss()
                    }
                }
                binding.ignore.isEnabled = false
                binding.ignore.setOnClickListener {
                    if (areEntriesChecked()) {
                        entriesWithoutIgnored.filter { summit -> summit.isSelected }
                            .forEach { summit ->
                                entriesWithoutIgnored.remove(summit)
                                summit.garminData?.activityId?.let { ignoredEntry ->
                                    viewModel.saveIgnoredActivity(
                                        IgnoredActivity(
                                            ignoredEntry
                                        )
                                    )
                                }
                            }
                    }
                }
                binding.back.setOnClickListener {
                    dialog?.cancel()
                }
            }
        }
    }

    private fun asyncDownloadActivities(
        summits: List<Summit>?,
        pythonExecutor: GarminPythonExecutor?,
        startDate: String,
        endDate: String,
        startDateForSync: String
    ) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    activitiesDir?.let {
                        pythonExecutor?.downloadActivitiesByDate(
                            it, startDate, endDate
                        )
                    }
                } catch (e: RuntimeException) {
                    Log.e("AsyncDownloadActivities", e.message ?: "")
                }
            }

            if (activitiesDir?.exists() == true && activitiesDir?.isDirectory == true) {
                val files = activitiesDir?.listFiles()
                if (files?.isNotEmpty() == true) {
                    val edit =
                        PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .edit()
                    edit.putString(Keys.PREF_THIRD_PARTY_START_DATE, startDateForSync)
                    edit.apply()
                }
            }
            summits?.let { updateEntriesWithoutIgnored(it) }
            addNewSummitsAdapter.differ.submitList(entriesWithoutIgnored)
            binding.loadingPanel.visibility = View.GONE
        }
    }

    private fun getAllActivitiesFromThirdParty(
        activityIdsInSummitBook: List<String>,
        activitiesIdIgnored: List<String> = emptyList()
    ): MutableList<Summit> {
        return GarminPythonExecutor.getAllDownloadedSummitsFromGarmin(
            activitiesDir,
            activityIdsInSummitBook,
            activitiesIdIgnored
        )
    }

    private fun updateEntriesWithoutIgnored(summits: List<Summit>, showAll: Boolean = false) {
        activitiesIdIgnored = ignoredActivities.map {
            it.activityId
        }
        val activityIdsInSummitBook =
            summits.filter { !it.garminData?.activityIds.isNullOrEmpty() }
                .map { it.garminData?.activityIds as List<String> }.flatten()
        entriesWithoutIgnored = if (showAll) {
            getAllActivitiesFromThirdParty(activityIdsInSummitBook)
        } else {
            getAllActivitiesFromThirdParty(
                activityIdsInSummitBook,
                activitiesIdIgnored
            )
        }
        Log.i("ShowNewSummits", "showing ${entriesWithoutIgnored.size} entries")
    }

    private fun canSelectedSummitsBeMerged() =
        entriesWithoutIgnored.filter { summitEntry -> summitEntry.isSelected }
            .map { it.getDateAsString() }
            .toSet().size == 1 && entriesWithoutIgnored.filter { summit -> summit.isSelected }.size > 1

    private fun areEntriesChecked(): Boolean {
        return entriesWithoutIgnored.map { it.isSelected }.contains(true)
    }

    override fun getDialogContext(): Context {
        return currentContext
    }

    override fun getProgressBarForAsyncTask(): ProgressBar {
        return binding.progressBar
    }

    override fun isStepByStepDownload(): Boolean {
        return false
    }

    override fun doInPostExecute(index: Int, successfulDownloaded: Boolean) {
        //do nothing
    }
}

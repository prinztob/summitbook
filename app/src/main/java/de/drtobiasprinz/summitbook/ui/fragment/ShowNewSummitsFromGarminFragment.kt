package de.drtobiasprinz.summitbook.ui.fragment

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.AddNewSummitsAdapter
import de.drtobiasprinz.summitbook.databinding.FragmentShowNewSummitsFromGarminBinding
import de.drtobiasprinz.summitbook.db.entities.IgnoredActivity
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.activitiesDir
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonExecutor
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ShowNewSummitsFromGarminFragment : Fragment() {
    private val viewModel: DatabaseViewModel by viewModels()
    private lateinit var binding: FragmentShowNewSummitsFromGarminBinding
    private lateinit var currentContext: Context
    private var entriesWithoutIgnored: MutableList<Summit> = mutableListOf()
    private var showAllButtonEnabled = false
    private var activitiesIdIgnored: List<String> = emptyList()
    private var ignoredActivities: List<IgnoredActivity> = emptyList()
    var save: (List<Summit>, Boolean) -> Unit = { _, _ -> }
    var summits: List<Summit> = emptyList()
    var selectedDate: Date? = null
    private lateinit var addNewSummitsAdapter: AddNewSummitsAdapter
    private var startDate: Date = getDefaultStartDate()
    private var endDate: Date = getDefaultEndDate()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentShowNewSummitsFromGarminBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentContext = requireContext()
        val sharedPreferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }

        // Setup date pickers
        setupDatePickers()

        viewModel.ignoredActivityList.observe(viewLifecycleOwner) {
            it.data.let { entries ->
                ignoredActivities = entries ?: emptyList()
                updateEntriesWithoutIgnored(summits)
                updateEmptyState()
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
                            summits, pythonExecutor, startDate, endDate, startDateForSync
                        )
                    } else {
                        Toast.makeText(
                            context, getString(R.string.set_user_pwd), Toast.LENGTH_LONG
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
                        summits, showAllButtonEnabled
                    )
                    addNewSummitsAdapter.differ.submitList(entriesWithoutIgnored)
                    updateEmptyState()
                }
                binding.save.isEnabled = false
                binding.save.setOnClickListener {
                    if (areEntriesChecked()) {
                        save(
                            entriesWithoutIgnored.filter { summit -> summit.isSelected }, false
                        )
                        back(R.string.garmin_add_successful)
                    }
                }
                binding.addSummitMerge.isEnabled = false
                binding.addSummitMerge.setOnClickListener {
                    if (canSelectedSummitsBeMerged()) {
                        save(
                            entriesWithoutIgnored.filter { summit -> summit.isSelected }, true
                        )
                        back(R.string.garmin_add_successful)
                    }
                }
                binding.recalculate.setOnClickListener {
                    val activity = requireActivity()
                    if (activity is MainActivity) {
                        activity.updateThirdPartyData()
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
                    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                        putString(Keys.PREF_THIRD_PARTY_START_DATE, startDateForSync)
                    }
                }
            }
            summits?.let { updateEntriesWithoutIgnored(it) }
            addNewSummitsAdapter.differ.submitList(entriesWithoutIgnored)
            updateEmptyState()
            binding.loadingPanel.visibility = View.GONE
        }
    }

    private fun getAllActivitiesFromThirdParty(
        activityIdsInSummitBook: List<String>, activitiesIdIgnored: List<String> = emptyList()
    ): MutableList<Summit> {
        return GarminPythonExecutor.getAllDownloadedSummitsFromGarmin(
            activitiesDir, activityIdsInSummitBook, activitiesIdIgnored
        )
    }

    private fun updateEntriesWithoutIgnored(summits: List<Summit>, showAll: Boolean = false) {
        activitiesIdIgnored = ignoredActivities.map {
            it.activityId
        }
        val activityIdsInSummitBook = summits.filter { !it.garminData?.activityIds.isNullOrEmpty() }
            .map { it.garminData?.activityIds as List<String> }.flatten()
        val allEntries = if (showAll) {
            getAllActivitiesFromThirdParty(activityIdsInSummitBook)
        } else {
            getAllActivitiesFromThirdParty(
                activityIdsInSummitBook, activitiesIdIgnored
            )
        }

        // Apply date filtering
        entriesWithoutIgnored = allEntries.filter { summit ->
            summit.date.time >= startDate.time && summit.date.time <= endDate.time
        }.toMutableList()

        Log.i(
            "ShowNewSummits",
            "showing ${entriesWithoutIgnored.size} entries (filtered by date range)"
        )
    }

    private fun canSelectedSummitsBeMerged() =
        entriesWithoutIgnored.filter { summitEntry -> summitEntry.isSelected }
            .map { it.getDateAsString() }
            .toSet().size == 1 && entriesWithoutIgnored.filter { summit -> summit.isSelected }.size > 1

    private fun areEntriesChecked(): Boolean {
        return entriesWithoutIgnored.map { it.isSelected }.contains(true)
    }

    private fun back(messageId: Int) {
        val ft = parentFragmentManager.beginTransaction()
        ft.replace(R.id.content_frame, SummitViewFragment())
        ft.commit()
        Toast.makeText(
            activity, getString(
                messageId,
                entriesWithoutIgnored.firstOrNull { summit -> summit.isSelected }?.name
                    ?: "'new summit'"
            ), Toast.LENGTH_LONG
        ).show()
    }

    private fun setupDatePickers() {
        // If a selected date is provided, use it to set the date range
        selectedDate?.let { date ->
            // Set start date to the selected date
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            startDate = calendar.time

            // Set end date to the selected date
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            endDate = calendar.time
        }

        // Set initial date values
        binding.startDate.setText(dateFormat.format(startDate))
        binding.endDate.setText(dateFormat.format(endDate))

        binding.startDate.setOnClickListener {
            showDatePickerDialog(startDate) { selectedDate ->
                startDate = selectedDate
                binding.startDate.setText(dateFormat.format(startDate))
                updateEntriesWithoutIgnored(summits, showAllButtonEnabled)
                addNewSummitsAdapter.differ.submitList(entriesWithoutIgnored)
                updateEmptyState()
            }
        }

        binding.endDate.setOnClickListener {
            showDatePickerDialog(endDate) { selectedDate ->
                endDate = selectedDate
                binding.endDate.setText(dateFormat.format(endDate))
                updateEntriesWithoutIgnored(summits, showAllButtonEnabled)
                addNewSummitsAdapter.differ.submitList(entriesWithoutIgnored)
                updateEmptyState()
            }
        }
    }

    private fun showDatePickerDialog(currentDate: Date, onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.time = currentDate

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.CustomDatePickerDialogTheme,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)
                onDateSelected(selectedCalendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateEmptyState() {
        if (entriesWithoutIgnored.isEmpty()) {
            binding.emptyBody.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyBody.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    companion object {
        private fun getDefaultStartDate(): Date {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -1) // One month ago
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.time
        }

        private fun getDefaultEndDate(): Date {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            return calendar.time
        }
    }

}
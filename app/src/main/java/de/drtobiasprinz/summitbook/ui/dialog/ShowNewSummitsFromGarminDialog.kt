package de.drtobiasprinz.summitbook.ui.dialog

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
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
    private lateinit var backButton: ImageButton
    private lateinit var tableLayout: TableLayout
    private var activitiesIdIgnored: List<String> = emptyList()
    private var ignoredActivities: List<IgnoredActivity> = emptyList()
    var save: (List<Summit>, Boolean) -> Unit = { _, _ -> }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogShowNewSummitFromGarminBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentContext = requireContext()
        val sharedPreferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }

        viewModel.ignoredActivityList.observe(viewLifecycleOwner) {
            it.data.let { entries ->

                viewModel.summitsList.observe(viewLifecycleOwner) { summitListDataStatus ->
                    summitListDataStatus.data.let { summits ->
                        ignoredActivities = entries ?: emptyList()
                        summits?.let { summitsNotNull -> updateEntriesWithoutIgnored(summitsNotNull) }

                        binding.updateNewSummits.setOnClickListener {
                            if (pythonExecutor != null) {
                                val startDate =
                                    sharedPreferences?.getString("garmin_start_date", null) ?: ""
                                val current = LocalDateTime.now()
                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                val endDate = current.format(formatter)
                                binding.loadingPanel.visibility = View.VISIBLE
                                asyncDownloadActivities(summits, pythonExecutor, startDate, endDate)
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
                            summits?.let { summitsNotNull -> updateEntriesWithoutIgnored(summitsNotNull, showAllButtonEnabled) }
                            drawTable(view)
                        }
                        binding.updateData.isEnabled = false
                        binding.updateData.setOnClickListener {
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
                            drawTable(view)
                        }
                        backButton = view.findViewById(R.id.back)
                        backButton.setOnClickListener {
                            dialog?.cancel()
                        }

                        tableLayout = view.findViewById(R.id.tableSummits) as TableLayout
                        drawTable(view)
                    }
                }
            }
        }
    }

    private fun asyncDownloadActivities(
        summits: List<Summit>?,
        pythonExecutor: GarminPythonExecutor?,
        startDate: String,
        endDate: String
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
                    edit.putString("garmin_start_date", endDate)
                    edit.apply()
                }
            }
            summits?.let { updateEntriesWithoutIgnored(it) }
            view?.let { drawTable(it) }
            binding.loadingPanel.visibility = View.GONE

        }
    }

    private fun getAllActivitiesFromThirdParty(): MutableList<Summit> {
        return GarminPythonExecutor.getAllDownloadedSummitsFromGarmin(activitiesDir)
    }

    private fun updateEntriesWithoutIgnored(summits: List<Summit>, showAll: Boolean = false) {
        activitiesIdIgnored = ignoredActivities.map {
            it.activityId
        }
        val activityIdsInSummitBook =
            summits.filter { !it.garminData?.activityIds.isNullOrEmpty() }
                .map { it.garminData?.activityIds as List<String> }.flatten()
        entriesWithoutIgnored = if (showAll) {
            getAllActivitiesFromThirdParty().filter { it.garminData?.activityId !in activityIdsInSummitBook } as MutableList
        } else {
            getAllActivitiesFromThirdParty().filter { it.garminData?.activityId !in activitiesIdIgnored && it.garminData?.activityId !in activityIdsInSummitBook } as MutableList
        }
    }

    private fun drawTable(view: View) {
        tableLayout.removeAllViews()
        addHeader(view, tableLayout)
        for ((i, entry) in entriesWithoutIgnored.sortedBy { it.getDateAsString() }.reversed()
            .withIndex()) {
            addSummitToTable(entry, view, i, tableLayout)
        }
    }

    private fun addSummitToTable(entry: Summit, view: View, i: Int, tl: TableLayout) {
        val date =
            "<a href=\"${entry.garminData?.url ?: "unknown"}\">${entry.getDateAsString()}</a>"
        val name: String = entry.name.chunked(10).joinToString("\n")
        val kilometers: Double = entry.kilometers
        val heightMeters: Int = entry.elevationData.elevationGain

        val tr = TableRow(view.context)
        if (entry.garminData?.activityId in activitiesIdIgnored) {
            tr.setBackgroundColor(Color.LTGRAY)
        } else {
            tr.setBackgroundColor(Color.GRAY)
        }
        tr.id = 100 + i
        tr.layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT
        )
        addLabel(view, tr, 200 + i, date, padding = 2, isHtml = true)
        addLabel(view, tr, 200 + i, name, padding = 2)
        addLabel(
            view, tr, 200 + i, String.format(
                requireContext().resources.configuration.locales[0], "%.1f km", kilometers
            ), padding = 2, alignment = View.TEXT_ALIGNMENT_TEXT_END
        )
        addLabel(
            view, tr, 200 + i, String.format(
                requireContext().resources.configuration.locales[0], "%s m", heightMeters
            ), padding = 2, alignment = View.TEXT_ALIGNMENT_TEXT_END
        )
        val box = CheckBox(view.context)
        box.setOnCheckedChangeListener { _, arg1 ->
            entry.isSelected = arg1
            binding.addSummitMerge.isEnabled = canSelectedSummitsBeMerged()
            binding.updateData.isEnabled = areEntriesChecked()
            binding.ignore.isEnabled = areEntriesChecked()
        }

        tr.addView(box)
        tl.addView(
            tr, TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun canSelectedSummitsBeMerged() =
        entriesWithoutIgnored.filter { summitEntry -> summitEntry.isSelected }
            .map { it.getDateAsString() }
            .toSet().size == 1 && entriesWithoutIgnored.filter { summit -> summit.isSelected }.size > 1

    private fun addHeader(view: View, tl: TableLayout) {
        val tableRowHead = TableRow(view.context)
        10.also { tableRowHead.id = it }
        tableRowHead.setBackgroundColor(Color.WHITE)
        tableRowHead.layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT
        )
        addLabel(view, tableRowHead, 20, "Date", Color.GRAY)
        addLabel(view, tableRowHead, 21, "Summit\nName", Color.GRAY)
        addLabel(view, tableRowHead, 22, "km", Color.GRAY)
        addLabel(view, tableRowHead, 23, "hm", Color.GRAY)
        addLabel(view, tableRowHead, 24, "", Color.GRAY)

        tl.addView(
            tableRowHead, TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun addLabel(
        view: View,
        tr: TableRow,
        id: Int,
        text: String,
        color: Int = Color.WHITE,
        padding: Int = 5,
        alignment: Int = View.TEXT_ALIGNMENT_CENTER,
        isHtml: Boolean = false
    ) {
        val label = TextView(view.context)
        label.id = id
        if (isHtml) {
            label.setLinkTextColor(Color.WHITE)
            label.isClickable = true
            label.movementMethod = LinkMovementMethod.getInstance()
            label.text = Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
        } else {
            label.text = text
        }
        label.gravity = alignment
        label.setTextColor(color)
        label.setPadding(padding, padding, padding, padding)
        tr.addView(label)
    }

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

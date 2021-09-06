package de.drtobiasprinz.summitbook.ui.dialog

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment.Companion.activitiesDir
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment.Companion.updateNewSummits
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class ShowNewSummitsFromGarminDialog(private val allEntries: MutableList<SummitEntry>, val sortFilterHelper: SortFilterHelper, private val pythonExecutor: GarminPythonExecutor?, val progressBar: ProgressBar? = null) : DialogFragment(), BaseDialog {

    private lateinit var addSummitsButton: Button
    private lateinit var currentContext: Context
    private lateinit var entriesWithoutIgnored: MutableList<SummitEntry>
    private lateinit var mergeSummitsButton: Button
    private lateinit var ignoreSummitsButton: Button
    private lateinit var updateSummitsButton: ImageButton
    private lateinit var revertIgnoredSummitsButton: ImageButton
    private lateinit var backButton: Button
    private lateinit var tableLayout: TableLayout
    private lateinit var helper: SummitBookDatabaseHelper
    private lateinit var database: SQLiteDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_show_new_summit_from_garmin, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentContext = requireContext()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val useTcx = sharedPreferences.getBoolean("download_tcx", false)
        helper = SummitBookDatabaseHelper(view.context)
        database = helper.writableDatabase
        updateEntriesWithoutIgnored()

        updateSummitsButton = view.findViewById(R.id.update_new_summits)
        updateSummitsButton.setOnClickListener {
            val startDate = sharedPreferences.getString("garmin_start_date", null) ?: ""
            if (pythonExecutor != null) {
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val endDate = current.format(formatter)
                view.findViewById<RelativeLayout>(R.id.loadingPanel).visibility = View.VISIBLE
                AsyncDownloadActivities(sortFilterHelper.entries, pythonExecutor, startDate, endDate, this).execute()
            }
        }
        revertIgnoredSummitsButton = view.findViewById(R.id.revert_ignored_summits)
        revertIgnoredSummitsButton.setOnClickListener {
            helper.dropIgnoredActivities(database)
            updateEntriesWithoutIgnored()
            tableLayout.removeAllViews()
            drawTable(view)
        }
        addSummitsButton = view.findViewById(R.id.update_data)
        addSummitsButton.isEnabled = false
        addSummitsButton.setOnClickListener {
            if (pythonExecutor != null && areEntriesChecked()) {
                progressBar?.visibility = View.VISIBLE
                progressBar?.tooltipText = "Add summits ${entriesWithoutIgnored.filter { summit -> summit.isSelected }.map { it.name }.joinToString(", ")} and download its tracks. This may take a while."
                entriesWithoutIgnored.filter { summit -> summit.isSelected }.forEach { entry ->
                    GarminPythonExecutor.Companion.AsyncDownloadGpxViaPython(pythonExecutor, listOf(entry), sortFilterHelper, useTcx, this).execute()
                }
            }
            dialog?.cancel()
        }
        mergeSummitsButton = view.findViewById(R.id.add_summit_merge)
        mergeSummitsButton.isEnabled = false
        mergeSummitsButton.setOnClickListener {
            if (pythonExecutor != null && canSelectedSummitsBeMerged()) {
                progressBar?.visibility = View.VISIBLE
                progressBar?.tooltipText = "Merge summits ${entriesWithoutIgnored.filter { summit -> summit.isSelected }.map { it.name }.joinToString(", ")} and downloaded tracks. This may take a while."
                GarminPythonExecutor.Companion.AsyncDownloadGpxViaPython(pythonExecutor, entriesWithoutIgnored.filter { summit -> summit.isSelected }, sortFilterHelper, useTcx, this).execute()
            }
            dialog?.cancel()
        }
        ignoreSummitsButton = view.findViewById(R.id.ignore)
        ignoreSummitsButton.isEnabled = false
        ignoreSummitsButton.setOnClickListener {
            if (areEntriesChecked()) {
                entriesWithoutIgnored.filter { summit -> summit.isSelected }.forEach {
                    entriesWithoutIgnored.remove(it)
                    it.activityData?.activityId?.let { ignoredEntry -> helper.insertIgnoredActivityId(database, ignoredEntry) }
                }
            }
            tableLayout.removeAllViews()
            drawTable(view)
        }
        backButton = view.findViewById(R.id.back)
        backButton.setOnClickListener {
            dialog?.cancel()
        }

        tableLayout = view.findViewById(R.id.tableSummits) as TableLayout
        drawTable(view)

    }

    private fun updateEntriesWithoutIgnored() {
        val activitiesIdIgnored = helper.getAllIgnoredActivities(sortFilterHelper.database)
        val activityIdsInSummitBook = sortFilterHelper.entries.filter { !it.activityData?.activityIds.isNullOrEmpty() }.map { it.activityData?.activityIds as List<String> }.flatten()
        entriesWithoutIgnored = allEntries.filter { !(it.activityData?.activityId in activitiesIdIgnored) && !(it.activityData?.activityId in activityIdsInSummitBook) } as MutableList
    }

    private fun drawTable(view: View) {
        addHeader(view, tableLayout)
        for ((i, entry) in entriesWithoutIgnored.sortedBy { it.getDateAsString() }.reversed().withIndex()) {
            addSummitToTable(entry, view, i, tableLayout)
        }
    }

    private fun addSummitToTable(entry: SummitEntry, view: View, i: Int, tl: TableLayout) {
        val date = "<a href=\"${entry.activityData?.url ?: "unknown"}\">${entry.getDateAsString()}</a>"
        val name: String = entry.name.chunked(10).joinToString("\n")
        val kilometers: Double = entry.kilometers
        val heightMeters: Int = entry.elevationData.elevationGain

        val tr = TableRow(view.context)
        tr.setBackgroundColor(Color.GRAY)
        tr.id = 100 + i
        tr.layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
        addLabel(view, tr, 200 + i, date, padding = 2, isHtml = true)
        addLabel(view, tr, 200 + i, name, padding = 2)
        addLabel(view, tr, 200 + i, String.format(Locale.ENGLISH, "%.1f km", kilometers), padding = 2, aligment = View.TEXT_ALIGNMENT_TEXT_END)
        addLabel(view, tr, 200 + i, String.format(Locale.ENGLISH, "%s m", heightMeters), padding = 2, aligment = View.TEXT_ALIGNMENT_TEXT_END)
        val box = CheckBox(view.context)
        box.setOnCheckedChangeListener { _, arg1 ->
            entry.isSelected = arg1
            mergeSummitsButton.isEnabled = canSelectedSummitsBeMerged()
            addSummitsButton.isEnabled = areEntriesChecked()
            ignoreSummitsButton.isEnabled = areEntriesChecked()
        }

        tr.addView(box)
        tl.addView(tr, TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun canSelectedSummitsBeMerged() =
            entriesWithoutIgnored.filter { summitEntry -> summitEntry.isSelected }.map { it.getDateAsString() }.toSet().size == 1 && entriesWithoutIgnored.filter { summit -> summit.isSelected }.size > 1

    private fun addHeader(view: View, tl: TableLayout) {
        val tableRowHead = TableRow(view.context)
        10.also { tableRowHead.id = it }
        tableRowHead.setBackgroundColor(Color.WHITE)
        tableRowHead.layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
        addLabel(view, tableRowHead, 20, "Date", Color.GRAY)
        addLabel(view, tableRowHead, 21, "Summit\nName", Color.GRAY)
        addLabel(view, tableRowHead, 22, "km", Color.GRAY)
        addLabel(view, tableRowHead, 23, "hm", Color.GRAY)
        addLabel(view, tableRowHead, 24, "", Color.GRAY)

        tl.addView(tableRowHead, TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun addLabel(view: View, tr: TableRow, id: Int, text: String, color: Int = Color.WHITE, padding: Int = 5, aligment: Int = View.TEXT_ALIGNMENT_CENTER, isHtml: Boolean = false) {
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
        label.gravity = aligment
        label.setTextColor(color)
        label.setPadding(padding, padding, padding, padding)
        tr.addView(label)
    }

    private fun areEntriesChecked(): Boolean {
        return entriesWithoutIgnored.map { it.isSelected }.contains(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        database.close()
        helper.close()
    }

    companion object {

        class AsyncDownloadActivities(private val summits: List<SummitEntry>, private val pythonExecutor: GarminPythonExecutor, private val startDate: String, private val endDate: String, val dialog: ShowNewSummitsFromGarminDialog) : AsyncTask<Void?, Void?, Void?>() {

            override fun doInBackground(vararg params: Void?): Void? {
                try {
                    dialog.view?.findViewById<RelativeLayout>(R.id.loadingPanel)?.visibility = View.VISIBLE
                    pythonExecutor.downloadActivitiesByDate(activitiesDir, startDate, endDate)
                } catch (e: java.lang.RuntimeException) {
                    Log.e("AsyncDownloadActivities", e.message ?: "")
                    dialog.view?.findViewById<RelativeLayout>(R.id.loadingPanel)?.visibility = View.GONE
                }
                return null
            }

            override fun onPostExecute(param: Void?) {
                updateNewSummits(activitiesDir, summits, dialog.requireContext())
                if (activitiesDir.exists() && activitiesDir.isDirectory) {
                    val files = activitiesDir.listFiles()
                    if (files?.isNotEmpty() == true) {
                        val edit = PreferenceManager.getDefaultSharedPreferences(dialog.requireContext()).edit()
                        edit.putString("garmin_start_date", endDate)
                        edit.apply()
                    }
                }
                dialog.updateEntriesWithoutIgnored()
                dialog.tableLayout.removeAllViews()
                dialog.view?.let { dialog.drawTable(it) }
                dialog.view?.findViewById<RelativeLayout>(R.id.loadingPanel)?.visibility = View.GONE

            }
        }


    }

    override fun getDialogContext(): Context {
        return currentContext
    }

    override fun getProgressBarForAsyncTask(): ProgressBar? {
        return progressBar
    }

    override fun isStepByStepDownload(): Boolean {
        return false
    }

    override fun doInPostExecute(index: Int, successfulDownloaded: Boolean) {
        //do nothing
    }
}

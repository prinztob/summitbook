package de.drtobiasprinz.summitbook.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.database.sqlite.SQLiteDatabase
import android.os.AsyncTask
import android.os.Bundle
import android.view.*
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.SummitViewAdapter
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.SwipeToDeleteCallback
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog
import de.drtobiasprinz.summitbook.ui.dialog.ShowNewSummitsFromGarminDialog
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import java.io.File
import java.util.*


class SummitViewFragment(private val sortFilterHelper: SortFilterHelper) : Fragment(), SummationFragment, OnSharedPreferenceChangeListener {
    private lateinit var summitEntries: ArrayList<SummitEntry>
    private lateinit var myContext: FragmentActivity
    private var filteredEntries: ArrayList<SummitEntry>? = null
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        summitRecycler = inflater.inflate(
                R.layout.fragment_summit_view, container, false) as RecyclerView
        setHasOptionsMenu(true)
        sortFilterHelper.setFragment(this)
        summitEntries = sortFilterHelper.entries
        adapter = SummitViewAdapter(sortFilterHelper)
        filteredEntries = sortFilterHelper.filteredEntries
        update(filteredEntries)
        summitRecycler.adapter = adapter
        val layoutManager = LinearLayoutManager(activity)
        summitRecycler.layoutManager = layoutManager
        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(adapter, requireContext(), summitRecycler))
        itemTouchHelper.attachToRecyclerView(summitRecycler)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        readSharedPreference(sharedPreferences)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        return summitRecycler
    }

    override fun onAttach(activity: Activity) {
        myContext = activity as FragmentActivity
        super.onAttach(activity)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        AsyncSummitTask(this).execute()
        optionMenu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_search) {
            return true
        }
        if (id == R.id.action_show_new_summits) {
            ShowNewSummitsFromGarminDialog(allEntriesFromGarmin, sortFilterHelper).show(myContext.supportFragmentManager, "Show new summits from Garmin")
        }
        if (id == R.id.action_sort) {
            sortFilterHelper.setFragment(this)
            adapter.sort(sortFilterHelper)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun getAdapter(): SummitViewAdapter {
        return adapter
    }

    override fun update(filteredSummitEntries: ArrayList<SummitEntry>?) {
        adapter.setFilteredSummitEntries(filteredSummitEntries)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sortFilterHelper.areSharedPrefInitialized = false
        if (key == "current_year_switch") {
            readSharedPreference(sharedPreferences)
        }
    }

    private fun readSharedPreference(sharedPreferences: SharedPreferences?) {
        if (!sortFilterHelper.areSharedPrefInitialized) {
            sortFilterHelper.areSharedPrefInitialized = true
            if (sharedPreferences?.getBoolean("current_year_switch", false) == true) {
                sortFilterHelper.setSelectedDateItemDefault(2)
            } else {
                sortFilterHelper.setSelectedDateItemDefault(0)
            }
            sortFilterHelper.setDataSpinnerToDefault()
            sortFilterHelper.apply()
        }
    }


    companion object {
        lateinit var summitRecycler: RecyclerView
        private var optionMenu: Menu? = null
        private val allEntriesFromGarmin = mutableListOf<SummitEntry>()

        val activitiesDir = File(MainActivity.storage, "activities")

        @SuppressLint("StaticFieldLeak")
        lateinit var adapter: SummitViewAdapter

        fun updateNewSummits(activitiesDir: File, summits: List<SummitEntry>, context: Context?) {
            if (activitiesDir.exists() && activitiesDir.isDirectory) {
                val files = activitiesDir.listFiles()
                if (files.isNotEmpty()) {
                    allEntriesFromGarmin.clear()
                    val activitiesIdOfAllSummits = summits.filter { it.activityData != null && it.activityData?.activityIds?.isNotEmpty() == true }.map { it.activityData?.activityIds as List<String> }.flatten().toMutableList()
                    if (files != null && files.isNotEmpty()) {
                        files.forEach {
                            if (it.name.startsWith("activity_")) {
                                val gson = JsonParser().parse(it.readText()) as JsonObject
                                allEntriesFromGarmin.add(AddSummitDialog.parseJsonObject(gson))

                            }
                        }
                    }
                    val newEntriesFromGarmin = allEntriesFromGarmin.filter { !(it.activityData?.activityId in activitiesIdOfAllSummits) }
                    when (newEntriesFromGarmin.size) {
                        0 -> optionMenu?.getItem(1)?.icon = context?.let { ResourcesCompat.getDrawable(it.resources, R.drawable.ic_baseline_filter_none_24, null) }
                        1 -> optionMenu?.getItem(1)?.icon = context?.let { ResourcesCompat.getDrawable(it.resources, R.drawable.ic_baseline_filter_1_24, null) }
                        2 -> optionMenu?.getItem(1)?.icon = context?.let { ResourcesCompat.getDrawable(it.resources, R.drawable.ic_baseline_filter_2_24, null) }
                        3 -> optionMenu?.getItem(1)?.icon = context?.let { ResourcesCompat.getDrawable(it.resources, R.drawable.ic_baseline_filter_3_24, null) }
                        4 -> optionMenu?.getItem(1)?.icon = context?.let { ResourcesCompat.getDrawable(it.resources, R.drawable.ic_baseline_filter_4_24, null) }
                        5 -> optionMenu?.getItem(1)?.icon = context?.let { ResourcesCompat.getDrawable(it.resources, R.drawable.ic_baseline_filter_5_24, null) }
                        6 -> optionMenu?.getItem(1)?.icon = context?.let { ResourcesCompat.getDrawable(it.resources, R.drawable.ic_baseline_filter_6_24, null) }
                        7 -> optionMenu?.getItem(1)?.icon = context?.let { ResourcesCompat.getDrawable(it.resources, R.drawable.ic_baseline_filter_7_24, null) }
                        8 -> optionMenu?.getItem(1)?.icon = context?.let { ResourcesCompat.getDrawable(it.resources, R.drawable.ic_baseline_filter_8_24, null) }
                        9 -> optionMenu?.getItem(1)?.icon = context?.let { ResourcesCompat.getDrawable(it.resources, R.drawable.ic_baseline_filter_9_24, null) }
                        else -> optionMenu?.getItem(1)?.icon = context?.let { ResourcesCompat.getDrawable(it.resources, R.drawable.ic_baseline_filter_9_plus_24, null) }
                    }

                }
            }
        }

    }

    @SuppressLint("StaticFieldLeak")
    inner class AsyncSummitTask(val fragement: SummitViewFragment) : AsyncTask<Void?, Void?, Void?>() {
        var allEntries = arrayListOf<SummitEntry>()
        val helper: SummitBookDatabaseHelper = SummitBookDatabaseHelper(context)
        val database: SQLiteDatabase = helper.readableDatabase

        override fun doInBackground(vararg params: Void?): Void? {
            allEntries = helper.getAllSummits(database)
            return null
        }

        override fun onPostExecute(param: Void?) {
            database.close()
            sortFilterHelper.update(allEntries)
            sortFilterHelper.prepare()
            sortFilterHelper.setAllToDefault()
            adapter.notifyDataSetChanged()
            updateNewSummits(activitiesDir, summitEntries, fragement.context)
        }
    }

}
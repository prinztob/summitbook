package de.drtobiasprinz.summitbook.fragments

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.database.sqlite.SQLiteDatabase
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.SummitViewAdapter
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.SwipeToDeleteCallback
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import java.util.*


class SummitViewFragment(private val sortFilterHelper: SortFilterHelper) : Fragment(), SummationFragment, OnSharedPreferenceChangeListener {
    private lateinit var summitEntries: ArrayList<SummitEntry>
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
        AsyncSummitTask().execute()
        return summitRecycler
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_search) {
            return true
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

        @SuppressLint("StaticFieldLeak")
        lateinit var adapter: SummitViewAdapter
    }

    @SuppressLint("StaticFieldLeak")
    inner class AsyncSummitTask : AsyncTask<Void?, Void?, Void?>() {
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
        }
    }

}
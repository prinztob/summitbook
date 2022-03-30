package de.drtobiasprinz.summitbook.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.SummitViewAdapter
import de.drtobiasprinz.summitbook.models.FragmentResultReceiver
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor.Companion.getAllDownloadedSummitsFromGarmin
import de.drtobiasprinz.summitbook.ui.SwipeToMarkCallback
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import java.io.File


class SummitViewFragment() : Fragment(), SummationFragment, OnSharedPreferenceChangeListener {
    private lateinit var summitEntries: List<Summit>
    private lateinit var myContext: FragmentActivity
    private var filteredEntries: List<Summit>? = null
    private lateinit var resultreceiver: FragmentResultReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultreceiver = context as FragmentResultReceiver
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        summitRecycler = inflater.inflate(
                R.layout.fragment_summit_view, container, false) as RecyclerView
        setHasOptionsMenu(true)
        resultreceiver.getSortFilterHelper().fragment = this
        summitEntries = resultreceiver.getSortFilterHelper().entries
        adapter = SummitViewAdapter(resultreceiver.getSortFilterHelper(), resultreceiver.getPythonExecutor())
        filteredEntries = resultreceiver.getSortFilterHelper().filteredEntries
        update(filteredEntries)
        summitRecycler.adapter = adapter
        val layoutManager = LinearLayoutManager(activity)
        summitRecycler.layoutManager = layoutManager
        val itemTouchHelper = ItemTouchHelper(SwipeToMarkCallback(adapter, requireContext()))
        itemTouchHelper.attachToRecyclerView(summitRecycler)
        resultreceiver.getSharedPreference().registerOnSharedPreferenceChangeListener(this)
        resultreceiver.getSortFilterHelper().apply()
        return summitRecycler
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        myContext = activity as FragmentActivity
    }

    fun getAdapter(): SummitViewAdapter {
        return adapter
    }

    override fun update(filteredSummitEntries: List<Summit>?) {
        adapter.setFilteredSummitEntries(filteredSummitEntries)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        resultreceiver.getSortFilterHelper().areSharedPrefInitialized = false
        if (key == "current_year_switch" || key == "indoor_height_meter_per_cent") {
            readSharedPreference(sharedPreferences)
        }
    }

    private fun readSharedPreference(sharedPreferences: SharedPreferences?) {
        if (!resultreceiver.getSortFilterHelper().areSharedPrefInitialized) {
            resultreceiver.getSortFilterHelper().areSharedPrefInitialized = true
            if (sharedPreferences?.getBoolean("current_year_switch", false) == true) {
                resultreceiver.getSortFilterHelper().setSelectedDateItemDefault(2)
            } else {
                resultreceiver.getSortFilterHelper().setSelectedDateItemDefault(0)
            }
            resultreceiver.getSortFilterHelper().setIndoorHeightMeterPercent(sharedPreferences?.getInt("indoor_height_meter_per_cent", 0) ?: 0)
            resultreceiver.getSortFilterHelper().setDataSpinnerToDefault()
            resultreceiver.getSortFilterHelper().apply()
        }
    }


    companion object {
        lateinit var summitRecycler: RecyclerView
        private var optionMenu: Menu? = null

        @SuppressLint("StaticFieldLeak")
        lateinit var adapter: SummitViewAdapter

        fun updateNewSummits(allEntriesFromGarmin: List<Summit>, summits: List<Summit>, context: Context?) {
            if (allEntriesFromGarmin.isNotEmpty()) {
                val activitiesIdOfAllSummits = summits.filter { it.garminData != null && it.garminData?.activityIds?.isNotEmpty() == true }.map { it.garminData?.activityIds as List<String> }.flatten().toMutableList()
                val newEntriesFromGarmin = allEntriesFromGarmin.filter { !(it.garminData?.activityId in activitiesIdOfAllSummits) }
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
package de.drtobiasprinz.summitbook.db.entities

import android.content.Context
import android.content.SharedPreferences
import android.widget.ProgressBar
import de.drtobiasprinz.summitbook.adapter.ContactsAdapter
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper

interface FragmentResultReceiver {

    fun getContext(): Context

    fun getSortFilterHelper(): SortFilterHelper

    fun getSharedPreference(): SharedPreferences

    fun getPythonExecutor(): GarminPythonExecutor?

    fun getAllActivitiesFromThirdParty(): MutableList<Summit>

    fun getProgressBar(): ProgressBar?

    fun getSummitViewAdapter(): ContactsAdapter?

}
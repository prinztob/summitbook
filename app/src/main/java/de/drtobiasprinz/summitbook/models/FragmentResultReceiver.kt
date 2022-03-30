package de.drtobiasprinz.summitbook.models

import android.content.SharedPreferences
import android.widget.ProgressBar
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper

interface FragmentResultReceiver {

    fun getSortFilterHelper(): SortFilterHelper

    fun getSharedPreference(): SharedPreferences

    fun getPythonExecutor(): GarminPythonExecutor?

    fun getAllActivitiesFromThirdParty(): MutableList<Summit>

    fun getProgressBar(): ProgressBar?

}
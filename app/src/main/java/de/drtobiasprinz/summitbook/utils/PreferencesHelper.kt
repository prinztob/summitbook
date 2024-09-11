package de.drtobiasprinz.summitbook.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import de.drtobiasprinz.summitbook.Keys


/*
 * PreferencesHelper object
 */
object PreferencesHelper {

    /* The sharedPreferences object to be initialized */
    private lateinit var sharedPreferences: SharedPreferences


    /* Initialize a single sharedPreferences object when the app is launched */
    fun Context.initPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    /* Load the On Device Maps preference */
    fun loadOnDeviceMaps(): Boolean {
        return sharedPreferences.getBoolean(Keys.PREF_ON_DEVICE_MAPS, false)
    }


    /* Load the folder for the On Device Maps */
    fun loadOnDeviceMapsFolder(): String {
        return sharedPreferences.getString(Keys.PREF_ON_DEVICE_MAPS_FOLDER, String()) ?: String()
    }


    /* Save the folder for the On Device Maps */
    fun saveOnDeviceMapsFolder(onDeviceMapsFolder: String) {
        sharedPreferences.edit { putString(Keys.PREF_ON_DEVICE_MAPS_FOLDER, onDeviceMapsFolder) }
    }


}

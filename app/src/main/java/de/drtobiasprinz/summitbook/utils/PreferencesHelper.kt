package de.drtobiasprinz.summitbook.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import de.drtobiasprinz.summitbook.Keys

object PreferencesHelper {

    private lateinit var sharedPreferences: SharedPreferences


    fun Context.initPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    fun loadOnDeviceMaps(): Boolean {
        return sharedPreferences.getBoolean(Keys.PREF_ON_DEVICE_MAPS, false)
    }


    fun loadOnDeviceMapsFolder(): String {
        return sharedPreferences.getString(Keys.PREF_ON_DEVICE_MAPS_FOLDER, String()) ?: String()
    }


    fun saveOnDeviceMapsFolder(onDeviceMapsFolder: String) {
        sharedPreferences.edit { putString(Keys.PREF_ON_DEVICE_MAPS_FOLDER, onDeviceMapsFolder) }
    }

}

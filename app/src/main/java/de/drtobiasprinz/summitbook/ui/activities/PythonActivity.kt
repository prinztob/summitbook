package de.drtobiasprinz.summitbook.ui.activities

import android.app.Application
import de.drtobiasprinz.summitbook.ui.activities.MainActivityCompose
import de.drtobiasprinz.summitbook.data.appstate.AppState.storage

import de.drtobiasprinz.summitbook.ui.activities.PythonConsoleActivity
import de.drtobiasprinz.summitbook.core.Keys
import de.drtobiasprinz.summitbook.data.appstate.AppState

class PythonActivity : PythonConsoleActivity() {
    override fun getTaskClass(): Class<out Task?> {
        return Task::class.java
    }


    class Task(app: Application?) : PythonConsoleActivity.Task(app) {
        override fun run() {
            val sharedPreferences = AppState.sharedPreferences
            val username = sharedPreferences.getString(Keys.PREF_GARMIN_USERNAME, "")
            val password = sharedPreferences.getString(Keys.PREF_GARMIN_PASSWORD, "")
            py.getModule("entry_point")
                .callAttr("init_api", username, password, storage?.absolutePath)
        }
    }

}
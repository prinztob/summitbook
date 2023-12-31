package de.drtobiasprinz.summitbook

import android.app.Application
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.storage

import de.drtobiasprinz.summitbook.utils.PythonConsoleActivity

class PythonActivity : PythonConsoleActivity() {
    override fun getTaskClass(): Class<out Task?> {
        return Task::class.java
    }


    class Task(app: Application?) : PythonConsoleActivity.Task(app) {
        override fun run() {
            val sharedPreferences = MainActivity.sharedPreferences
            val username = sharedPreferences.getString("garmin_username", "")
            val password = sharedPreferences.getString("garmin_password", "")
            py.getModule("entry_point")
                .callAttr("init_api", username, password, storage?.absolutePath)
        }
    }

}
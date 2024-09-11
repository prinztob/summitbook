package de.drtobiasprinz.summitbook

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import de.drtobiasprinz.summitbook.utils.PreferencesHelper.initPreferences

@HiltAndroidApp
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initPreferences()
    }
}
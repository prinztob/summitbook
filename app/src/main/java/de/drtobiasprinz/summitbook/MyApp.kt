package de.drtobiasprinz.summitbook

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import de.drtobiasprinz.summitbook.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.utils.PreferencesHelper.initPreferences
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {

    @Inject
    lateinit var repository: DatabaseRepository

    override fun onCreate() {
        super.onCreate()
        initPreferences()
    }
}
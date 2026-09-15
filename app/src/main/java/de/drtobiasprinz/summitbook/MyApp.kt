package de.drtobiasprinz.summitbook

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import de.drtobiasprinz.summitbook.data.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.core.preferences.PreferencesHelper.initPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var repository: DatabaseRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        initPreferences()
        // Pre-warm database on background thread to prevent main thread blocking
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Trigger database initialization once; do not keep collecting
                // (an eternal collect would re-query the whole table on every write)
                repository.getAllSummits().first()
            } catch (_: Exception) {
                // Ignore errors during pre-warming
            }
        }
    }
}

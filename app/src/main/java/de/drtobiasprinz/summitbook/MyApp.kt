package de.drtobiasprinz.summitbook

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp
import de.drtobiasprinz.summitbook.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.utils.PreferencesHelper.initPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {

    @Inject
    lateinit var repository: DatabaseRepository

    override fun onCreate() {
        // Enable StrictMode for debug builds to detect main thread violations
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        super.onCreate()
        // Initialize preferences - use temp permit to allow disk read during startup
        // SharedPreferences is designed for main thread access and is generally fast
        val oldPolicy = StrictMode.allowThreadDiskReads()
        try {
            initPreferences()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
        // Pre-warm database on background thread to prevent main thread blocking
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Trigger database initialization by accessing a simple query
                repository.getAllSummits().collect { }
            } catch (_: Exception) {
                // Ignore errors during pre-warming
            }
        }
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }
}

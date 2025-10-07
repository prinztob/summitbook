package de.drtobiasprinz.summitbook.utils

import android.util.Log
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.db.entities.GarminData
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.MainActivity
import java.io.BufferedReader
import java.io.IOException

/**
 * Utility class for Android instrumentation tests that reads test data from assets
 */
object TestUtilsForAndroidTest {

    fun getAllEntries(): List<Summit> {
        val entries = mutableListOf<Summit>()

        // Create a BufferedReader from the raw content string
        val reader = getCsvContentSummits()

        try {
            var line: String?
            while (reader?.readLine().also { line = it } != null) {
                val lineLocal = line
                try {
                    if (
                        lineLocal != null &&
                        !lineLocal.startsWith("Activity") &&
                        !lineLocal.startsWith("required")
                    ) {
                        entries.add(Summit.parseFromCsvFileLine(lineLocal, "v0"))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                reader?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        readThirdPartyData(entries)
        return entries
    }

    private fun readThirdPartyData(allSummits: MutableList<Summit>) {
        val reader = getCsvContentThirdPartyData()
        var line: String?
        while (reader?.readLine().also { line = it } != null) {
            val lineLocal = line
            try {
                if (lineLocal != null && !lineLocal.startsWith("activityId") && !lineLocal.startsWith(
                        "required"
                    )
                ) {
                    val added =
                        GarminData.parseFromCsvFileLineAndSave(
                            lineLocal,
                            allSummits,
                            { _, _ -> },
                            ZipFileVersions.V0
                        )
                    if (added) {
                        Log.d(
                            "ZipFileReader",
                            "ThirdPartyData line $lineLocal was added in db."
                        )
                    } else {
                        Log.d("ZipFileReader", "ThirdPartyData line $lineLocal is already db.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun injectTestData(scenario: ActivityScenario<MainActivity>, entries: List<Summit>) {
        scenario.onActivity { activity ->
            activity.viewModel.deleteSummits()
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            prefs.edit().putBoolean(Keys.PREF_CURRENT_YEAR_SWITCH, false).apply()
            activity.viewModel.saveSummits(entries)
        }
        Thread.sleep(500)
    }

    /**
     * Gets only current year entries for testing the year filter
     * The test needs to verify that exactly 85 entries are from the current year
     */
    fun getCurrentYearEntries(): List<Summit> {
        val allEntries = getAllEntries()
        return allEntries.filter { isCurrentYear(it) }
    }

    /**
     * Checks if a summit entry is from the current year
     */
    private fun isCurrentYear(summit: Summit): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        val summitCalendar = java.util.Calendar.getInstance()
        summitCalendar.time = summit.date
        val summitYear = summitCalendar.get(java.util.Calendar.YEAR)

        return summitYear == currentYear
    }
    private fun getCsvContentSummits(): BufferedReader? {
        val resourceSummit = this.javaClass.classLoader?.getResource("summits.csv")
        if (resourceSummit != null) {
            return resourceSummit.openStream().bufferedReader()
        }
        return null
    }

    private fun getCsvContentThirdPartyData(): BufferedReader? {
        val resourceSummit = this.javaClass.classLoader?.getResource("thirdparty.csv")
        if (resourceSummit != null) {
            return resourceSummit.openStream().bufferedReader()
        }
        return null
    }
}
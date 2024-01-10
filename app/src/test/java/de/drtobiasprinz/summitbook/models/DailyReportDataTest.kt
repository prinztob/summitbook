package de.drtobiasprinz.summitbook.models


import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.db.entities.DailyReportData
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.JsonUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DailyReportDataTest {

    @Test
    fun testHeartRateVariability() {
        val resource = this.javaClass.classLoader?.getResource("hrv.json")
        if (resource != null) {
            val gson =
                JsonParser.parseString(JsonUtils.getJsonData(File(resource.path))) as JsonObject
            val actualHrv = DailyReportData.getHrv(gson)
            assertEquals(actualHrv, 50)
        }
    }

    @Test
    fun testParsingOfSummary() {
        val resource = this.javaClass.classLoader?.getResource("summary.json")
        if (resource != null) {
            val gson =
                JsonParser.parseString(JsonUtils.getJsonData(File(resource.path))) as JsonObject
            val actual = DailyReportData.getDailyReportDataFromJsonObject(Summit.parseDate("2023-06-15"), gson)
            assertEquals(actual.steps, 3134)
            assertEquals(actual.floorsClimbed, 17)
            assertEquals(actual.totalIntensityMinutes, 194)
            assertEquals(actual.restingHeartRate, 48)
            assertEquals(actual.maxHeartRate, 159)
            assertEquals(actual.minHeartRate, 47)
            assertEquals(actual.sleepHours, 6.85, 0.01)
            assertFalse(actual.isForWholeDay)
        }
    }

    @Test
    fun testDailyEventsNotEmpty() {
        val resource = this.javaClass.classLoader?.getResource("dailyReport_with_activities.json")
        if (resource != null) {
            val gson =
                JsonParser.parseString(JsonUtils.getJsonData(File(resource.path))) as JsonArray
            val actualEvents = DailyReportData.getDailyEvents(gson)
            assertTrue(actualEvents.isNotEmpty())
            assertEquals(actualEvents.sumOf { it.duration }, 150)
            assertEquals(actualEvents.sumOf { it.moderateIntensityMinutes }, 15)
            assertEquals(actualEvents.sumOf { it.vigorousIntensityMinutes }, 100)
        }
    }

    @Test
    fun testDailyEventsEmpty() {
        val resource = this.javaClass.classLoader?.getResource("dailyReport_empty.json")
        if (resource != null) {
            val gson =
                JsonParser.parseString(JsonUtils.getJsonData(File(resource.path))) as JsonArray
            val actualEvents = DailyReportData.getDailyEvents(gson)

            assertTrue(actualEvents.isEmpty())
        }
    }
}
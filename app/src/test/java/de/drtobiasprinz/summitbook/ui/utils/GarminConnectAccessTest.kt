package de.drtobiasprinz.summitbook.ui.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.utils.GarminConnectAccess.Companion.getDisplayName
import de.drtobiasprinz.summitbook.ui.utils.GarminConnectAccess.Companion.getJsonData
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.text.ParseException
import java.util.*

class GarminConnectAccessTest {
    @Test
    @Throws(Exception::class)
    fun getDisplayName() {
        val response = "window.VIEWER_USERPREFERENCES = JSON.parse({\\\"displayName\\\":\\\"e74a846c-38b5-45d9-bf92-cec22694053d\\\",\\\"preferredLocale"
        Assert.assertEquals(getDisplayName(response), "e74a846c-38b5-45d9-bf92-cec22694053d")
    }

    @Test
    @Throws(ParseException::class)
    fun getSummitsAtDate() {
        val resource = this.javaClass.classLoader?.getResource("activity.json")
        if (resource != null) {
            val activity = File(resource.path)
            val gson = JsonParser().parse(getJsonData(activity)) as JsonArray
            val connectAccess = GarminConnectAccess()
            val actualEntries: ArrayList<SummitEntry> = connectAccess.getSummitsAtDate(gson)
            val expectedEntry = SummitEntry(SummitEntry.parseDate("2019-12-29"), "summit1", SportType.Hike, emptyList(), emptyList(), "", 122, 4.14, 2.99, 9.31, 218, mutableListOf("participant1"), mutableListOf())
            Assert.assertEquals(1, actualEntries.size.toLong())
            Assert.assertEquals(expectedEntry, actualEntries[0])
        }
    }

    @Test
    @Throws(ParseException::class)
    fun parseSportTypeTest() {
        val resource = this.javaClass.classLoader?.getResource("multiple-activities.json")
        if (resource != null) {
            val activity = File(resource.path)
            val gson = JsonParser().parse(getJsonData(activity)) as JsonArray
            val connectAccess = GarminConnectAccess()
            var entry = gson[0] as JsonObject
            Assert.assertEquals(connectAccess.parseSportType(entry["activityType"].asJsonObject), SportType.Hike)
            entry = gson[1] as JsonObject
            Assert.assertEquals(connectAccess.parseSportType(entry["activityType"].asJsonObject), SportType.Bicycle)
            entry = gson[2] as JsonObject
            Assert.assertEquals(connectAccess.parseSportType(entry["activityType"].asJsonObject), SportType.Running)
            entry = gson[3] as JsonObject
            Assert.assertEquals(connectAccess.parseSportType(entry["activityType"].asJsonObject), SportType.Hike)
            entry = gson[4] as JsonObject
            Assert.assertEquals(connectAccess.parseSportType(entry["activityType"].asJsonObject), SportType.Hike)
            entry = gson[5] as JsonObject
            Assert.assertEquals(connectAccess.parseSportType(entry["activityType"].asJsonObject), SportType.BikeAndHike)
        }
    }
}
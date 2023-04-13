package de.drtobiasprinz.summitbook.models


import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.db.entities.SolarIntensity
import de.drtobiasprinz.summitbook.ui.utils.JsonUtils
import junit.framework.Assert.*
import org.junit.Test
import java.io.File

class SolarIntensityTest {
    @Test
    fun testSolarIntensityHalfDay() {
        val resource = this.javaClass.classLoader?.getResource("solar_half_day.json")
        if (resource != null) {
            val gson = JsonParser.parseString(JsonUtils.getJsonData(File(resource.path))) as JsonObject
            val actual = SolarIntensity.parseFromJson(gson)
            assert(actual != null)
            if (actual != null) {
                assertEquals(actual.solarUtilizationInHours, 2.76, 0.01)
                assertEquals(actual.solarExposureInHours, 4.82, 0.01)
                assertFalse(actual.isForWholeDay)
            }
        }
    }

    @Test
    fun testSolarIntensityWholeDay() {
        val resource = this.javaClass.classLoader?.getResource("solar_whole_day.json")
        if (resource != null) {
            val gson = JsonParser.parseString(JsonUtils.getJsonData(File(resource.path))) as JsonObject
            val actual = SolarIntensity.parseFromJson(gson)
            assert(actual != null)
            if (actual != null) {
                assertEquals(actual.solarUtilizationInHours, 0.45, 0.01)
                assertEquals(actual.solarExposureInHours, 1.93, 0.01)
                assertTrue(actual.isForWholeDay)
            }
        }
    }
}
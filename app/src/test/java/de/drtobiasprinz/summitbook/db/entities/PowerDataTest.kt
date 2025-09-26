package de.drtobiasprinz.summitbook.db.entities

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import de.drtobiasprinz.summitbook.utils.TestUtils

class PowerDataTest {

    private val testJson = TestUtils.loadTestJson()

    @Test
    fun testParseFromGarminJson() {
        val jsonObject = JsonParser.parseString(testJson).asJsonObject
        val result = PowerData.parseFromGarminJson(jsonObject)
        
        assertEquals(176.0f, result.avgPower, 0.01f)
        assertEquals(445.0f, result.maxPower, 0.01f)
        assertEquals(204.0f, result.normPower, 0.01f)
        assertEquals(445, result.oneSec)
        assertEquals(443, result.twoSec)
        assertEquals(420, result.fiveSec)
        assertEquals(373, result.tenSec)
        assertEquals(359, result.twentySec)
        assertEquals(344, result.thirtySec)
        assertEquals(301, result.oneMin)
        assertEquals(255, result.twoMin)
        assertEquals(218, result.fiveMin)
        assertEquals(207, result.tenMin)
        assertEquals(178, result.twentyMin)
        assertEquals(183, result.thirtyMin)
        assertEquals(30.9f, result.trainingStressScore, 0.01f)
        assertEquals(0.74f, result.intensityFactor, 0.01f)
    }

    @Test
    fun testToString() {
        val data = PowerData(
            avgPower = 176.0f,
            maxPower = 445.0f,
            normPower = 204.0f,
            oneSec = 445,
            twoSec = 443,
            fiveSec = 420,
            tenSec = 373,
            twentySec = 359,
            thirtySec = 344,
            oneMin = 301,
            twoMin = 255,
            fiveMin = 218,
            tenMin = 207,
            twentyMin = 178,
            thirtyMin = 183,
            trainingStressScore = 30.9f,
            intensityFactor = 0.74f
        )
        
        val expected = "176.0,445.0,204.0,445,443,420,373,359,344,301,255,218,207,178,183,0,0,0,0,0,30.9,0.74"
        assertEquals(expected, data.toString())
    }

    @Test
    fun testHasPowerData() {
        val emptyData = PowerData()
        val populatedData = PowerData(avgPower = 150.0f)
        
        assertFalse(emptyData.hasPowerData())
        assertTrue(populatedData.hasPowerData())
    }
}
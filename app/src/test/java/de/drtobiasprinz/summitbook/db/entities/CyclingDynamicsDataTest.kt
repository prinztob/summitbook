package de.drtobiasprinz.summitbook.db.entities

import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.utils.TestUtils.loadTestJson
import org.junit.Assert.*
import org.junit.Test

class CyclingDynamicsDataTest {

    @Test
    fun testToString() {
        val data = CyclingDynamicsData(
            leftBalance = 50.5f,
            rightBalance = 49.5f,
            leftTorqueEffectiveness = 78.5f,
            rightTorqueEffectiveness = 75.0f,
            leftPedalSmoothness = 23.0f,
            rightPedalSmoothness = 21.5f,
            totalNumberOfStrokes = 1996,
            standingTime = 0,
            maxStandingPower = 65535,
            averageStandingPower = 65535,
            leftPowerPhaseStart = 1,
            leftPowerPhaseEnd = 208,
            leftPowerPhaseArcCenter = 105,
            leftPowerPhasePeakStart = 60,
            leftPowerPhasePeakEnd = 119,
            leftPowerPhasePeakArcCenter = 90,
            leftPlatformCenterOffset = 14,
            rightPowerPhaseStart = 1,
            rightPowerPhaseEnd = 201,
            rightPowerPhaseArcCenter = 101,
            rightPowerPhasePeakStart = 59,
            rightPowerPhasePeakEnd = 116,
            rightPowerPhasePeakArcCenter = 88,
            rightPlatformCenterOffset = 19
        )

        val expected = "50.5,49.5,78.5,75.0,23.0,21.5,1996,0,65535,65535," +
                "1,208,105,60,119,90,14," +
                "1,201,101,59,116,88,19"
        
        assertEquals(expected, data.toString())
    }

    @Test
    fun testHasCyclingDynamics_True() {
        val data = CyclingDynamicsData(leftBalance = 50.5f)
        assertTrue(data.hasCyclingDynamics())
    }

    @Test
    fun testHasCyclingDynamics_False() {
        val data = CyclingDynamicsData()
        assertFalse(data.hasCyclingDynamics())
    }

    @Test
    fun testParse_ValidData() {
        val input = listOf(
            "50.5", "49.5", "78.5", "75.0", "23.0", "21.5",
            "1996", "0", "65535", "65535",
            "1", "208", "105", "60", "119", "90", "14",
            "1", "201", "101", "59", "116", "88", "19"
        )

        val result = CyclingDynamicsData.parse(input)
        
        assertEquals(50.5f, result.leftBalance)
        assertEquals(49.5f, result.rightBalance)
        assertEquals(1996, result.totalNumberOfStrokes)
        assertEquals(14, result.leftPlatformCenterOffset)
        assertEquals(19, result.rightPlatformCenterOffset)
    }

    @Test
    fun testParse_InvalidData() {
        val input = listOf("invalid", "data")
        val result = CyclingDynamicsData.parse(input)
        
        assertEquals(0f, result.leftBalance)
        assertEquals(0, result.totalNumberOfStrokes)
    }

    @Test
    fun testParseFromGarminJson() {
        val jsonObject = JsonParser.parseString(loadTestJson("exercise_set_with_cycling_dynamics.json")).asJsonObject
        val result = CyclingDynamicsData.parseCyclingDynamicsFromGarminJson(jsonObject.getAsJsonObject("summaryDTO"))
        
        assertEquals(50.49f, result.leftBalance)
        assertEquals(49.51f, result.rightBalance)
        assertEquals(78.5f, result.leftTorqueEffectiveness)
        assertEquals(75.0f, result.rightTorqueEffectiveness)
        assertEquals(23.0f, result.leftPedalSmoothness)
        assertEquals(21.5f, result.rightPedalSmoothness)
        assertEquals(1996, result.totalNumberOfStrokes)
        assertEquals(0, result.standingTime)
        assertEquals(0, result.maxStandingPower)
        assertEquals(0, result.averageStandingPower)
        assertEquals(1, result.leftPowerPhaseStart)
        assertEquals(208, result.leftPowerPhaseEnd)
        assertEquals(105, result.leftPowerPhaseArcCenter)
        assertEquals(60, result.leftPowerPhasePeakStart)
        assertEquals(120, result.leftPowerPhasePeakEnd)
        assertEquals(90, result.leftPowerPhasePeakArcCenter)
        assertEquals(14, result.leftPlatformCenterOffset)
        assertEquals(1, result.rightPowerPhaseStart)
        assertEquals(201, result.rightPowerPhaseEnd)
        assertEquals(101, result.rightPowerPhaseArcCenter)
        assertEquals(59, result.rightPowerPhasePeakStart)
        assertEquals(117, result.rightPowerPhasePeakEnd)
        assertEquals(89, result.rightPowerPhasePeakArcCenter)
        assertEquals(19, result.rightPlatformCenterOffset)
    }
}
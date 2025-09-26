package de.drtobiasprinz.summitbook.db.entities

import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.utils.TestUtils.loadTestJson
import org.junit.Assert.*
import org.junit.Test

class ElevationDataTest {

    @Test
    fun testToString() {
        val data = ElevationData(
            maxElevation = 617,
            elevationGain = 131,
            maxVerticalVelocity1Min = 5.0,
            maxVerticalVelocity10Min = 4.5,
            maxVerticalVelocity1h = 3.0,
            maxSlope = 10.2,
            maxVerticalVelocityDown1Min = 6.0,
            maxVerticalVelocityDown10Min = 5.5,
            maxVerticalVelocityDown1h = 4.0,
            maxSlopeDown = 12.3
        )

        val expectedBase = "617,131"
        val expectedCalculated = "5.0,4.5,3.0,10.2,6.0,5.5,4.0,12.3"
        
        assertEquals(expectedBase, data.toStringBase())
        assertEquals(expectedCalculated, data.toStringCalculated())
        assertEquals("$expectedBase,$expectedCalculated", data.toString())
    }

    @Test
    fun testParseFromGarminJson() {
        val jsonObject = JsonParser.parseString(loadTestJson()).asJsonObject
        val result = ElevationData.parseFromGarminJson(jsonObject)
        
        assertEquals(617, result.maxElevation)
        assertEquals(131, result.elevationGain)
    }

    @Test
    fun testParseCalculatedData() {
        val data = ElevationData()
        val input = listOf("5.0", "4.5", "3.0", "10.2", "6.0", "5.5", "4.0", "12.3")
        
        assertTrue(data.parseCalculatedData(input))
        assertEquals(5.0, data.maxVerticalVelocity1Min, 0.01)
        assertEquals(10.2, data.maxSlope, 0.01)
        assertEquals(12.3, data.maxSlopeDown, 0.01)
    }

    @Test
    fun testHasAdditionalData() {
        val emptyData = ElevationData()
        val populatedData = ElevationData(maxVerticalVelocity1Min = 5.0)
        
        assertFalse(emptyData.hasAdditionalData())
        assertTrue(populatedData.hasAdditionalData())
    }

}
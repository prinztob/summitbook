package de.drtobiasprinz.summitbook.db.entities

import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.utils.TestUtils.loadTestJson
import org.junit.Assert.*
import org.junit.Test

class VelocityDataTest {


    @Test
    fun testToString() {
        val data = VelocityData(
            maxVelocity = 16.14,
            oneKilometer = 3.5,
            fiveKilometer = 4.0,
            tenKilometers = 4.2,
            fifteenKilometers = 4.1,
            twentyKilometers = 4.0,
            thirtyKilometers = 3.8,
            fortyKilometers = 3.5,
            fiftyKilometers = 3.2,
            seventyFiveKilometers = 2.8,
            hundredKilometers = 2.5
        )

        val expectedBase = "16.14"
        val expectedCalculated = "3.5,4.0,4.2,4.1,4.0,3.8,3.5,3.2,2.8,2.5"
        
        assertEquals(expectedBase, data.toStringBase())
        assertEquals(expectedCalculated, data.toStringCalculated())
        assertEquals("$expectedBase,$expectedCalculated", data.toString())
    }

    @Test
    fun testParseFromGarminJson() {
        val jsonObject = JsonParser.parseString(loadTestJson()).asJsonObject
        val result = VelocityData.parseFromGarminJson(jsonObject)
        
        assertEquals(58.11, result.maxVelocity, 0.001)
    }

    @Test
    fun testParseCalculatedData() {
        val data = VelocityData()
        val input = listOf("3.5", "4.0", "4.2", "4.1", "4.0", "3.8", "3.5", "3.2", "2.8", "2.5")
        
        assertTrue(data.parseCalculatedData(input))
        assertEquals(3.5, data.oneKilometer, 0.01)
        assertEquals(2.5, data.hundredKilometers, 0.01)
    }

    @Test
    fun testHasAdditionalData() {
        val emptyData = VelocityData()
        val populatedData = VelocityData(oneKilometer = 3.5)
        
        assertFalse(emptyData.hasAdditionalData())
        assertTrue(populatedData.hasAdditionalData())
    }

}
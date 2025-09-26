package de.drtobiasprinz.summitbook.db.entities

import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.utils.TestUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GarminDataTest {

    private val activityIds = mutableListOf("20451565041")

    private fun createTestGarminData(): GarminData {
        val jsonObject = JsonParser.parseString(TestUtils.loadTestJson()).asJsonObject
        val jsonObjectSplits =
            JsonParser.parseString(TestUtils.loadTestJson("exercise_set_with_cycling_dynamics.json")).asJsonObject
        return GarminData.parseFromGarminJson(
            activityIds,
            jsonObject,
            gsonExerciseSet = jsonObjectSplits
        )
    }

    @Test
    fun testParseFromGarminJson() {
        val result = createTestGarminData()

        assertEquals(412.0f, result.calories, 0.01f)
        assertEquals(135.0f, result.averageHR, 0.01f)
        assertEquals(154.0f, result.maxHR, 0.01f)
        assertEquals(204.0f, result.power.normPower, 0.01f)
        assertEquals(50.49f, result.cyclingDynamics.leftBalance, 0.01f)
        assertEquals(1996, result.cyclingDynamics.totalNumberOfStrokes)
    }

    @Test
    fun testGetStringRepresentation() {
        val data = createTestGarminData()
        val summitId = 12345L
        val result = data.getStringRepresentation(summitId)

        assertTrue(result.startsWith("$summitId;${activityIds.first()}"))
        assertTrue(result.contains("412.0"))
        assertTrue(result.contains("135.0"))
    }

    @Test
    fun testToString() {
        val data = createTestGarminData()
        val result = data.toString()

        assertTrue(result.startsWith(activityIds.first()))
        assertTrue(result.contains("412.0"))
        assertTrue(result.contains("135.0"))
    }

    @Test
    fun testEqualsAndHashCode() {
        val data1 = GarminData(mutableListOf("123"))
        val data2 = GarminData(mutableListOf("123"))
        val data3 = GarminData(mutableListOf("456"))

        assertEquals(data1, data2)
        assertNotEquals(data1, data3)
        assertEquals(data1.hashCode(), data2.hashCode())
        assertNotEquals(data1.hashCode(), data3.hashCode())
    }

}
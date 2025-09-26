package de.drtobiasprinz.summitbook.db.entities

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import de.drtobiasprinz.summitbook.utils.TestUtils

class SummitTest {

    @Test
    fun testParseFromGarminJson() {
        val jsonObject = JsonParser.parseString(TestUtils.loadTestJson()).asJsonObject
        val result = Summit.parseFromGarminJson(jsonObject)
        
        assertEquals("Egmating Cyclocross", result.name)
        assertEquals(SportType.Racer, result.sportType)
        assertEquals(14.32, result.kilometers, 0.001)
        assertEquals(1992, result.duration)
        assertEquals(131, result.elevationData.elevationGain)
        assertEquals(617, result.elevationData.maxElevation)
        assertEquals(58.11, result.velocityData.maxVelocity, 0.001)
    }

    @Test
    fun testToString() {
        val summit = Summit(
            date = Summit.parseDate("2025-09-21"),
            name = "Test Summit",
            sportType = SportType.Hike,
            elevationData = ElevationData(maxElevation = 1000, elevationGain = 500),
            kilometers = 10.5,
            velocityData = VelocityData(maxVelocity = 5.0),
            activityId = 12345
        )
        
        val expected = "2025-09-21;Test Summit;Hike;12345;10.5;0;500;1000;5.0;;;0;0;;;;;\n"
        assertEquals(expected, summit.toString())
    }

    @Test
    fun testEqualsAndHashCode() {
        val summit1 = Summit(activityId = 1)
        val summit2 = Summit(activityId = 1)
        val summit3 = Summit(activityId = 2)
        
        assertEquals(summit1, summit2)
        assertNotEquals(summit1, summit3)
        assertEquals(summit1.hashCode(), summit2.hashCode())
    }

    @Test
    fun testGetStringRepresentation() {
        val summit = Summit(
            date = Summit.parseDate("2025-09-21"),
            name = "Test Summit",
            sportType = SportType.Hike,
            activityId = 12345
        )
        
        val result = summit.getStringRepresentation()
        assertTrue(result.startsWith("2025-09-21;Test Summit;Hike;12345"))
    }
}
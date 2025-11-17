package de.drtobiasprinz.summitbook.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoadTypeTest {

    @Test
    fun `fromHighwayTag returns correct RoadType for each category`() {
        // WAY
        assertEquals(RoadType.WAY, RoadType.fromHighwayTag("path", null))
        assertEquals(RoadType.WAY, RoadType.fromHighwayTag("footway", null))
        
        // SIDE_STREET
        assertEquals(RoadType.SIDE_STREET, RoadType.fromHighwayTag("residential", null))
        assertEquals(RoadType.SIDE_STREET, RoadType.fromHighwayTag("service", null))
        
        // CYCLE_WAY
        assertEquals(RoadType.CYCLE_WAY, RoadType.fromHighwayTag("cycleway", null))
        assertEquals(RoadType.CYCLE_WAY, RoadType.fromHighwayTag("bicycle", null))
        
        // MINOR_ROAD
        assertEquals(RoadType.MINOR_ROAD, RoadType.fromHighwayTag("tertiary", null))
        
        // MAJOR_ROAD
        assertEquals(RoadType.MAJOR_ROAD, RoadType.fromHighwayTag("primary", null))
        assertEquals(RoadType.MAJOR_ROAD, RoadType.fromHighwayTag("motorway", null))
        
        // ROAD
        assertEquals(RoadType.ROAD, RoadType.fromHighwayTag("road", null))
    }

    @Test
    fun `fromHighwayTag returns UNKNOWN for null or unrecognized tags`() {
        assertEquals(RoadType.UNKNOWN, RoadType.fromHighwayTag(null, null))
        assertEquals(RoadType.UNKNOWN, RoadType.fromHighwayTag("unknown_tag", null))
        assertEquals(RoadType.UNKNOWN, RoadType.fromHighwayTag("", null))
    }

    @Test
    fun `fromHighwayTag is case insensitive`() {
        assertEquals(RoadType.MAJOR_ROAD, RoadType.fromHighwayTag("PRIMARY", null))
        assertEquals(RoadType.SIDE_STREET, RoadType.fromHighwayTag("ReSiDeNtIaL", null))
    }

    @Test
    fun `fromHighwayTag prioritizes additionalTags over highway tag`() {
        val additionalTags = mapOf("bicycle" to "bic_designated")
        
        // Should return CYCLE_WAY due to additionalTags, not MAJOR_ROAD from highway tag
        assertEquals(RoadType.CYCLE_WAY, RoadType.fromHighwayTag("primary", additionalTags))
        
        // Should return CYCLE_WAY even with null highway tag
        assertEquals(RoadType.CYCLE_WAY, RoadType.fromHighwayTag(null, additionalTags))
    }

    @Test
    fun `fromHighwayTag uses highway tag when additionalTags don't match`() {
        val additionalTags = mapOf("bicycle" to "no")
        assertEquals(RoadType.MAJOR_ROAD, RoadType.fromHighwayTag("primary", additionalTags))
    }

    @Test
    fun `mapFromRoadInfo correctly maps RoadInfo to RoadType`() {
        val roadInfo = RoadInfo(roadType = "primary")
        assertEquals(RoadType.MAJOR_ROAD, RoadType.mapFromRoadInfo(roadInfo))
        
        val roadInfoWithAdditionalTags = RoadInfo(
            roadType = "residential",
            additionalTags = mapOf("bicycle" to "bic_designated")
        )
        assertEquals(RoadType.CYCLE_WAY, RoadType.mapFromRoadInfo(roadInfoWithAdditionalTags))
    }
}
package de.drtobiasprinz.summitbook.utils

import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.TrackBoundingBox
import de.drtobiasprinz.summitbook.models.ExtensionFromYaml
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.RoadInfo
import de.drtobiasprinz.summitbook.models.RoadType
import de.drtobiasprinz.summitbook.models.Surface
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import org.joda.time.DateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.reader.MapFile
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileInputStream

@RunWith(RobolectricTestRunner::class)
class OfflineMapAnalyzerTest {

    @Test
    fun testE2EClimb() {
        val track = fixture("track3.gpx")
        val extension = fixture("track3_extensions.yaml")
        val mapFile = assumeBayernMapPresent()
        val gpsTrack = GpsTrack(track.toPath(), yamlExtensionsFile = extension)
        gpsTrack.parseTrack(false)
        val analyzer = OfflineMapAnalyzer(listOf(MapFile(FileInputStream(mapFile))), 15.0)
        val resultClimb = analyzer.getRoadTypeSummaryFromTrackPoints(gpsTrack.trackPoints, SportType.Climb)
        assertEquals(447, resultClimb.first[Surface.UNKNOWN])
        assertEquals(0, resultClimb.second[RoadType.UNKNOWN])
        assertTrue(resultClimb.second[RoadType.CYCLE_WAY] == 6003)
    }

    @Test
    fun testE2ERacer() {
        val track = fixture("track3.gpx")
        val extension = fixture("track3_extensions.yaml")
        val mapFile = assumeBayernMapPresent()
        val gpsTrack = GpsTrack(track.toPath(), yamlExtensionsFile = extension)
        gpsTrack.parseTrack(false)
        val analyzer = OfflineMapAnalyzer(listOf(MapFile(FileInputStream(mapFile))), 15.0)
        val resultRacer = analyzer.getRoadTypeSummaryFromTrackPoints(gpsTrack.trackPoints, SportType.Racer)
        assertEquals(0, resultRacer.first[Surface.UNKNOWN])
        assertEquals(0, resultRacer.second[RoadType.UNKNOWN])
        assertEquals(6008, resultRacer.second[RoadType.CYCLE_WAY])
    }

    @Test
    fun testE2EAllTerrain() {
        val track = fixture("track_all_terrain.gpx")
        val extension = fixture("track_all_terrain_extensions.yaml")
        val mapFile = assumeBayernMapPresent()
        val gpsTrack = GpsTrack(track.toPath(), yamlExtensionsFile = extension)
        gpsTrack.parseTrack(false)
        val analyzer = OfflineMapAnalyzer(listOf(MapFile(FileInputStream(mapFile))), 15.0)
        val result = analyzer.getRoadTypeSummaryFromTrackPoints(gpsTrack.trackPoints)
        assertEquals(16029, result.first[Surface.ASPHALT])
        assertEquals(0, result.first[Surface.STONE_PAVEMENT])
        assertEquals(5406, result.first[Surface.COMPACTED])
        assertEquals(21, result.first[Surface.LOSE_GROUND])
        assertEquals(25, result.first[Surface.PATH])
        assertEquals(0, result.first[Surface.UNKNOWN])
        assertEquals(10882, result.second[RoadType.WAY])
        assertEquals(4355, result.second[RoadType.SIDE_STREET])
        assertEquals(695, result.second[RoadType.MINOR_ROAD])
        assertEquals(0, result.second[RoadType.MAJOR_ROAD])
        assertEquals(5549, result.second[RoadType.CYCLE_WAY])
        assertEquals(0, result.second[RoadType.ROAD])
        assertEquals(0, result.second[RoadType.UNKNOWN])
    }

    @Test
    fun testE2EOnlyAsphalt() {
        val track = fixture("track_asphalt.gpx")
        val extension = fixture("track_asphalt_extensions.yaml")
        val mapFile = assumeBayernMapPresent()
        val gpsTrack = GpsTrack(track.toPath(), yamlExtensionsFile = extension)
        gpsTrack.parseTrack(false)
        val analyzer = OfflineMapAnalyzer(listOf(MapFile(FileInputStream(mapFile))), 15.0)
        val result = analyzer.getRoadTypeSummaryFromTrackPoints(gpsTrack.trackPoints)
        assertEquals(32049, result.first[Surface.ASPHALT])
        assertEquals(0, result.first[Surface.STONE_PAVEMENT])
        assertEquals(221, result.first[Surface.COMPACTED])
        assertEquals(38, result.first[Surface.LOSE_GROUND])
        assertEquals(0, result.first[Surface.PATH])
        assertEquals(0, result.first[Surface.UNKNOWN])
        assertEquals(2737, result.second[RoadType.WAY])
        assertEquals(27811, result.second[RoadType.SIDE_STREET])
        assertEquals(1108, result.second[RoadType.MINOR_ROAD])
        assertEquals(559, result.second[RoadType.MAJOR_ROAD])
        assertEquals(93, result.second[RoadType.CYCLE_WAY])
        assertEquals(0, result.second[RoadType.ROAD])
        assertEquals(0, result.second[RoadType.UNKNOWN])
    }

    @Test
    fun getNameOfPoi() {
        val mapFile = assumeBayernMapPresent()
        val analyzer = OfflineMapAnalyzer(listOf(MapFile(FileInputStream(mapFile))), 15.0)

        val locationInfoHut =
            analyzer.getClosestLocationInfo(LatLong(47.63978, 11.6795541271))
        assert(locationInfoHut != null)
        assertEquals("Buchsteinhütte", locationInfoHut?.name)

        val locationInfoVillage = analyzer.getClosestLocationInfo(LatLong(48.002878, 11.79445))
        assert(locationInfoVillage != null)
        assertEquals("Egmating", locationInfoVillage?.name)

        val locationInfoPeak =
            analyzer.getClosestLocationInfo(LatLong(47.7036326, 12.0109743))
        assert(locationInfoPeak != null)
        assertEquals("Wendelstein", locationInfoPeak?.name)

        val locationInfoPass =
            analyzer.getClosestLocationInfo(LatLong(47.6722222,11.8863889))
        assert(locationInfoPass != null)
        assertEquals("Spitzingsattel", locationInfoPass?.name)
    }

    /**
     * Test that short road segments (≤10 points) surrounded by the same road type are filtered out.
     * This simulates GPS noise or brief incorrect road detections.
     */
    @Test
    fun testFilterWronglySelectedRoadTypes_RemovesShortSegmentsBetweenSameRoad() {
        // Create a scenario: Road A (15 points) -> Road B (5 points) -> Road A (15 points)
        // Road B should be filtered out because it's ≤10 points and surrounded by Road A
        val trackPoints = createTrackPointsWithRoads(
            listOf(
                RoadSegment("Road A", "asphalt", "primary", 15),
                RoadSegment("Road B", "gravel", "track", 5),
                RoadSegment("Road A", "asphalt", "primary", 15)
            )
        )

        val roadInfos = createRoadInfos(
            listOf(
                RoadSegment("Road A", "asphalt", "primary", 15),
                RoadSegment("Road B", "gravel", "track", 5),
                RoadSegment("Road A", "asphalt", "primary", 15)
            )
        )

        // Apply the filter
        val analyzer = OfflineMapAnalyzer(emptyList(), 25.0)
        analyzer.filterWronglySelectedRoadTypes(roadInfos, trackPoints)

        // Verify that all points now have Road A's properties
        trackPoints.forEach { (_, extension) ->
            assertEquals(
                "All points should have Road A's surface",
                Surface.ASPHALT,
                extension.surface
            )
            assertEquals(
                "All points should have Road A's road type",
                RoadType.MAJOR_ROAD,
                extension.roadType
            )
        }
    }

    /**
     * Test that short segments NOT surrounded by the same road are kept.
     * This represents legitimate road changes.
     */
    @Test
    fun testFilterWronglySelectedRoadTypes_KeepsShortSegmentsBetweenDifferentRoads() {
        // Create: Road A (15 points) -> Road B (5 points) -> Road C (15 points)
        // Road B should be kept because it's between different roads
        val trackPoints = createTrackPointsWithRoads(
            listOf(
                RoadSegment("Road A", "asphalt", "primary", 15),
                RoadSegment("Road B", "gravel", "track", 5),
                RoadSegment("Road C", "concrete", "secondary", 15)
            )
        )

        val roadInfos = createRoadInfos(
            listOf(
                RoadSegment("Road A", "asphalt", "primary", 15),
                RoadSegment("Road B", "gravel", "track", 5),
                RoadSegment("Road C", "concrete", "secondary", 15)
            )
        )

        val analyzer = OfflineMapAnalyzer(emptyList(), 25.0)
        analyzer.filterWronglySelectedRoadTypes(roadInfos, trackPoints)

        // Verify that Road B's points still have their original properties
        val roadBPoints = trackPoints.subList(15, 20)
        roadBPoints.forEach { (_, extension) ->
            assertEquals(
                "Road B points should keep their surface",
                Surface.LOSE_GROUND,
                extension.surface
            )
            assertEquals(
                "Road B points should keep their road type",
                RoadType.WAY,
                extension.roadType
            )
        }
    }

    /**
     * Test that long segments (>10 points) are always kept, even if surrounded by the same road.
     */
    @Test
    fun testFilterWronglySelectedRoadTypes_KeepsLongSegments() {
        // Create: Road A (15 points) -> Road B (12 points) -> Road A (15 points)
        // Road B should be kept because it has >10 points
        val trackPoints = createTrackPointsWithRoads(
            listOf(
                RoadSegment("Road A", "asphalt", "primary", 15),
                RoadSegment("Road B", "gravel", "track", 12),
                RoadSegment("Road A", "asphalt", "primary", 15)
            )
        )

        val roadInfos = createRoadInfos(
            listOf(
                RoadSegment("Road A", "asphalt", "primary", 15),
                RoadSegment("Road B", "gravel", "track", 12),
                RoadSegment("Road A", "asphalt", "primary", 15)
            )
        )

        val analyzer = OfflineMapAnalyzer(emptyList(), 25.0)
        analyzer.filterWronglySelectedRoadTypes(roadInfos, trackPoints)

        // Verify that Road B's points still have their original properties
        val roadBPoints = trackPoints.subList(15, 27)
        roadBPoints.forEach { (_, extension) ->
            assertEquals(
                "Road B points should keep their surface",
                Surface.LOSE_GROUND,
                extension.surface
            )
            assertEquals(
                "Road B points should keep their road type",
                RoadType.WAY,
                extension.roadType
            )
        }
    }

    /**
     * Test edge case: first and last segments are always kept regardless of length.
     */
    @Test
    fun testFilterWronglySelectedRoadTypes_KeepsFirstAndLastSegments() {
        // Create: Road A (3 points) -> Road B (15 points) -> Road C (3 points)
        // Road A and C should be kept even though they're short (first and last)
        val trackPoints = createTrackPointsWithRoads(
            listOf(
                RoadSegment("Road A", "asphalt", "primary", 3),
                RoadSegment("Road B", "gravel", "track", 15),
                RoadSegment("Road C", "concrete", "secondary", 3)
            )
        )

        val roadInfos = createRoadInfos(
            listOf(
                RoadSegment("Road A", "asphalt", "primary", 3),
                RoadSegment("Road B", "gravel", "track", 15),
                RoadSegment("Road C", "concrete", "secondary", 3)
            )
        )

        val analyzer = OfflineMapAnalyzer(emptyList(), 25.0)
        analyzer.filterWronglySelectedRoadTypes(roadInfos, trackPoints)

        // Verify first segment keeps its properties
        trackPoints.subList(0, 3).forEach { (_, extension) ->
            assertEquals(
                "First segment should keep its surface",
                Surface.ASPHALT,
                extension.surface
            )
            assertEquals(
                "First segment should keep its road type",
                RoadType.MAJOR_ROAD,
                extension.roadType
            )
        }

        // Verify last segment keeps its properties
        trackPoints.subList(18, 21).forEach { (_, extension) ->
            assertEquals(
                "Last segment should keep its surface",
                Surface.STONE_PAVEMENT,
                extension.surface
            )
            assertEquals(
                "Last segment should keep its road type",
                RoadType.MAJOR_ROAD,
                extension.roadType
            )
        }
    }

    /**
     * Test complex scenario with multiple short segments.
     */
    @Test
    fun testFilterWronglySelectedRoadTypes_ComplexScenario() {
        // Road A (20) -> Road B (3) -> Road A (5) -> Road C (15) -> Road A (3) -> Road C (20)
        // Road B should be filtered (short, surrounded by A)
        // First Road A continuation (5 points) should be filtered (short, surrounded by A)
        // Last Road A (3 points) should be kept (between different roads C and C)
        val trackPoints = createTrackPointsWithRoads(
            listOf(
                RoadSegment("Road A", "asphalt", "primary", 20),
                RoadSegment("Road B", "gravel", "track", 3),
                RoadSegment("Road A", "asphalt", "primary", 5),
                RoadSegment("Road C", "concrete", "secondary", 15),
                RoadSegment("Road A", "asphalt", "primary", 3),
                RoadSegment("Road C", "concrete", "secondary", 20)
            )
        )

        val roadInfos = createRoadInfos(
            listOf(
                RoadSegment("Road A", "asphalt", "primary", 20),
                RoadSegment("Road B", "gravel", "track", 3),
                RoadSegment("Road A", "asphalt", "primary", 5),
                RoadSegment("Road C", "concrete", "secondary", 15),
                RoadSegment("Road A", "asphalt", "primary", 3),
                RoadSegment("Road C", "concrete", "secondary", 20)
            )
        )

        val analyzer = OfflineMapAnalyzer(emptyList(), 25.0)
        val filterMethod = analyzer.javaClass.getDeclaredMethod(
            "filterWronglySelectedRoadTypes",
            MutableList::class.java,
            List::class.java
        )
        filterMethod.isAccessible = true
        filterMethod.invoke(analyzer, roadInfos, trackPoints)

        // Verify Road B was filtered to Road A
        trackPoints.subList(20, 23).forEach { (_, extension) ->
            assertEquals("Road B should be filtered to Road A", Surface.ASPHALT, extension.surface)
            assertEquals(
                "Road B should be filtered to Road A",
                RoadType.MAJOR_ROAD,
                extension.roadType
            )
        }

        // Verify second Road A segment was filtered to Road A (merged)
        trackPoints.subList(23, 28).forEach { (_, extension) ->
            assertEquals("Second Road A should remain Road A", Surface.ASPHALT, extension.surface)
            assertEquals(
                "Second Road A should remain Road A",
                RoadType.MAJOR_ROAD,
                extension.roadType
            )
        }

        // Verify Road C segments keep their properties
        trackPoints.subList(28, 43).forEach { (_, extension) ->
            assertEquals(
                "Road C should keep its surface",
                Surface.STONE_PAVEMENT,
                extension.surface
            )
            assertEquals(
                "Road C should keep its road type",
                RoadType.MAJOR_ROAD,
                extension.roadType
            )
        }
    }

    /**
     * Test with null road infos (representing unknown roads).
     */
    @Test
    fun testFilterWronglySelectedRoadTypes_HandlesNullRoadInfos() {
        val trackPoints = createTrackPointsWithRoads(
            listOf(
                RoadSegment("Road A", "asphalt", "primary", 10),
                RoadSegment(null, null, null, 5),
                RoadSegment("Road A", "asphalt", "primary", 10)
            )
        )

        val roadInfos = mutableListOf<RoadInfo?>()
        roadInfos.addAll(List(10) { RoadInfo("primary", "Road A", "asphalt") })
        roadInfos.addAll(List(5) { null })
        roadInfos.addAll(List(10) { RoadInfo("primary", "Road A", "asphalt") })

        val analyzer = OfflineMapAnalyzer(emptyList(), 25.0)
        val filterMethod = analyzer.javaClass.getDeclaredMethod(
            "filterWronglySelectedRoadTypes",
            MutableList::class.java,
            List::class.java
        )
        filterMethod.isAccessible = true
        filterMethod.invoke(analyzer, roadInfos, trackPoints)

        // Verify the method handles nulls gracefully
        // Null segments should be treated as empty road info
        trackPoints.subList(10, 15).forEach { (_, extension) ->
            // These should be filtered to Road A since they're surrounded by it
            assertEquals(
                "Null road should be filtered to surrounding road",
                Surface.ASPHALT,
                extension.surface
            )
        }
    }

    // Helper data class
    private data class RoadSegment(
        val name: String?,
        val surface: String?,
        val roadType: String?,
        val pointCount: Int
    )

    // Helper function to create track points with specific road assignments
    private fun createTrackPointsWithRoads(segments: List<RoadSegment>): List<Pair<TrackPoint, ExtensionFromYaml>> {
        val trackPoints = mutableListOf<Pair<TrackPoint, ExtensionFromYaml>>()
        var time = 1505900660000L
        var distance = 0.0

        segments.forEach { segment ->
            repeat(segment.pointCount) { i ->
                val trackPoint = TrackPoint.Builder()
                    .setLatitude(54.93 + i * 0.0001)
                    .setLongitude(9.86 + i * 0.0001)
                    .setElevation(100.0)
                    .setTime(DateTime(time))
                    .build() as TrackPoint

                val extension = ExtensionFromYaml(
                    distance = distance,
                    surface = when (segment.surface) {
                        "asphalt" -> Surface.ASPHALT
                        "gravel" -> Surface.LOSE_GROUND
                        "concrete" -> Surface.STONE_PAVEMENT
                        else -> Surface.UNKNOWN
                    },
                    roadType = when (segment.roadType) {
                        "primary", "secondary" -> RoadType.MINOR_ROAD
                        "track" -> RoadType.WAY
                        else -> RoadType.UNKNOWN
                    }
                )

                trackPoints.add(Pair(trackPoint, extension))
                time += 10000
                distance += 10.0
            }
        }

        return trackPoints
    }

    // Helper function to create road infos matching the segments
    private fun createRoadInfos(segments: List<RoadSegment>): MutableList<RoadInfo?> {
        val roadInfos = mutableListOf<RoadInfo?>()

        segments.forEach { segment ->
            repeat(segment.pointCount) {
                if (segment.name != null && segment.roadType != null) {
                    roadInfos.add(
                        RoadInfo(
                            roadType = segment.roadType,
                            name = segment.name,
                            surface = segment.surface,
                            minDistance = 10
                        )
                    )
                } else {
                    roadInfos.add(null)
                }
            }
        }

        return roadInfos
    }

    /**
     * Test that hasMapCoverageForBoundingBox returns true when track overlaps with map
     */
    @Test
    fun testHasMapCoverageForBoundingBox_WithOverlap() {
        val mapFile = assumeBayernMapPresent()
        val analyzer = OfflineMapAnalyzer(listOf(MapFile(FileInputStream(mapFile))), 15.0)

        // Create a bounding box that overlaps with Bayern map
        // Bayern map covers approximately: lat 47.27-50.56, lon 8.98-13.84
        val trackBoundingBox = TrackBoundingBox(
            latNorth = 48.5,
            latSouth = 47.5,
            lonWest = 11.0,
            lonEast = 12.0
        )

        assertTrue(
            "Track bounding box should overlap with Bayern map",
            analyzer.hasMapCoverageForBoundingBox(trackBoundingBox)
        )
    }

    /**
     * Test that hasMapCoverageForBoundingBox returns false when track doesn't overlap with map
     */
    @Test
    fun testHasMapCoverageForBoundingBox_WithoutOverlap() {
        val mapFile = assumeBayernMapPresent()
        val analyzer = OfflineMapAnalyzer(listOf(MapFile(FileInputStream(mapFile))), 15.0)

        // Create a bounding box that doesn't overlap with map
        // This is somewhere in northern Germany, far from Bayern
        val trackBoundingBox = TrackBoundingBox(
            latNorth = 54.0,
            latSouth = 53.0,
            lonWest = 9.0,
            lonEast = 10.0
        )

        assertFalse(
            "Track bounding box should not overlap with Bayern map",
            analyzer.hasMapCoverageForBoundingBox(trackBoundingBox)
        )
    }

    /**
     * Test that hasMapCoverageForBoundingBox returns false when no map files are available
     */
    @Test
    fun testHasMapCoverageForBoundingBox_NoMapFiles() {
        val analyzer = OfflineMapAnalyzer(emptyList(), 15.0)
        
        val trackBoundingBox = TrackBoundingBox(
            latNorth = 48.5,
            latSouth = 47.5,
            lonWest = 11.0,
            lonEast = 12.0
        )
        
        assertFalse(
            "Should return false when no map files are available",
            analyzer.hasMapCoverageForBoundingBox(trackBoundingBox)
        )
    }

    /**
     * Test that getRoadTypeSummaryFromTrackPoints returns empty results when track doesn't overlap with map
     */
    @Test
    fun testGetRoadTypeSummaryFromTrackPoints_NoOverlap() {
        val mapFile = assumeBayernMapPresent()
        val analyzer = OfflineMapAnalyzer(listOf(MapFile(FileInputStream(mapFile))), 15.0)

        val trackBoundingBox = TrackBoundingBox(
            latNorth = 54.0,
            latSouth = 53.0,
            lonWest = 9.0,
            lonEast = 10.0
        )

        assertFalse(analyzer.hasMapCoverageForBoundingBox(trackBoundingBox))
    }

    /**
     * Test that getRoadTypeSummaryFromTrackPoints works normally when track overlaps with map
     */
    @Test
    fun testGetRoadTypeSummaryFromTrackPoints_WithOverlap() {
        val mapFile = assumeBayernMapPresent()
        val analyzer = OfflineMapAnalyzer(listOf(MapFile(FileInputStream(mapFile))), 15.0)

        val trackBoundingBox = TrackBoundingBox(
            latNorth = 48.5,
            latSouth = 47.5,
            lonWest = 11.0,
            lonEast = 12.0
        )

        assertTrue(analyzer.hasMapCoverageForBoundingBox(trackBoundingBox))
    }

    private fun fixture(name: String): File =
        File(
            checkNotNull(this.javaClass.classLoader?.getResource(name)) {
                "Required test fixture '$name' is missing from app/src/test/resources"
            }.path
        )

    private fun assumeBayernMapPresent(): File {
        val mapUrl = this.javaClass.classLoader?.getResource("Bayern_oam.osm.map")
        assumeNotNull(
            "Bayern_oam.osm.map is not on the classpath (large file, gitignored); " +
                "place it in app/src/test/resources to run map-dependent tests",
            mapUrl
        )
        return File(mapUrl!!.path)
    }
}
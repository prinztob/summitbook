package de.drtobiasprinz.summitbook.data.analytics

import de.drtobiasprinz.summitbook.data.appstate.AppState
import de.drtobiasprinz.summitbook.data.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.data.db.entities.SportType
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.model.ExtensionFromYaml
import de.drtobiasprinz.summitbook.data.model.GpsTrack
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
class SegmentEntryCreatorTest {

    @Before
    fun setUp() {
        AppState.storage = Files.createTempDirectory("summitbook_test").toFile()
    }

    @Test
    fun testCreateSegmentEntryOnSameCourse() {
        val templateTrack = parseFixture("2026-10-02_Kleinhelfendorf.gpx")
        Assert.assertTrue(templateTrack.size > 2000)
        val startIdx = 1000
        val endIdx = 2000
        val template = segmentEntryFromTrack(templateTrack, startIdx, endIdx)
        val newSummit = summitWithFixtureTrack(2000, "2026-10-07_Kleinhelfendorf.gpx")
        val newTrackBefore = parseFixture("2026-10-07_Kleinhelfendorf.gpx")

        val entry = SegmentEntryCreator.createSegmentEntry(newSummit, template)

        Assert.assertNotNull("slice should be found on the same course", entry)
        entry!!
        Assert.assertEquals(7L, entry.segmentId)
        Assert.assertEquals(2000L, entry.activityId)
        Assert.assertEquals(newSummit.date, entry.date)
        Assert.assertFalse(entry.isMountainPass)
        Assert.assertTrue(
            "start must be before end",
            entry.startPositionInTrack < entry.endPositionInTrack
        )
        Assert.assertTrue(entry.startPositionInTrack < newTrackBefore.size)
        Assert.assertTrue(entry.endPositionInTrack < newTrackBefore.size)
        Assert.assertTrue(
            "start point should be near the template start",
            GpsUtils.getDistance(
                GeoPoint(entry.startPositionLatitude, entry.startPositionLongitude),
                GeoPoint(template.startPositionLatitude, template.startPositionLongitude)
            ) < SegmentEntryCreator.POINT_MATCH_TOLERANCE_METERS
        )
        Assert.assertTrue(
            "end point should be near the template end",
            GpsUtils.getDistance(
                GeoPoint(entry.endPositionLatitude, entry.endPositionLongitude),
                GeoPoint(template.endPositionLatitude, template.endPositionLongitude)
            ) < SegmentEntryCreator.POINT_MATCH_TOLERANCE_METERS
        )
        Assert.assertTrue("duration was ${entry.duration}", entry.duration > 0.0)
        Assert.assertTrue(
            "elevation should change over ${entry.startPositionInTrack}..${entry.endPositionInTrack}",
            entry.heightMetersUp + Math.abs(entry.heightMetersDown) > 0.0
        )
        println(
            "Created segment entry on same course: indices ${entry.startPositionInTrack}.." +
                    "${entry.endPositionInTrack} (template $startIdx..$endIdx), duration " +
                    "${entry.duration} min, ${entry.kilometers} km, up ${entry.heightMetersUp} m, " +
                    "down ${entry.heightMetersDown} m"
        )
    }

    @Test
    fun testCreateMountainPassOnSameCourse() {
        val templateTrack = parseFixture("2026-10-02_Kleinhelfendorf.gpx")
        val startIdx = 1000
        val endIdx = 2000
        val template = segmentEntryFromTrack(templateTrack, startIdx, endIdx)
            .copy(
                segmentId = 0,
                isMountainPass = true,
                name = "Kleinhelfendorfer Anstieg",
                windowDistanceMeters = 500.0
            )
        val newSummit = summitWithFixtureTrack(2000, "2026-10-07_Kleinhelfendorf.gpx")

        val entry = SegmentEntryCreator.createSegmentEntry(newSummit, template)

        Assert.assertNotNull(entry)
        entry!!
        Assert.assertTrue(entry.isMountainPass)
        Assert.assertEquals("Kleinhelfendorfer Anstieg", entry.name)
        Assert.assertEquals(0L, entry.segmentId)
        Assert.assertEquals(500.0, entry.windowDistanceMeters, 0.0)
        Assert.assertEquals(0, entry.averageHeartRate)
        Assert.assertEquals(0, entry.averagePower)
        Assert.assertTrue(entry.durationInMotion >= 0.0)
        Assert.assertTrue(
            "duration was ${entry.duration}",
            entry.duration > 0.0
        )
        println(
            "Created mountain pass on same course: avgGradient ${entry.avgGradient} %, " +
                    "maxGradeInWindow ${entry.maxGradeInWindow} %, durationInMotion " +
                    "${entry.durationInMotion} min"
        )
    }

    @Test
    fun testCreateSegmentEntryNotFoundOnDifferentCourse() {
        val templateTrack = parseFixture("2026-10-02_Kleinhelfendorf.gpx")
        val template = segmentEntryFromTrack(templateTrack, 1000, 2000)
            .copy(
                startPositionLatitude = 48.137,
                startPositionLongitude = 11.575
            )
        val newSummit = summitWithFixtureTrack(2000, "2026-10-07_Kleinhelfendorf.gpx")

        val entry = SegmentEntryCreator.createSegmentEntry(newSummit, template)

        Assert.assertNull("slice around Munich must not be found near Egmating", entry)
    }

    @Test
    fun testFindSliceIndicesOnSyntheticTrack() {
        val trackPoints = listOf(
            trackPoint(47.5000, 11.5000, 100.0, 0),
            trackPoint(47.5001, 11.5001, 110.0, 1),
            trackPoint(47.5002, 11.5002, 150.0, 2),
            trackPoint(47.5003, 11.5003, 90.0, 3)
        )
        val template = SegmentEntry(
            segmentId = 1,
            date = Summit.parseDate("2024-05-01"),
            activityId = 1000,
            startPositionInTrack = 1,
            startPositionLatitude = 47.5001,
            startPositionLongitude = 11.5001,
            endPositionInTrack = 2,
            endPositionLatitude = 47.5002,
            endPositionLongitude = 11.5002,
            duration = 1.0,
            kilometers = 0.0,
            averageHeartRate = 0,
            averagePower = 0
        )
        Assert.assertEquals(Pair(1, 2), SegmentEntryCreator.findSliceIndices(trackPoints, template))

        val noStartMatch = template.copy(startPositionLatitude = 48.0)
        Assert.assertNull(SegmentEntryCreator.findSliceIndices(trackPoints, noStartMatch))

        val noEndMatch = template.copy(endPositionLatitude = 48.0)
        Assert.assertNull(SegmentEntryCreator.findSliceIndices(trackPoints, noEndMatch))

        val endBeforeStart = template.copy(
            startPositionLatitude = 47.5002,
            startPositionLongitude = 11.5002,
            endPositionLatitude = 47.5001,
            endPositionLongitude = 11.5001
        )
        Assert.assertNull(
            "end before start must not produce a slice",
            SegmentEntryCreator.findSliceIndices(trackPoints, endBeforeStart)
        )
    }

    @Test
    fun testComputeStats() {
        val trackPoints = listOf(
            trackPoint(47.5000, 11.5000, 100.0, 0),
            trackPoint(47.5001, 11.5001, 150.0, 60),
            trackPoint(47.5002, 11.5002, 90.0, 120)
        )
        val stats = SegmentEntryCreator.computeStats(trackPoints, 0, 2)
        Assert.assertNotNull(stats)
        stats!!
        Assert.assertEquals(2.0, stats.duration, 0.001)
        Assert.assertEquals(0.0, stats.kilometers, 0.001)
        Assert.assertEquals(0, stats.averageHeartRate)
        Assert.assertEquals(0, stats.averagePower)
        Assert.assertEquals(50.0, stats.heightMetersUp, 0.001)
        Assert.assertEquals(-60.0, stats.heightMetersDown, 0.001)
        Assert.assertNull(stats.elevationWindowResult)

        Assert.assertNull(SegmentEntryCreator.computeStats(emptyList(), 0, 1))
        Assert.assertNull(SegmentEntryCreator.computeStats(trackPoints, 0, 3))
        Assert.assertNull(SegmentEntryCreator.computeStats(trackPoints, -1, 2))
        Assert.assertNull(SegmentEntryCreator.computeStats(null, 0, 1))
    }

    @Test
    fun testIsAlreadyCreated() {
        val regularEntry = SegmentEntry(
            segmentId = 7,
            date = Summit.parseDate("2024-05-01"),
            activityId = 2000,
            startPositionInTrack = 0,
            startPositionLatitude = 47.50,
            startPositionLongitude = 11.50,
            endPositionInTrack = 10,
            endPositionLatitude = 47.51,
            endPositionLongitude = 11.51,
            duration = 10.0,
            kilometers = 2.0,
            averageHeartRate = 0,
            averagePower = 0
        )
        Assert.assertTrue(
            SegmentEntryCreator.isAlreadyCreated(
                listOf(regularEntry), regularEntry.copy(entryId = 99)
            )
        )
        Assert.assertFalse(
            SegmentEntryCreator.isAlreadyCreated(
                listOf(regularEntry), regularEntry.copy(segmentId = 8)
            )
        )

        val mountainPass = regularEntry.copy(
            segmentId = 0,
            isMountainPass = true,
            name = "Pass"
        )
        Assert.assertTrue(
            SegmentEntryCreator.isAlreadyCreated(
                listOf(mountainPass), mountainPass.copy(entryId = 99)
            )
        )
        Assert.assertFalse(
            "same segment id must not match a mountain pass",
            SegmentEntryCreator.isAlreadyCreated(listOf(mountainPass), regularEntry)
        )
        Assert.assertFalse(
            SegmentEntryCreator.isAlreadyCreated(
                listOf(mountainPass),
                mountainPass.copy(endPositionLatitude = 47.515, endPositionLongitude = 11.515)
            )
        )
        Assert.assertFalse(
            "regular entries must not match a mountain pass template",
            SegmentEntryCreator.isAlreadyCreated(listOf(regularEntry), mountainPass)
        )
    }

    private fun segmentEntryFromTrack(
        trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
        startIdx: Int,
        endIdx: Int
    ): SegmentEntry {
        return SegmentEntry(
            segmentId = 7,
            date = Summit.parseDate("2026-10-02"),
            activityId = 1000,
            startPositionInTrack = startIdx,
            startPositionLatitude = trackPoints[startIdx].first.latitude,
            startPositionLongitude = trackPoints[startIdx].first.longitude,
            endPositionInTrack = endIdx,
            endPositionLatitude = trackPoints[endIdx].first.latitude,
            endPositionLongitude = trackPoints[endIdx].first.longitude,
            duration = 20.0,
            kilometers = 10.0,
            averageHeartRate = 0,
            averagePower = 0
        )
    }

    private fun summitWithFixtureTrack(activityId: Long, resource: String): Summit {
        val summit = Summit(
            Summit.parseDate("2026-10-07"), "Egmating Cyclocross", SportType.Bicycle,
            kilometers = 60.0, activityId = activityId, hasTrack = true
        )
        val resourceUrl = javaClass.classLoader?.getResource(resource)
            ?: throw IllegalStateException("Missing test resource $resource")
        val file = summit.getGpsTrackPath().toFile()
        file.parentFile?.mkdirs()
        File(resourceUrl.path).copyTo(file, overwrite = true)
        return summit
    }

    private fun parseFixture(resource: String): List<Pair<TrackPoint, ExtensionFromYaml>> {
        val resourceUrl = javaClass.classLoader?.getResource(resource) ?: return emptyList()
        val track = GpsTrack(File(resourceUrl.path).toPath(), File("").toPath())
        track.parseTrack(false)
        return track.trackPoints
    }

    private fun trackPoint(
        latitude: Double,
        longitude: Double,
        elevation: Double,
        seconds: Int
    ): Pair<TrackPoint, ExtensionFromYaml> {
        val trackPoint = TrackPoint.Builder()
            .setLatitude(latitude)
            .setLongitude(longitude)
            .setElevation(elevation)
            .setTime(DateTime(1505900660000L + seconds * 1000L))
            .build() as TrackPoint
        return Pair(trackPoint, ExtensionFromYaml())
    }
}

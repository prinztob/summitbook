package de.drtobiasprinz.summitbook.data.analytics

import de.drtobiasprinz.summitbook.data.appstate.AppState
import de.drtobiasprinz.summitbook.data.db.entities.SportType
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.db.entities.TrackBoundingBox
import de.drtobiasprinz.summitbook.data.model.GpsTrack
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files
import kotlin.math.cos

@RunWith(RobolectricTestRunner::class)
class TrackSimilarityTest {

    @Before
    fun setUp() {
        AppState.storage = Files.createTempDirectory("summitbook_test").toFile()
    }

    @Test
    fun testIdenticalTrackHasPerfectScore() {
        val points = getTrackPoints("track.gpx")
        Assert.assertEquals(100.0, TrackSimilarity.similarityScore(points, points), 0.01)
    }

    @Test
    fun testTrackWithGpsNoiseIsSameCourse() {
        val points = getTrackPoints("track.gpx")
        val score = TrackSimilarity.similarityScore(points, jitter(points, 10.0))
        Assert.assertTrue("score was $score", TrackSimilarity.isSameCourse(score))
    }

    @Test
    fun testReversedTrackIsSameCourse() {
        val points = getTrackPoints("track.gpx")
        val score = TrackSimilarity.similarityScore(points, points.reversed())
        Assert.assertTrue("score was $score", TrackSimilarity.isSameCourse(score))
    }

    @Test
    fun testShiftedTrackIsDifferentCourse() {
        val points = getTrackPoints("track.gpx")
        val shifted = points.mapIndexed { index, point ->
            trackPoint(point.latitude + 0.01, point.longitude, point.elevation, index)
        }
        val score = TrackSimilarity.similarityScore(points, shifted)
        Assert.assertFalse("score was $score", TrackSimilarity.isSameCourse(score))
    }

    @Test
    fun testHalfOfATrackIsNotTheSameCourse() {
        val points = getTrackPoints("track.gpx")
        val score = TrackSimilarity.similarityScore(points, points.take(points.size / 2))
        Assert.assertFalse("score was $score", TrackSimilarity.isSameCourse(score))
        Assert.assertEquals(50.0, score, 15.0)
    }

    @Test
    fun testSimilarButDifferentRoadsIsNotSameCourse() {
        val piusheim = getTrackPoints("2026-10-04_Piusheim.gpx")
        val kleinhelfendorf = getTrackPoints("2026-10-02_Kleinhelfendorf.gpx")
        Assert.assertTrue(piusheim.size >= 2)
        Assert.assertTrue(kleinhelfendorf.size >= 2)
        val start = System.currentTimeMillis()
        val score = TrackSimilarity.similarityScore(piusheim, kleinhelfendorf)
        val durationMs = System.currentTimeMillis() - start
        println(
            "Similarity score for 2026-10-04_Piusheim.gpx (${piusheim.size} track points) vs " +
                    "2026-10-02_Kleinhelfendorf.gpx (${kleinhelfendorf.size} track points): " +
                    "$score, calculation took $durationMs ms"
        )
        Assert.assertFalse("score was $score", TrackSimilarity.isSameCourse(score))
        Assert.assertTrue("score was $score", score > 0.0)
    }

    @Test
    fun testSameCourseRiddenAgainIsSameCourse() {
        val october02 = getTrackPoints("2026-10-02_Kleinhelfendorf.gpx")
        val october07 = getTrackPoints("2026-10-07_Kleinhelfendorf.gpx")
        Assert.assertTrue(october02.size >= 2)
        Assert.assertTrue(october07.size >= 2)
        val start = System.currentTimeMillis()
        val score = TrackSimilarity.similarityScore(october02, october07)
        val durationMs = System.currentTimeMillis() - start
        println(
            "Similarity score for 2026-10-02_Kleinhelfendorf.gpx (${october02.size} track points) vs " +
                    "2026-10-07_Kleinhelfendorf.gpx (${october07.size} track points): " +
                    "$score, calculation took $durationMs ms"
        )
        Assert.assertTrue("score was $score", TrackSimilarity.isSameCourse(score))
    }

    @Test
    fun testIsPotentialSameCourse() {
        val ruhpoldingBox = TrackBoundingBox(
            latNorth = 47.6, latSouth = 47.4, lonWest = 11.4, lonEast = 11.6
        )
        val existingEntry = summitWithBoundingBox(
            activityId = 1000, kilometers = 40.0, boundingBox = ruhpoldingBox
        )
        val newEntry = summitWithBoundingBox(
            activityId = 2000, kilometers = 40.0,
            boundingBox = TrackBoundingBox(
                latNorth = 47.5, latSouth = 47.3, lonWest = 11.5, lonEast = 11.7
            )
        )
        Assert.assertTrue(TrackSimilarity.isPotentialSameCourse(newEntry, existingEntry))

        val entryWithToleratedLengthDifference = summitWithBoundingBox(
            activityId = 1000, kilometers = 44.0, boundingBox = ruhpoldingBox
        )
        Assert.assertTrue(
            TrackSimilarity.isPotentialSameCourse(newEntry, entryWithToleratedLengthDifference)
        )

        val entryWithDifferentLength = summitWithBoundingBox(
            activityId = 1000, kilometers = 60.0, boundingBox = ruhpoldingBox
        )
        Assert.assertFalse(
            TrackSimilarity.isPotentialSameCourse(newEntry, entryWithDifferentLength)
        )

        val entrySomewhereElse = summitWithBoundingBox(
            activityId = 3000, kilometers = 40.0,
            boundingBox = TrackBoundingBox(
                latNorth = 48.0, latSouth = 47.9, lonWest = 11.4, lonEast = 11.6
            )
        )
        Assert.assertFalse(TrackSimilarity.isPotentialSameCourse(newEntry, entrySomewhereElse))

        val hiker = Summit(
            Summit.parseDate("2024-05-01"), "entry", SportType.Hike,
            kilometers = 40.0, activityId = 1000, hasTrack = true
        )
        hiker.trackBoundingBox = ruhpoldingBox
        Assert.assertFalse(TrackSimilarity.isPotentialSameCourse(newEntry, hiker))

        val entryWithoutBoundingBox = Summit(
            Summit.parseDate("2024-05-01"), "entry", SportType.Bicycle,
            kilometers = 40.0, activityId = 1000, hasTrack = true
        )
        Assert.assertFalse(
            TrackSimilarity.isPotentialSameCourse(newEntry, entryWithoutBoundingBox)
        )

        val entryWithoutTrack = summitWithBoundingBox(
            activityId = 1000, kilometers = 40.0, boundingBox = ruhpoldingBox
        )
        entryWithoutTrack.hasTrack = false
        Assert.assertFalse(TrackSimilarity.isPotentialSameCourse(newEntry, entryWithoutTrack))
    }

    @Test
    fun testRankSimilarSummits() {
        val loop = listOf(
            47.50 to 11.50, 47.51 to 11.50, 47.51 to 11.51, 47.50 to 11.51, 47.50 to 11.50
        )
        val summit = Summit(
            Summit.parseDate("2024-06-01"), "Egmating", SportType.Bicycle,
            kilometers = 3.7, activityId = 3000, hasTrack = true
        )
        writeGpxFile(summit, loop)

        val jitteredLoop = loop.mapIndexed { index, (lat, lon) ->
            (lat + (index % 3 - 1) * 0.0001) to (lon + (index % 2) * 0.0001)
        }
        val sameCourse = Summit(
            Summit.parseDate("2024-07-01"), "Egmating", SportType.Bicycle,
            kilometers = 3.7, activityId = 1000, hasTrack = true
        )
        writeGpxFile(sameCourse, jitteredLoop)

        // Overlapping bounding box, but every point is about 700 m off the
        // original course, so it gets scored but is not the same course.
        val shiftedLoop = loop.map { (lat, lon) -> (lat + 0.005) to (lon + 0.005) }
        val differentCourse = Summit(
            Summit.parseDate("2024-08-01"), "München", SportType.Bicycle,
            kilometers = 3.7, activityId = 2000, hasTrack = true
        )
        writeGpxFile(differentCourse, shiftedLoop)

        val withoutTrack = Summit(
            Summit.parseDate("2024-09-01"), "Kein Track", SportType.Bicycle,
            kilometers = 3.7, activityId = 4000
        )

        // The pre-filter needs bounding boxes, as for entries from the DB
        listOf(summit, sameCourse, differentCourse).forEach { it.setBoundingBoxFromTrack() }

        val ranked = TrackSimilarity.rankSimilarSummits(
            summit, listOf(summit, sameCourse, differentCourse, withoutTrack)
        )

        // The summit itself and the entry without a track are never ranked
        Assert.assertEquals(listOf(1000L, 2000L), ranked.map { it.first.activityId })
        val sameCourseScore = ranked.first { it.first.activityId == 1000L }.second
        val differentCourseScore = ranked.first { it.first.activityId == 2000L }.second
        Assert.assertTrue("score was $sameCourseScore", TrackSimilarity.isSameCourse(sameCourseScore))
        Assert.assertTrue(
            "expected $differentCourseScore < $sameCourseScore",
            differentCourseScore < sameCourseScore
        )
    }

    @Test
    fun testRankSimilarSummitsWithFixtures() {
        val summit = summitWithFixtureTrack(3000, "2026-10-02_Kleinhelfendorf.gpx")
        val sameCourse = summitWithFixtureTrack(1000, "2026-10-07_Kleinhelfendorf.gpx")
        val differentCourse = summitWithFixtureTrack(2000, "2026-10-04_Piusheim.gpx")

        val ranked = TrackSimilarity.rankSimilarSummits(
            summit, listOf(sameCourse, differentCourse)
        )

        Assert.assertTrue(ranked.isNotEmpty())
        Assert.assertEquals(1000L, ranked.first().first.activityId)
        Assert.assertTrue(
            "score was ${ranked.first().second}",
            TrackSimilarity.isSameCourse(ranked.first().second)
        )
        Assert.assertTrue(
            ranked.map { it.second }.zipWithNext().all { it.first >= it.second }
        )
        ranked.filter { it.first.activityId == 2000L }.forEach {
            Assert.assertFalse(
                "score was ${it.second}",
                TrackSimilarity.isSameCourse(it.second)
            )
        }
    }

    @Test
    fun testRankSimilarSummitsFallsBackFromUnparsableSimplifiedTrack() {
        val loop = listOf(
            47.50 to 11.50, 47.51 to 11.50, 47.51 to 11.51, 47.50 to 11.51, 47.50 to 11.50
        )
        val summit = Summit(
            Summit.parseDate("2024-06-01"), "Egmating", SportType.Bicycle,
            kilometers = 3.7, activityId = 3000, hasTrack = true
        )
        writeGpxFile(summit, loop)
        summit.setBoundingBoxFromTrack()

        val jitteredLoop = loop.mapIndexed { index, (lat, lon) ->
            (lat + (index % 3 - 1) * 0.0001) to (lon + (index % 2) * 0.0001)
        }
        val sameCourse = Summit(
            Summit.parseDate("2024-07-01"), "Egmating", SportType.Bicycle,
            kilometers = 3.7, activityId = 1000, hasTrack = true
        )
        writeGpxFile(sameCourse, jitteredLoop)

        // A simplified file that exists but cannot be parsed (e.g. partially
        // written when the app was killed) must not exclude the activity
        // from the ranking: the full track is used instead, and even the
        // bounding box calculation falls back.
        val simplifiedFile = sameCourse.getGpsTrackPath(simplified = true).toFile()
        simplifiedFile.parentFile?.mkdirs()
        simplifiedFile.writeText("")
        sameCourse.setBoundingBoxFromTrack()
        Assert.assertNotNull(sameCourse.trackBoundingBox)

        val ranked = TrackSimilarity.rankSimilarSummits(summit, listOf(sameCourse))

        Assert.assertEquals(listOf(1000L), ranked.map { it.first.activityId })
        Assert.assertTrue(
            "score was ${ranked.first().second}",
            TrackSimilarity.isSameCourse(ranked.first().second)
        )
    }

    @Test
    fun testApplyCourseInfoFromMatch() {
        val newEntry = Summit(
            Summit.parseDate("2024-06-01"), "München Fahrt", SportType.Bicycle,
            activityId = 2000
        )
        val existingEntry = Summit(
            Summit.parseDate("2023-05-01"), "Hausstrecke", SportType.Bicycle,
            places = listOf("Ruhpolding"), countries = listOf("Deutschland"),
            kilometers = 3.7, activityId = 1000, isPeak = true
        )
        TrackSimilarity.applyCourseInfoFromMatch(newEntry, Match(existingEntry, 97.0))
        Assert.assertEquals("Hausstrecke", newEntry.name)
        Assert.assertEquals(listOf("Ruhpolding"), newEntry.places)
        Assert.assertEquals(listOf("Deutschland"), newEntry.countries)
        Assert.assertTrue(newEntry.isPeak)
    }

    @Test
    fun testAdoptInfoFromSimilarEntries() {
        val loop = listOf(
            47.50 to 11.50, 47.51 to 11.50, 47.51 to 11.51, 47.50 to 11.51, 47.50 to 11.50
        )
        val existingEntry = Summit(
            Summit.parseDate("2023-05-01"), "Hausstrecke", SportType.Bicycle,
            places = listOf("Ruhpolding"), countries = listOf("Deutschland"),
            kilometers = 3.7, activityId = 1000, hasTrack = true, isPeak = true
        )
        existingEntry.id = 1
        existingEntry.trackBoundingBox = TrackBoundingBox(
            latNorth = 47.51, latSouth = 47.50, lonWest = 11.50, lonEast = 11.51
        )
        writeGpxFile(existingEntry, loop)
        val newEntry = Summit(
            Summit.parseDate("2024-06-01"), "München Fahrt", SportType.Bicycle,
            kilometers = 3.7, activityId = 2000
        )
        val jitteredLoop = loop.mapIndexed { index, (lat, lon) ->
            (lat + (index % 3 - 1) * 0.0001) to (lon + (index % 2) * 0.0001)
        }
        writeGpxFile(newEntry, jitteredLoop)

        TrackSimilarity.adoptInfoFromSimilarEntries(listOf(newEntry), listOf(existingEntry))

        Assert.assertEquals("Hausstrecke", newEntry.name)
        Assert.assertEquals(listOf("Ruhpolding"), newEntry.places)
        Assert.assertEquals(listOf("Deutschland"), newEntry.countries)
        Assert.assertTrue(newEntry.isPeak)
    }

    @Test
    fun testNoAdoptionForDifferentCourse() {
        val loop = listOf(
            47.50 to 11.50, 47.51 to 11.50, 47.51 to 11.51, 47.50 to 11.51, 47.50 to 11.50
        )
        val existingEntry = Summit(
            Summit.parseDate("2023-05-01"), "Hausstrecke", SportType.Bicycle,
            places = listOf("Ruhpolding"), countries = listOf("Deutschland"),
            kilometers = 3.7, activityId = 1000, hasTrack = true
        )
        existingEntry.id = 1
        existingEntry.trackBoundingBox = TrackBoundingBox(
            latNorth = 47.51, latSouth = 47.50, lonWest = 11.50, lonEast = 11.51
        )
        writeGpxFile(existingEntry, loop)
        val newEntry = Summit(
            Summit.parseDate("2024-06-01"), "München Fahrt", SportType.Bicycle,
            kilometers = 3.7, activityId = 2000
        )
        val otherLoop = listOf(
            48.50 to 13.50, 48.51 to 13.50, 48.51 to 13.51, 48.50 to 13.51, 48.50 to 13.50
        )
        writeGpxFile(newEntry, otherLoop)

        TrackSimilarity.adoptInfoFromSimilarEntries(listOf(newEntry), listOf(existingEntry))

        Assert.assertEquals("München Fahrt", newEntry.name)
        Assert.assertTrue(newEntry.places.isEmpty())
        Assert.assertTrue(newEntry.countries.isEmpty())
    }

    private fun summitWithBoundingBox(
        activityId: Long,
        kilometers: Double,
        boundingBox: TrackBoundingBox
    ): Summit {
        val summit = Summit(
            Summit.parseDate("2024-05-01"), "entry", SportType.Bicycle,
            kilometers = kilometers, activityId = activityId, hasTrack = true
        )
        summit.trackBoundingBox = boundingBox
        return summit
    }

    private fun summitWithFixtureTrack(activityId: Long, resource: String): Summit {
        val summit = Summit(
            Summit.parseDate("2026-10-02"), "entry", SportType.Bicycle,
            kilometers = 60.0, activityId = activityId, hasTrack = true
        )
        val resourceUrl = javaClass.classLoader?.getResource(resource)
            ?: throw IllegalStateException("Missing test resource $resource")
        val file = summit.getGpsTrackPath().toFile()
        file.parentFile?.mkdirs()
        File(resourceUrl.path).copyTo(file, overwrite = true)
        summit.setBoundingBoxFromTrack()
        return summit
    }

    private fun writeGpxFile(summit: Summit, points: List<Pair<Double, Double>>) {
        val xml = StringBuilder()
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        xml.append("<gpx version=\"1.1\" creator=\"test\" ")
        xml.append("xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        xml.append("<trk><name>test</name><trkseg>\n")
        points.forEachIndexed { index, (lat, lon) ->
            xml.append("<trkpt lat=\"")
                .append(lat)
                .append("\" lon=\"")
                .append(lon)
                .append("\"><ele>600</ele><time>2024-01-01T08:00:")
                .append(String.format("%02d", index % 60))
                .append("Z</time></trkpt>\n")
        }
        xml.append("</trkseg></trk>\n")
        xml.append("</gpx>\n")
        val file = summit.getGpsTrackPath().toFile()
        file.parentFile?.mkdirs()
        file.writeText(xml.toString())
    }

    private fun jitter(trackPoints: List<TrackPoint>, jitterMeters: Double): List<TrackPoint> {
        return trackPoints.mapIndexed { index, point ->
            val latitude = point.latitude +
                    ((index % 5) - 2) * jitterMeters / 111320.0
            val longitude = point.longitude +
                    ((index % 3) - 1) * jitterMeters /
                    (111320.0 * cos(Math.toRadians(point.latitude)))
            trackPoint(latitude, longitude, point.elevation, index)
        }
    }

    private fun trackPoint(
        latitude: Double,
        longitude: Double,
        elevation: Double?,
        index: Int
    ): TrackPoint {
        return TrackPoint.Builder()
            .setLatitude(latitude)
            .setLongitude(longitude)
            .setElevation(elevation ?: 0.0)
            .setTime(DateTime(1505900660000L + index * 1000))
            .build() as TrackPoint
    }

    private fun getTrackPoints(resource: String): List<TrackPoint> {
        val resourceUrl = javaClass.classLoader?.getResource(resource) ?: return emptyList()
        val track = GpsTrack(File(resourceUrl.path).toPath(), File("").toPath())
        track.parseTrack(false)
        return track.trackPoints.map { it.first }
    }
}

package de.drtobiasprinz.summitbook.data.model

import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.data.appstate.AppState
import de.drtobiasprinz.summitbook.data.db.entities.SportType
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
class TrackIntervalDataTest {

    private val intervalJson = """
        {
          "km_times": [950.2, 981.0, 902.5],
          "best_efforts_km": {
            "1": {"time": 512.3, "start_distance": 3200.0, "end_distance": 4200.0},
            "2": {"time": 1040.0, "start_distance": 3000.0, "end_distance": 5000.0},
            "5": {"time": 2650.0, "start_distance": 0.0, "end_distance": 5000.0},
            "10": {"time": 5300.0, "start_distance": 0.0, "end_distance": 10000.0}
          },
          "ascents": [
            {"start_distance": 0.0, "end_distance": 1200.0, "start_elevation": 500.0,
             "end_elevation": 560.0, "duration": 300.0, "power": 320, "hr": 160},
            {"start_distance": 4000.0, "end_distance": 5500.0, "start_elevation": 600.0,
             "end_elevation": 780.0, "duration": 612.4, "power": 285, "hr": 172}
          ],
          "descents": [
            {"start_distance": 6000.0, "end_distance": 7200.0, "start_elevation": 560.0,
             "end_elevation": 500.0, "duration": 280.0, "power": null, "hr": null}
          ],
          "best_power_min": {"1": 320.0, "10": 261.0},
          "best_power_km": {"1": 350.0, "5": 280.0},
          "best_hr_min": {"1": 178.0},
          "best_hr_km": {"1": 172.0, "5": 165.0},
          "best_distance_min": {"1": 550.5, "10": 5400.2},
          "power_per_km": [210.0, 220.0, 205.0],
          "hr_per_km": [150.0, 160.0, 155.0],
          "interval_data_version": 2
        }
    """

    private fun parse(json: String) =
        TrackIntervalData.fromJson(JsonParser.parseString(json).asJsonObject)

    @Test
    fun testParseIntervalData() {
        val data = parse(intervalJson)!!

        Assert.assertEquals(listOf(950.2, 981.0, 902.5), data.kmTimes)
        Assert.assertEquals(setOf(1, 2, 5, 10), data.bestEfforts.keys)
        val best2km = data.bestEfforts[2]!!
        Assert.assertEquals(1040.0, best2km.timeSeconds, 0.001)
        Assert.assertEquals(3000.0, best2km.startDistanceMeters, 0.001)
        Assert.assertEquals(5000.0, best2km.endDistanceMeters, 0.001)

        Assert.assertEquals(2, data.ascents.size)
        val firstAscent = data.ascents.first()
        Assert.assertEquals(1200.0, firstAscent.endDistanceMeters, 0.001)
        Assert.assertEquals(60.0, firstAscent.elevationDelta, 0.001)
        Assert.assertEquals(600.0, firstAscent.midpointDistanceMeters, 0.001)
        Assert.assertEquals(320.0, firstAscent.power!!, 0.001)
        Assert.assertEquals(160.0, firstAscent.hr!!, 0.001)
        Assert.assertEquals(1, data.descents.size)
        Assert.assertEquals(280.0, data.descents.first().durationSeconds, 0.001)
        // null section values mean the track has no power/heart rate data
        Assert.assertNull(data.descents.first().power)
        Assert.assertNull(data.descents.first().hr)

        Assert.assertEquals(mapOf(1 to 320.0, 10 to 261.0), data.bestPowerMin)
        Assert.assertEquals(mapOf(1 to 350.0, 5 to 280.0), data.bestPowerKm)
        Assert.assertEquals(mapOf(1 to 178.0), data.bestHrMin)
        Assert.assertEquals(mapOf(1 to 172.0, 5 to 165.0), data.bestHrKm)
        Assert.assertEquals(mapOf(1 to 550.5, 10 to 5400.2), data.bestDistanceMin)
        Assert.assertEquals(listOf(210.0, 220.0, 205.0), data.powerPerKm)
        Assert.assertEquals(listOf(150.0, 160.0, 155.0), data.hrPerKm)
    }

    @Test
    fun testIncompleteJsonIsTreatedAsMissing() {
        Assert.assertNull(parse("""{"km_times": [1.0]}"""))
        Assert.assertNull(parse("""{"km_times": [1.0], "best_efforts_km": {}, "ascents": []}"""))
        Assert.assertNull(parse("""{"duration": 3600.0}"""))
        // written before the power/heart rate windows existed -> backfill
        Assert.assertNull(
            parse(
                """
                {"km_times": [1.0], "best_efforts_km": {}, "ascents": [],
                 "descents": []}
                """.trimIndent()
            )
        )
        // written with an older schema version -> backfill
        Assert.assertNull(
            parse(
                """
                {"km_times": [1.0], "best_efforts_km": {}, "ascents": [],
                 "descents": [], "best_power_min": {}, "best_power_km": {},
                 "best_hr_min": {}, "best_hr_km": {}, "best_distance_min": {},
                 "power_per_km": [], "hr_per_km": [], "interval_data_version": 1}
                """.trimIndent()
            )
        )
    }

    @Test
    fun testReadTrackIntervalDataFromGpxPyJsonFile() {
        AppState.storage = Files.createTempDirectory("summitbook_test").toFile()
        val summit = Summit(
            Summit.parseDate("2026-10-07"), "Kleinhelfendorf", SportType.Bicycle,
            kilometers = 42.0, activityId = 1746615937985L, hasTrack = true
        )

        // No file yet -> null, so callers know the backfill has to run
        Assert.assertNull(readTrackIntervalData(summit))

        val gpxPyFile = summit.getGpxPyPath().toFile()
        gpxPyFile.parentFile?.mkdirs()
        gpxPyFile.writeText(intervalJson)

        val data = readTrackIntervalData(summit)!!
        Assert.assertEquals(3, data.kmTimes.size)
        Assert.assertEquals(2, data.ascents.size)

        // A file written before the interval computation existed is also null
        gpxPyFile.writeText("""{"duration": 3600.0, "elevation_gain": 500.0}""")
        Assert.assertNull(readTrackIntervalData(summit))
    }

    @Test
    fun testSplitTimesForRange() {
        val kmTimes = listOf(100.0, 90.0, 110.0, 95.0, 105.0)

        Assert.assertEquals(listOf(100.0, 90.0, 110.0, 95.0, 105.0), splitTimesForRange(kmTimes, 1))
        Assert.assertEquals(listOf(190.0, 205.0), splitTimesForRange(kmTimes, 2))
        // the trailing 2 km do not span the full 3 km range and are dropped
        Assert.assertEquals(listOf(300.0), splitTimesForRange(kmTimes, 3))
        Assert.assertEquals(listOf(500.0), splitTimesForRange(kmTimes, 5))
        Assert.assertTrue(splitTimesForRange(kmTimes, 6).isEmpty())
    }

    @Test
    fun testMatchElevationInterval() {
        val interval = ElevationInterval(0.0, 1200.0, 500.0, 560.0, 300.0)
        val others = listOf(
            ElevationInterval(5000.0, 6400.0, 700.0, 810.0, 400.0),
            ElevationInterval(50.0, 1250.0, 495.0, 562.0, 290.0)
        )

        val matched = matchElevationInterval(interval, others)!!
        Assert.assertEquals(290.0, matched.durationSeconds, 0.001)

        // closer than the tolerance but nothing within 500 m -> no match
        val farAway = listOf(ElevationInterval(5000.0, 6400.0, 700.0, 810.0, 400.0))
        Assert.assertNull(matchElevationInterval(interval, farAway))
        Assert.assertNull(matchElevationInterval(interval, emptyList()))
    }

    @Test
    fun testFastestMatchedDuration() {
        val interval = ElevationInterval(0.0, 1200.0, 500.0, 560.0, 300.0)
        val others = listOf(
            ElevationInterval(50.0, 1250.0, 495.0, 562.0, 290.0),
            ElevationInterval(80.0, 1160.0, 498.0, 558.0, 275.0)
        )

        // the fastest of the matched same-course sections
        Assert.assertEquals(275.0, fastestMatchedDuration(interval, others)!!, 0.001)
        // nothing matches -> null
        Assert.assertNull(
            fastestMatchedDuration(
                interval,
                listOf(ElevationInterval(5000.0, 6400.0, 700.0, 810.0, 400.0))
            )
        )
        Assert.assertNull(fastestMatchedDuration(interval, emptyList()))
    }

    @Test
    fun testBestMatchedValue() {
        val interval = ElevationInterval(0.0, 1200.0, 500.0, 560.0, 300.0, power = 320.0)
        val others = listOf(
            // matched sections with their average power
            ElevationInterval(50.0, 1250.0, 495.0, 562.0, 290.0, power = 340.0),
            ElevationInterval(80.0, 1160.0, 498.0, 558.0, 275.0, power = 310.0),
            // a section without power data must not break the comparison
            ElevationInterval(60.0, 1230.0, 497.0, 559.0, 295.0),
            // too far away to match
            ElevationInterval(5000.0, 6400.0, 700.0, 810.0, 400.0, power = 500.0)
        )

        // the best average power of the matched same-course sections
        Assert.assertEquals(340.0, bestMatchedValue(interval, others) { it.power }!!, 0.001)
        // a section without heart rate data only -> null
        Assert.assertNull(bestMatchedValue(interval, others) { it.hr })
        Assert.assertNull(bestMatchedValue(interval, emptyList()) { it.power })
    }

    @Test
    fun testFastestSplitTimes() {
        // current ride: 5 km at 100 s, 100 s, 110 s, 90 s, 105 s per km
        val current = listOf(100.0, 100.0, 110.0, 90.0, 105.0)
        val other = TrackIntervalData(
            kmTimes = listOf(95.0, 105.0, 100.0, 80.0, 100.0),
            bestEfforts = emptyMap(),
            ascents = emptyList(),
            descents = emptyList()
        )
        val shorterOther = TrackIntervalData(
            kmTimes = listOf(99.0, 99.0),
            bestEfforts = emptyMap(),
            ascents = emptyList(),
            descents = emptyList()
        )

        // per 1-km interval: min of the others, missing when none covers it
        Assert.assertEquals(
            listOf(95.0, 99.0, 100.0, 80.0, 100.0),
            fastestSplitTimes(current, 1, listOf(other, shorterOther)).map { it!! }
        )

        // per 2-km interval: current [200, 200] vs other [200, 180] -> [200, 180]
        Assert.assertEquals(
            listOf(200.0, 180.0),
            fastestSplitTimes(current, 2, listOf(other)).map { it!! }
        )

        // an interval no other ride reaches stays null
        val fastest = fastestSplitTimes(listOf(100.0, 100.0, 100.0), 1, listOf(shorterOther))
        Assert.assertEquals(99.0, fastest[0]!!, 0.001)
        Assert.assertEquals(99.0, fastest[1]!!, 0.001)
        Assert.assertNull(fastest[2])

        // no others at all -> everything null
        Assert.assertTrue(fastestSplitTimes(current, 1, emptyList()).all { it == null })
    }

    @Test
    fun testSplitValuesForRange() {
        val powerPerKm = listOf(200.0, 400.0, 400.0)
        val kmTimes = listOf(60.0, 30.0, 60.0)

        // 1-km intervals are the per-km values themselves
        Assert.assertEquals(powerPerKm, splitValuesForRange(powerPerKm, kmTimes, 1))
        // 2-km interval: (200*60 + 400*30) / 90 = 266.67; trailing partial dropped
        Assert.assertEquals(
            listOf(266.6666666666667),
            splitValuesForRange(powerPerKm, kmTimes, 2)
        )
        // 3-km interval: (200*60 + 400*30 + 400*60) / 150 = 320
        Assert.assertEquals(listOf(320.0), splitValuesForRange(powerPerKm, kmTimes, 3))
        // no values at all -> empty
        Assert.assertTrue(splitValuesForRange(emptyList(), kmTimes, 1).isEmpty())
    }

    @Test
    fun testBestSplitValues() {
        val other = TrackIntervalData(
            kmTimes = listOf(60.0, 60.0, 60.0),
            bestEfforts = emptyMap(),
            ascents = emptyList(),
            descents = emptyList(),
            powerPerKm = listOf(210.0, 190.0, 205.0)
        )
        val shorterOther = TrackIntervalData(
            kmTimes = listOf(60.0, 60.0),
            bestEfforts = emptyMap(),
            ascents = emptyList(),
            descents = emptyList(),
            powerPerKm = listOf(220.0, 180.0)
        )

        // per 1-km interval: max of the others, null when none covers it
        val best = bestSplitValues(listOf(other, shorterOther)) {
            splitValuesForRange(it.powerPerKm, it.kmTimes, 1)
        }
        Assert.assertEquals(listOf(220.0, 190.0, 205.0), best)

        Assert.assertNull(bestSplitValues(emptyList()) { it.powerPerKm }.firstOrNull())
    }

    @Test
    fun testBestWindowValue() {
        val other = TrackIntervalData(
            kmTimes = emptyList(),
            bestEfforts = emptyMap(),
            ascents = emptyList(),
            descents = emptyList(),
            bestPowerKm = mapOf(1 to 350.0, 5 to 280.0)
        )
        val another = TrackIntervalData(
            kmTimes = emptyList(),
            bestEfforts = emptyMap(),
            ascents = emptyList(),
            descents = emptyList(),
            bestPowerKm = mapOf(1 to 340.0)
        )

        Assert.assertEquals(
            350.0,
            bestWindowValue(listOf(other, another), 1) { it.bestPowerKm }!!,
            0.001
        )
        Assert.assertEquals(
            280.0,
            bestWindowValue(listOf(other, another), 5) { it.bestPowerKm }!!,
            0.001
        )
        // no other ride has a value for that window
        Assert.assertNull(bestWindowValue(listOf(other, another), 10) { it.bestPowerKm })
        Assert.assertNull(bestWindowValue(emptyList(), 1) { it.bestPowerKm })
    }
}

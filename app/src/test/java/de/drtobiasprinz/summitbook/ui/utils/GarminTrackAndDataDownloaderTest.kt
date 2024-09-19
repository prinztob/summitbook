package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.db.entities.ElevationData
import de.drtobiasprinz.summitbook.db.entities.GarminData
import de.drtobiasprinz.summitbook.db.entities.PowerData
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.VelocityData
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import org.junit.Test

class GarminTrackAndDataDownloaderTest {
    @Test
    fun testExtractFinalSummitWithoutGarmin() {
        val entry1 = Summit(
            Summit.parseDate("2019-11-13"), "summit1", SportType.Bicycle,
            listOf("place1"), listOf("country1"), "comment1",
            ElevationData.Companion.parse(11, 1), 1.0,
            VelocityData.Companion.parse(1.0, 11.3),
            participants = mutableListOf("participant1"), activityId = 123L
        )
        val entry2 = Summit(
            Summit.parseDate("2019-11-13"), "summit2", SportType.Bicycle,
            listOf("place2"), listOf("country2"), "comment2",
            ElevationData.Companion.parse(110, 10), 10.0,
            VelocityData.Companion.parse(5.0, 12.3),
            participants = mutableListOf("participant2")
        )
        val entry3 = Summit(
            Summit.parseDate("2019-11-13"), "summit3", SportType.Bicycle,
            listOf("place3"), listOf("country3"), "comment3",
            ElevationData.Companion.parse(1100, 100), 70.0,
            VelocityData.Companion.parse(10.0, 10.3),
            participants = mutableListOf("participant4")
        )
        val finalEntryExpected = Summit(
            Summit.parseDate("2019-11-13"),
            "summit1",
            SportType.Bicycle,
            listOf("place1", "place2", "place3"),
            listOf("country1", "country2", "country3"),
            "merge of summit1, summit2, summit3",
            ElevationData.Companion.parse(1100, 111),
            81.0,
            VelocityData.Companion.parse(8.1, 12.3),
            participants = mutableListOf("participant1", "participant2", "participant4"),
            activityId = entry1.activityId
        )
        val downloader = GarminTrackAndDataDownloader(
            listOf(entry1, entry2, entry3),
            GarminPythonExecutor("aaa", "bbbb")
        )
        downloader.extractFinalSummit()
        assert(downloader.finalEntry?.equals(finalEntryExpected) == true)
    }

    @Test
    fun testExtractFinalSummitWithOneGarminActivity() {
        val garminData = GarminData(
            mutableListOf("1"), 1f, 2f, 3f,
            PowerData(4f, 5f, 6f, 100, 99, 98, 97, 96, 96, 95, 94, 93, 92, 91, 90, 89, 88, 87),
            4, 5f, 3.1f, 3.2f, 6f, 7f, 8f
        )
        val entry1 = Summit(
            Summit.parseDate("2019-11-13"),
            "summit1",
            SportType.Bicycle,
            listOf("place1"),
            listOf("country1"),
            "comment1",
            ElevationData.Companion.parse(11, 1),
            1.0,
            VelocityData.Companion.parse(1.0, 11.3),
            participants = mutableListOf("participant1"),
        )
        val entry2 = Summit(
            Summit.parseDate("2019-11-13"),
            "summit2",
            SportType.Bicycle,
            listOf("place2"),
            listOf("country2"),
            "comment2",
            ElevationData.Companion.parse(110, 11),
            10.0,
            VelocityData.Companion.parse(5.0, 12.3),
            participants = mutableListOf("participant2"),
        )
        val entry3 = Summit(
            Summit.parseDate("2019-11-13"),
            "summit3",
            SportType.Bicycle,
            listOf("place3"),
            listOf("country3"),
            "comment3",
            ElevationData.Companion.parse(1100, 110),
            70.0,
            VelocityData.Companion.parse(10.0, 10.3),
            participants = mutableListOf("participant4"),
        )
        entry3.garminData = garminData
        val downloader = GarminTrackAndDataDownloader(
            listOf(entry1, entry2, entry3),
            GarminPythonExecutor("aaa", "bbbb")
        )
        downloader.extractFinalSummit()
        assert(downloader.finalEntry?.garminData?.toString() == garminData.toString())
    }

    @Test
    fun testExtractFinalSummitWithSeveralGarminActivities() {
        val garminDataExpected = GarminData(
            mutableListOf("1", "2", "3"), 123f, 51.1f, 133f,
            PowerData(
                51.1f,
                155f,
                66f,
                101,
                101,
                101,
                97,
                96,
                96,
                95,
                94,
                93,
                92,
                91,
                90,
                11,
                10,
                0
            ),
            46, 55f, 3.1f, 3.2f, 6f, 7f, 8f
        )
        val entry1 = Summit(
            Summit.parseDate("2019-11-13"),
            "summit1",
            SportType.Bicycle,
            listOf("place1"),
            listOf("country1"),
            "comment1",
            ElevationData.Companion.parse(1, 11),
            1.0,
            VelocityData.Companion.parse(1.0, 11.3),
            participants = mutableListOf("participant1"),
        )
        entry1.garminData = GarminData(
            mutableListOf("1"), 1f, 1f, 33f,
            PowerData(1f, 5f, 66f, 101, 99, 98, 97, 96, 96, 95, 94, 93, 92, 91, 90, 11, 8, 0),
            4, 55f, 1.1f, 3.2f, 3f, 3f, 3f
        )
        val entry2 = Summit(
            Summit.parseDate("2019-11-13"),
            "summit2",
            SportType.Bicycle,
            listOf("place2"),
            listOf("country2"),
            "comment2",
            ElevationData.Companion.parse(10, 110),
            10.0,
            VelocityData.Companion.parse(5.0, 12.3),
            participants = mutableListOf("participant2"),
        )
        entry2.garminData = GarminData(
            mutableListOf("2"), 11f, 10f, 133f,
            PowerData(10f, 55f, 6f, 100, 101, 98, 97, 96, 96, 95, 94, 93, 92, 91, 90, 10, 9, 0),
            45, 45f, 2.1f, 3.1f, 6f, 7f, 3f
        )
        val entry3 = Summit(
            Summit.parseDate("2019-11-13"),
            "summit3",
            SportType.Bicycle,
            listOf("place3"),
            listOf("country3"),
            "comment3",
            ElevationData.Companion.parse(100, 1100),
            70.0,
            VelocityData.Companion.parse(10.0, 10.3),
            participants = mutableListOf("participant4"),
        )
        entry3.garminData = GarminData(
            mutableListOf("3"), 111f, 70f, 103f,
            PowerData(70f, 155f, 6f, 100, 99, 101, 97, 96, 96, 95, 94, 93, 92, 91, 90, 10, 10, 0),
            46, 35f, 3.1f, 3.0f, 2f, 2f, 2f
        )
        val downloader = GarminTrackAndDataDownloader(
            listOf(entry1, entry2, entry3),
            GarminPythonExecutor("aaa", "bbbb")
        )
        downloader.extractFinalSummit()
        assert(downloader.finalEntry?.garminData?.toString() == garminDataExpected.toString())
    }
}
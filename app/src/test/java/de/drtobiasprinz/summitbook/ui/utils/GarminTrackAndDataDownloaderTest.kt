package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.models.GarminActivityData
import de.drtobiasprinz.summitbook.models.PowerData
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import org.junit.Test

class GarminTrackAndDataDownloaderTest {
    @Test
    fun testExtractFinalSummitEntryWithouGarmin() {
        val entry1 = SummitEntry(SummitEntry.parseDate("2019-11-13"), "summit1", SportType.Bicycle, listOf("place1"), listOf("country1"), "comment1", 1, 1.0, 1.0, 11.3, 11, mutableListOf("participant1"), mutableListOf())
        val entry2 = SummitEntry(SummitEntry.parseDate("2019-11-13"), "summit2", SportType.Bicycle, listOf("place2"), listOf("country2"), "comment2", 10, 10.0, 5.0, 12.3, 110, mutableListOf("participant2"), mutableListOf())
        val entry3 = SummitEntry(SummitEntry.parseDate("2019-11-13"), "summit3", SportType.Bicycle, listOf("place3"), listOf("country3"), "comment3", 100, 70.0, 10.0, 10.3, 1100, mutableListOf("participant4"), mutableListOf())
        val finalEntryExpected = SummitEntry(SummitEntry.parseDate("2019-11-13"), "summit1", SportType.Bicycle, listOf("place1"), listOf("country1"), "merge of summit1, summit2, summit3", 111, 81.0, 8.1, 12.3, 1100, mutableListOf("participant1"), mutableListOf())
        val downloader = GarminTrackAndDataDownloader(listOf(entry1, entry2, entry3), GarminPythonExecutor("aaa", "bbbb"))
        downloader.extractFinalSummitEntry()
        assert(downloader.finalEntry?.equalsInAllProperties(finalEntryExpected) == true)
    }

    @Test
    fun testExtractFinalSummitEntryWithOneGarminActivity() {
        val activityData = GarminActivityData(mutableListOf("1"), 1f, 2f, 3f,
                PowerData(4f, 5f, 6f, 100, 99, 98, 97, 96, 96, 95, 94, 93, 92, 91, 90, 89, 88, 87),
                4, 5, 3.1f, 3.2f, 6f, 7f, 8f
        )
        val entry1 = SummitEntry(SummitEntry.parseDate("2019-11-13"), "summit1", SportType.Bicycle, listOf("place1"), listOf("country1"), "comment1", 1, 1.0, 1.0, 11.3, 11, mutableListOf("participant1"), mutableListOf())
        val entry2 = SummitEntry(SummitEntry.parseDate("2019-11-13"), "summit2", SportType.Bicycle, listOf("place2"), listOf("country2"), "comment2", 10, 10.0, 5.0, 12.3, 110, mutableListOf("participant2"), mutableListOf())
        val entry3 = SummitEntry(SummitEntry.parseDate("2019-11-13"), "summit3", SportType.Bicycle, listOf("place3"), listOf("country3"), "comment3", 100, 70.0, 10.0, 10.3, 1100, mutableListOf("participant4"), mutableListOf())
        entry3.activityData = activityData
        val downloader = GarminTrackAndDataDownloader(listOf(entry1, entry2, entry3), GarminPythonExecutor("aaa", "bbbb"))
        downloader.extractFinalSummitEntry()
        assert(downloader.finalEntry?.activityData?.toString() == activityData.toString())
    }

    @Test
    fun testExtractFinalSummitEntryWithSeveralGarminActivities() {
        val activityDataExpected = GarminActivityData(mutableListOf("1", "2", "3"), 123f, 51.1f, 133f,
                PowerData(51.1f, 155f, 66f, 101, 101, 101, 97, 96, 96, 95, 94, 93, 92, 91, 90, 11, 10, 0),
                46, 55, 3.1f, 3.2f, 6f, 7f, 8f
        )
        val entry1 = SummitEntry(SummitEntry.parseDate("2019-11-13"), "summit1", SportType.Bicycle, listOf("place1"), listOf("country1"), "comment1", 1, 1.0, 1.0, 11.3, 11, mutableListOf("participant1"), mutableListOf())
        entry1.activityData = GarminActivityData(mutableListOf("1"), 1f, 1f, 33f,
                PowerData(1f, 5f, 66f, 101, 99, 98, 97, 96, 96, 95, 94, 93, 92, 91, 90, 11, 8, 0),
                4, 55, 1.1f, 3.2f, 3f, 3f, 3f
        )
        val entry2 = SummitEntry(SummitEntry.parseDate("2019-11-13"), "summit2", SportType.Bicycle, listOf("place2"), listOf("country2"), "comment2", 10, 10.0, 5.0, 12.3, 110, mutableListOf("participant2"), mutableListOf())
        entry2.activityData = GarminActivityData(mutableListOf("2"), 11f, 10f, 133f,
                PowerData(10f, 55f, 6f, 100, 101, 98, 97, 96, 96, 95, 94, 93, 92, 91, 90, 10, 9, 0),
                45, 45, 2.1f, 3.1f, 6f, 7f, 3f
        )
        val entry3 = SummitEntry(SummitEntry.parseDate("2019-11-13"), "summit3", SportType.Bicycle, listOf("place3"), listOf("country3"), "comment3", 100, 70.0, 10.0, 10.3, 1100, mutableListOf("participant4"), mutableListOf())
        entry3.activityData = GarminActivityData(mutableListOf("3"), 111f, 70f, 103f,
                PowerData(70f, 155f, 6f, 100, 99, 101, 97, 96, 96, 95, 94, 93, 92, 91, 90, 10, 10, 0),
                46, 35, 3.1f, 3.0f, 2f, 2f, 2f
        )
        val downloader = GarminTrackAndDataDownloader(listOf(entry1, entry2, entry3), GarminPythonExecutor("aaa", "bbbb"))
        downloader.extractFinalSummitEntry()
        assert(downloader.finalEntry?.activityData?.toString() == activityDataExpected.toString())
    }
}
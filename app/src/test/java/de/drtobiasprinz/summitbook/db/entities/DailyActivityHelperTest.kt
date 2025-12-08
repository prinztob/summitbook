package de.drtobiasprinz.summitbook.db.entities

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.drtobiasprinz.summitbook.utils.TestUtils
import de.drtobiasprinz.summitbook.utils.ZipFileVersions
import org.junit.Assert
import org.junit.Test

class DailyActivityHelperTest {

    @Test
    fun testFindDailyActivitySummariesWhichWasNotAddedToSummitsWithMatchingSummits() {
        val jsonString = TestUtils.loadTestJson("report.json")
        val gson = Gson()
        val listType = object : TypeToken<List<DailyActivitySummary>>() {}.type
        val dailyActivitySummaries: List<DailyActivitySummary> = gson.fromJson(jsonString, listType)

        val summits = parseSummitsFromCsv()

        val filteredReports =
            DailyActivityHelper.findDailyActivitySummariesWhichWasNotAddedToSummits(
                dailyActivitySummaries,
                summits
            )
        val convertedSummits = DailyActivityHelper.parseAsSummit(filteredReports)

        Assert.assertEquals(50, convertedSummits.size)
        Assert.assertEquals(
            0,
            convertedSummits.filter { it.getDateAsString() == "2025-01-01" }.size
        )
        Assert.assertEquals(
            2,
            convertedSummits.filter { it.getDateAsString() == "2025-01-03" }.size
        )
        Assert.assertEquals(
            1,
            convertedSummits.filter { it.getDateAsString() == "2025-01-04" }.size
        )
        Assert.assertEquals(
            1,
            convertedSummits.filter { it.getDateAsString() == "2025-01-05" }.size
        )
        Assert.assertEquals(
            1,
            convertedSummits.filter { it.getDateAsString() == "2025-01-08" }.size
        )
        Assert.assertEquals(
            0,
            convertedSummits.filter { it.getDateAsString() == "2025-01-10" }.size
        )
    }

    @Test
    fun testFindDailyActivitySummariesWhichWasNotAddedToSummitsWithNoMatchingSummits() {
        val dailyActivitySummaries = getSummaries("2020")
        val summits = parseSummitsFromCsv()
        val filteredReports =
            DailyActivityHelper.findDailyActivitySummariesWhichWasNotAddedToSummits(
                dailyActivitySummaries,
                summits
            )
        Assert.assertEquals(2, filteredReports.size)
    }

    @Test
    fun testFindDailyActivitySummariesWhichWasNotAddedToSummitsWithNoMatchingSummitsInSummitsTimeRange() {
        val dailyActivitySummaries = getSummaries("2025")
        val summits = parseSummitsFromCsv()
        val filteredReports =
            DailyActivityHelper.findDailyActivitySummariesWhichWasNotAddedToSummits(
                dailyActivitySummaries,
                summits
            )
        Assert.assertEquals(2, filteredReports.size)
    }

    private fun getSummaries(year: String): List<DailyActivitySummary> = listOf(
        DailyActivitySummary(
            date = "$year-05-01",
            activityId = 1,
            distance = 1000.0,
            duration = 3600.0,
            elevationGain = 100.0,
            sportType = SportType.Running
        ),
        DailyActivitySummary(
            date = "$year-05-02",
            activityId = 2,
            distance = 2000.0,
            duration = 7200.0,
            elevationGain = 200.0,
            sportType = SportType.Bicycle
        )
    )


    private fun parseSummitsFromCsv(): List<Summit> {
        val csvContent = TestUtils.loadTestJson("de-prinz-summitbook-export.csv")
        val summits = mutableListOf<Summit>()
        val lines = csvContent.split("\n")

        // Skip header lines (first 2 lines)
        for (i in 2 until lines.size) {
            val line = lines[i].trim()
            if (line.isNotEmpty()) {
                try {
                    val summit = Summit.parseFromCsvFileLine(line, "v1")
                    summits.add(summit)
                } catch (_: Exception) {
                    // Skip invalid lines
                }
            }
        }
        val csvContentGarmin = TestUtils.loadTestJson("de-prinz-summitbook-export-third-party-data.csv")
        val linesGarmin = csvContentGarmin.split("\n")
        for (i in 2 until linesGarmin.size) {
            val line = linesGarmin[i].trim()
            if (line.isNotEmpty()) {
                try {
                    GarminData.parseFromCsvFileLineAndSave(line, summits, {a, b, -> }, ZipFileVersions.V1)
                } catch (_: Exception) {
                    // Skip invalid lines
                }
            }
        }

        return summits
    }
}
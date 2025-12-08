package de.drtobiasprinz.summitbook.db.entities

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.drtobiasprinz.summitbook.utils.TestUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyActivitySummaryTest {

    @Test
    fun testParseActivitySummaryJsonHas14UniqueDatesFromNovemberAndDecember() {
        val jsonString = TestUtils.loadTestJson("report.json")
        
        val gson = Gson()
        val listType = object : TypeToken<List<DailyActivitySummary>>() {}.type
        val reports: List<DailyActivitySummary> = gson.fromJson(jsonString, listType)
        
        val novemberDecemberReports = reports.filter {
            it.date.startsWith("2025-11") || it.date.startsWith("2025-12")
        }
        
        val uniqueDates = novemberDecemberReports.map { it.date }.distinct()
        assertEquals(16, uniqueDates.size)
    }

}
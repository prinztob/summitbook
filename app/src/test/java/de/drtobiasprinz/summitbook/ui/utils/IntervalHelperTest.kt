package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.db.entities.ElevationData
import de.drtobiasprinz.summitbook.db.entities.Summit
import org.junit.Assert
import org.junit.Test
import java.text.ParseException
import java.util.*

internal class IntervalHelperTest {

    @Test
    @Throws(ParseException::class)
    fun testGetDates() {
        val helper = IntervalHelper(
            listOf(
                Summit(Summit.parseDate("2019-10-13")),
                Summit(Summit.parseDate("2020-11-13"))
            )
        )
        val weeks =
            helper.getRangeAndAnnotationsForDate(Calendar.WEEK_OF_YEAR, Calendar.DAY_OF_WEEK)
        Assert.assertEquals(57, weeks.second.size)
        weeks.second.forEachIndexed { index, closedRange ->
            if (index > 0) {
                assert(closedRange.start.time - weeks.second[index - 1].endInclusive.time < 2000)
            }
        }
        val months = helper.getRangeAndAnnotationsForDate(Calendar.MONTH, Calendar.DAY_OF_MONTH)
        Assert.assertEquals(14, months.second.size)
        val monthsUntilEndOfCurrentYear =
            helper.getRangeAndAnnotationsForDate(Calendar.MONTH, Calendar.DAY_OF_MONTH, true)
        Assert.assertEquals(15, monthsUntilEndOfCurrentYear.second.size)
        val years = helper.getRangeAndAnnotationsForDate(Calendar.YEAR, Calendar.DAY_OF_YEAR)
        Assert.assertEquals(2, years.second.size)
        Assert.assertTrue(Summit.parseDate("2018-12-31") !in years.second[0])
        Assert.assertTrue(Summit.parseDate("2019-01-01") in years.second[0])
        Assert.assertTrue(Summit.parseDate("2019-12-31") in years.second[0])
        Assert.assertTrue(Summit.parseDate("2020-01-01") !in years.second[0])
    }

    @Test
    @Throws(ParseException::class)
    fun testDatesOverYear() {
        val helper = IntervalHelper(
            listOf(
                Summit(Date(1672570800000)),
                Summit(Date(1681898400000))
            )
        )
        val weeks =
            helper.getRangeAndAnnotationsForDate(Calendar.WEEK_OF_YEAR, Calendar.DAY_OF_WEEK)
        Assert.assertEquals(16, weeks.second.size)
    }

    @Test
    @Throws(ParseException::class)
    fun testQuarterlyRanges() {
        val helper = IntervalHelper(
            listOf(
                Summit(Summit.parseDate("2019-10-13")),
                Summit(Summit.parseDate("2020-11-13"))
            )
        )
        val quarter = helper.getRangeAndAnnotationQuarterly()
        Assert.assertEquals(8, quarter.second.size)
    }

    @Test
    @Throws(ParseException::class)
    fun testGetValues() {
        val helper = IntervalHelper(
            listOf(
                Summit(Summit.parseDate("2019-11-13"), elevationData = ElevationData(1000, 2000)),
                Summit(Summit.parseDate("2020-12-13"), elevationData = ElevationData(3000, 1000))
            )
        )
        val maxElevation =
            helper.getRangeAndAnnotationsForSummitValue(250f) { summit -> summit.elevationData.maxElevation.toFloat() }
        Assert.assertEquals(13, maxElevation.second.size)
        val elevationGain =
            helper.getRangeAndAnnotationsForSummitValue(250f) { summit -> summit.elevationData.elevationGain.toFloat() }
        Assert.assertEquals(9, elevationGain.second.size)
        Assert.assertTrue(250f !in elevationGain.second[0])
        Assert.assertTrue(250f in elevationGain.second[1])
    }

    @Test
    @Throws(ParseException::class)
    fun testGetChipValue() {
        val helper = IntervalHelper(
            listOf(
                Summit(Summit.parseDate("2019-11-13"), participants = listOf("part1", "part2")),
                Summit(Summit.parseDate("2020-12-13"), participants = listOf("part1", "part3"))
            )
        )
        val participants =
            helper.getRangeAndAnnotationForSummitChipValues { summit -> summit.participants }
        Assert.assertEquals(3, participants.second.size)
    }
}
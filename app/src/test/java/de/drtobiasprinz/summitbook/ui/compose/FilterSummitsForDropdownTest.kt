package de.drtobiasprinz.summitbook.ui.compose

import de.drtobiasprinz.summitbook.data.db.entities.SportType
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import org.junit.Assert
import org.junit.Test

class FilterSummitsForDropdownTest {

    @Test
    fun testBlankQueryLimitsToFirstHundredEntries() {
        val summits = (1..120).map { index ->
            summit(activityId = index.toLong(), name = "summit$index", date = "2026-01-01")
        }

        val result = filterSummitsForDropdown(summits, "")

        Assert.assertEquals((1..100).map { it.toLong() }, result.map { it.activityId })
        Assert.assertEquals(120, countSummitsMatchingSearchQuery(summits, ""))
    }

    @Test
    fun testBlankQueryShowsAllWhenBelowLimit() {
        val summits = (1..3).map { index ->
            summit(activityId = index.toLong(), name = "summit$index", date = "2026-01-01")
        }

        val result = filterSummitsForDropdown(summits, "   ")

        Assert.assertEquals(listOf(1L, 2L, 3L), result.map { it.activityId })
    }

    @Test
    fun testSearchMatchesDateAndNameCaseInsensitive() {
        val summits = listOf(
            summit(activityId = 1, name = "Kleinhelfendorf", date = "2026-10-07"),
            summit(activityId = 2, name = "Piusheim", date = "2026-10-04"),
            summit(activityId = 3, name = "Aschbach", date = "2026-03-15")
        )

        Assert.assertEquals(listOf(1L), filterSummitsForDropdown(summits, "Kleinhelfendorf").map { it.activityId })
        Assert.assertEquals(listOf(3L), filterSummitsForDropdown(summits, "aschbach").map { it.activityId })
        Assert.assertEquals(listOf(2L), filterSummitsForDropdown(summits, " 2026-10-04 ").map { it.activityId })
        Assert.assertEquals(
            listOf(1L, 2L),
            filterSummitsForDropdown(summits, "2026-10").map { it.activityId }
        )
        Assert.assertEquals(2, countSummitsMatchingSearchQuery(summits, "2026-10"))
        Assert.assertEquals(0, countSummitsMatchingSearchQuery(summits, "nothing matches"))
    }

    @Test
    fun testSearchCapsAtFiftyAndPreservesOrder() {
        val summits = (1..80).map { index ->
            summit(activityId = index.toLong(), name = "Tour $index", date = "2026-05-01")
        }

        val result = filterSummitsForDropdown(summits, "tour")

        Assert.assertEquals((1..50).map { it.toLong() }, result.map { it.activityId })
        Assert.assertEquals(80, countSummitsMatchingSearchQuery(summits, "tour"))
    }

    private fun summit(activityId: Long, name: String, date: String): Summit {
        val summit = Summit(
            Summit.parseDate(date), name, SportType.Bicycle,
            kilometers = 40.0, activityId = activityId, hasTrack = true
        )
        summit.id = activityId
        return summit
    }
}

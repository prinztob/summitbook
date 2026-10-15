package de.drtobiasprinz.summitbook.ui.activities

import de.drtobiasprinz.summitbook.data.db.entities.SportType
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import org.junit.Assert
import org.junit.Test

class GetSummitsToCompareTest {

    @Test
    fun testSimilarSummitsComeFirstSortedByScoreAndDate() {
        val base = summit(id = 1, activityId = 1000L, name = "base", date = "2026-01-01")
        val highestScore = summit(id = 2, activityId = 2000L, name = "other", date = "2026-02-01")
        val sameScoreNewer = summit(id = 3, activityId = 3000L, name = "x", date = "2026-04-01")
        val sameScoreOlder = summit(id = 4, activityId = 4000L, name = "y", date = "2026-03-01")
        val sameName = summit(id = 5, activityId = 5000L, name = "base", date = "2026-05-01")
        val other = summit(id = 6, activityId = 6000L, name = "z", date = "2026-06-01")
        // 97.4 and 97.8 are displayed as the same score (97), so the newer
        // one (April) is shown before the older one (March)
        val scores = mapOf(
            2000L to 99.4,
            3000L to 97.4,
            4000L to 97.8
        )

        val result = getSummitsToCompare(
            listOf(base, highestScore, sameScoreNewer, sameScoreOlder, sameName, other),
            base,
            scores
        )

        // Same-course summits first (score descending, same displayed score
        // by date descending), then same-name ones, then the rest by date
        Assert.assertEquals(
            listOf(2000L, 4000L, 3000L, 5000L, 6000L),
            result.summits.map { it.activityId }
        )
        Assert.assertEquals(scores, result.similarityScores)
    }

    @Test
    fun testSubThresholdScoresAreIgnored() {
        val base = summit(id = 1, activityId = 1000L, name = "base", date = "2026-01-01")
        val nearMiss = summit(id = 2, activityId = 2000L, name = "other", date = "2026-02-01")

        val result = getSummitsToCompare(
            listOf(base, nearMiss),
            base,
            mapOf(2000L to 90.0)
        )

        Assert.assertEquals(listOf(2000L), result.summits.map { it.activityId })
        Assert.assertTrue(result.similarityScores.isEmpty())
    }

    @Test
    fun testEmptyInput() {
        Assert.assertTrue(getSummitsToCompare(null, null).summits.isEmpty())
        val base = summit(id = 1, activityId = 1000L, name = "base", date = "2026-01-01")
        Assert.assertTrue(getSummitsToCompare(emptyList(), base).summits.isEmpty())
    }

    private fun summit(id: Long, activityId: Long, name: String, date: String): Summit {
        val summit = Summit(
            Summit.parseDate(date), name, SportType.Bicycle,
            kilometers = 40.0, activityId = activityId, hasTrack = true
        )
        summit.id = id
        return summit
    }
}

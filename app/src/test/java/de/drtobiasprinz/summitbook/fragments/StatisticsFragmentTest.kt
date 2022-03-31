package de.drtobiasprinz.summitbook.fragments

import de.drtobiasprinz.summitbook.models.ElevationData
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.models.VelocityData
import org.junit.Assert
import org.junit.Test
import java.text.ParseException

class StatisticsFragmentTest {
    companion object {
        private lateinit var entry1: Summit
        private lateinit var entry2: Summit
        private lateinit var entry3: Summit
        private val summitEntries: ArrayList<Summit> = ArrayList()

        init {
            try {
                entry1 = Summit(Summit.parseDate("2019-11-13"), "summit1", SportType.Bicycle, listOf("place1"), listOf("country1"), "comment1", ElevationData.Companion.parse(1, 11), 1.1, VelocityData.Companion.parse(1.2, 1.3), 0.0,0.0, mutableListOf("participant1"), mutableListOf(), isFavorite = false, isPeak = false, imageIds = mutableListOf(), garminData = null, trackBoundingBox = null)
                entry2 = Summit(Summit.parseDate("2018-11-18"), "summit2", SportType.Bicycle, listOf("place2"), listOf("country2"), "comment2", ElevationData.Companion.parse(2, 22), 2.1, VelocityData.Companion.parse(2.2, 2.3), 0.0,0.0, mutableListOf("participant1"), mutableListOf(), isFavorite = false, isPeak = false, imageIds = mutableListOf(), garminData = null, trackBoundingBox = null)
                entry3 = Summit(Summit.parseDate("2019-10-18"), "summit3", SportType.Bicycle, listOf("place3"), listOf("country3"), "comment3", ElevationData.Companion.parse(3, 33), 3.1, VelocityData.Companion.parse(3.2, 3.3), 0.0,0.0, mutableListOf("participant1"), mutableListOf(), isFavorite = false, isPeak = false, imageIds = mutableListOf(), garminData = null, trackBoundingBox = null)
                summitEntries.add(entry1)
                summitEntries.add(entry2)
                summitEntries.add(entry3)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
        }
    }

    @Test
    fun getAllYearsTest() {
        val expected: ArrayList<String> = object : ArrayList<String>() {
            init {
                add("2018")
                add("2019")
            }
        }
        val actual: ArrayList<String> = StatisticsFragment.getAllYears(summitEntries)
        actual.sort()
        expected.sort()
        Assert.assertEquals(expected, actual)
    }
}
package de.drtobiasprinz.summitbook.fragments

import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.SummitEntry
import org.junit.Assert
import org.junit.Test
import java.text.ParseException

class StatisticsFragmentTest {
    companion object {
        private lateinit var entry1: SummitEntry
        private lateinit var entry2: SummitEntry
        private lateinit var entry3: SummitEntry
        private val summitEntries: ArrayList<SummitEntry> = ArrayList()

        init {
            try {
                entry1 = SummitEntry(SummitEntry.parseDate("2019-11-13"), "summit1", SportType.Bicycle, listOf("place1"), listOf("country1"), "comment1", 1, 1.1, 1.2, 1.3, 11, mutableListOf("participant1"), mutableListOf())
                entry2 = SummitEntry(SummitEntry.parseDate("2018-11-18"), "summit2", SportType.Bicycle, listOf("place2"), listOf("country2"), "comment2", 2, 2.1, 2.2, 2.3, 22, mutableListOf("participant1"), mutableListOf())
                entry3 = SummitEntry(SummitEntry.parseDate("2019-10-18"), "summit3", SportType.Bicycle, listOf("place3"), listOf("country3"), "comment3", 3, 3.1, 3.2, 3.3, 32, mutableListOf("participant1"), mutableListOf())
                summitEntries?.add(entry1)
                summitEntries?.add(entry2)
                summitEntries?.add(entry3)
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
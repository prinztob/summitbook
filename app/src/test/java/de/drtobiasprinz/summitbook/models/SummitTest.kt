package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.db.entities.*
import org.junit.Assert
import org.junit.Test
import java.text.ParseException
import java.util.*

class SummitTest {
    companion object {
        private var entry1 = Summit(Summit.parseDate("2019-11-13"), "summit1", SportType.Bicycle, listOf("place1"), listOf("country1"),
                "comment1", ElevationData.Companion.parse(11, 1), 1.1,
                VelocityData.Companion.parse(1.2, 1.3), 0.0, 0.0, mutableListOf("participant1"), mutableListOf("equipment1"),
                isFavorite = false, isPeak = false, imageIds = mutableListOf(), garminData = null, trackBoundingBox = null)
        private var entry2 = Summit(Summit.parseDate("2018-11-18"), "summit2", SportType.Bicycle, listOf("place2"), listOf("country2"),
                "comment2", ElevationData.parse(22, 2), 2.1,
                VelocityData.Companion.parse(2.2, 2.3), 0.0, 0.0, mutableListOf("participant1"), mutableListOf(),
                isFavorite = false, isPeak = false, imageIds = mutableListOf(), garminData = null, trackBoundingBox = null)
        private var entryNewFormat = Summit(Summit.parseDate("2019-10-18"), "summitNewFormat", SportType.IndoorTrainer, listOf("placeNewFormat"),
                listOf("countryNewFormat"), "commentNewFormat", ElevationData.Companion.parse(569, 62), 11.73,
                VelocityData.Companion.parse(12.6, 24.3), 48.05205764248967, 11.60579879768192, mutableListOf(), mutableListOf(),
                isFavorite = false, isPeak = false, imageIds = mutableListOf(), garminData = null, trackBoundingBox = null)
        private var entryNotInList = Summit(Summit.parseDate("2019-10-18"), "summit3", SportType.Bicycle, listOf("place3"), listOf("country3"),
                "comment3", ElevationData.Companion.parse(33, 3), 3.1,
                VelocityData.Companion.parse(3.2, 3.3), 0.0, 0.0, mutableListOf("participant1"), mutableListOf(),
                isFavorite = false, isPeak = false, imageIds = mutableListOf(), garminData = null, trackBoundingBox = null)
        private var summitEntries: ArrayList<Summit>? = ArrayList()

        init {
            try {
                summitEntries?.add(entry1)
                summitEntries?.add(entry2)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun parseFromCsvFileLine() {
        val newFormatLineToParse = "2019-10-18;summitNewFormat;IndoorTrainer;placeNewFormat;countryNewFormat;commentNewFormat;62;11.73;12.6;24.3;569;48.05205764248967;11.60579879768192;;;0"
        Assert.assertEquals(Summit.parseFromCsvFileLine(newFormatLineToParse), entryNewFormat)
        val lineToParse = "2019-11-13;summit1;Bicycle;place1;country1;comment1;1;1.1;1.2;1.3;11;;;participant1,equipment1:eq;;;;;;;;;;;;;;;;0\n"
        Assert.assertEquals(Summit.parseFromCsvFileLine(lineToParse), entry1)
        Assert.assertEquals(Summit.parseFromCsvFileLine(lineToParse).equipments, listOf("equipment1"))
        Assert.assertNotEquals(Summit.parseFromCsvFileLine(lineToParse), entry2)
        val lineToParse2 = "2019-10-13;Taubensee;Hike;Reit im Winkel;D/AUT;;420;6.9;2.9;-1;1000;;;participants1;;;;;;;;;;;;;;;;0\n"
        Assert.assertNotEquals(Summit.parseFromCsvFileLine(lineToParse2), entry1)
    }

    @Test
    @Throws(Exception::class)
    fun parseGarminDataFromCsvFileLine() {
        val lineWithGarminToParse = "2020-01-02;Isar-Radweg;Bicycle;Unterbiberg;D;Mirjam Jonah;125;28.68;16.45;41.8;570;48.0764305405319;11.622356493026;participant1;210479206;4393181740;952;129;155;75,100,50,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15;236;53;2.7;0;0;0;0;1\n"
        val entry: Summit = Summit.parseFromCsvFileLine(lineWithGarminToParse)
        Assert.assertEquals(entry.garminData, GarminData(mutableListOf("4393181740"), 952F, 129F, 155F, PowerData(75F, 100F, 50F, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15), 236, 53, 2.7f, 0f, 0f, 0f, 0f))
        Assert.assertTrue(entry.isFavorite)
    }

    @Test(expected = Exception::class)
    @Throws(Exception::class)
    fun parseFromInvalidCsvFileLineWithWrongNumberOfElements() {
        val line = "summit1;Bicycle;;AUT;;1;14.22;3.4;;1225\n"
        Summit.parseFromCsvFileLine(line)
    }

    @Test(expected = Exception::class)
    @Throws(Exception::class)
    fun parseFromInvalidCsvFileLineWithWrongSportTyp() {
        val line = "2019-11-13;summit1;NonExistingSportType;;AUT;;1;14.22;3.4;;1225\n"
        Summit.parseFromCsvFileLine(line)
    }

    @Test(expected = Exception::class)
    @Throws(Exception::class)
    fun parseFromInvalidCsvFileLineWithInvalidDateFormat() {
        val line = "Nov-13;summit1;Bicycle;;AUT;;1;14.22;3.4;;1225\n"
        Summit.parseFromCsvFileLine(line)
    }

    @Test
    @Throws(ParseException::class)
    fun isDuplicate() {
        Assert.assertTrue(entry1.isDuplicate(summitEntries))
        Assert.assertTrue(Summit(Summit.parseDate("2019-11-13"), "summit1", SportType.Bicycle, listOf("place1"), listOf("country1"),
                "comment1", ElevationData.Companion.parse(11, 1), 1.1,
                VelocityData.Companion.parse(1.2, 1.3), 0.0, 0.0, mutableListOf("participant1"), mutableListOf(),
                false, isPeak = false, imageIds = mutableListOf(), garminData = null, trackBoundingBox = null).isDuplicate(summitEntries))
        Assert.assertTrue(entry2.isDuplicate(summitEntries))
        Assert.assertFalse(entryNotInList.isDuplicate(summitEntries))
    }
}
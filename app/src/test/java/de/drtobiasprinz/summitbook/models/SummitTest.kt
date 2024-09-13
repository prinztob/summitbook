package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.db.entities.*
import de.drtobiasprinz.summitbook.db.entities.Summit.Companion.parseFromCsvFileLine
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_VERSION
import org.joda.time.DateTimeZone
import org.joda.time.tz.UTCProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.ParseException
import java.util.*

@RunWith(RobolectricTestRunner::class)
class SummitTest {
    @Before
    fun setUp() {
        DateTimeZone.setProvider(UTCProvider())
    }

    companion object {
        private var entry1 = Summit(
            Summit.parseDate("2019-11-13"),
            "summit1",
            SportType.Bicycle,
            listOf("place1"),
            listOf("country1"),
            "comment1",
            ElevationData(11, 1),
            1.1,
            VelocityData(1.3),
            participants = mutableListOf("participant1"),
            equipments = mutableListOf("equipment1"),
            activityId = 1,
            duration = 3300
        )
        private var entry2 = Summit(
            Summit.parseDate("2018-11-18"),
            "summit2",
            SportType.Bicycle,
            listOf("place2"),
            listOf("country2"),
            "comment2",
            ElevationData.parse(22, 2),
            2.1,
            VelocityData(2.2),
            participants = mutableListOf("participant1"),
            activityId = 1
        )
        private var entryNewFormat = Summit(
            Summit.parseDate("2019-10-18"),
            "summitNewFormat",
            SportType.IndoorTrainer,
            listOf("placeNewFormat"),
            listOf("countryNewFormat"),
            "commentNewFormat",
            ElevationData(569, 62),
            11.73,
            VelocityData(24.3),
            48.05205764248967,
            11.60579879768192,
            activityId = 1,
            duration = 3351
        )
        private var entryNotInList = Summit(
            Summit.parseDate("2019-10-18"),
            "summit3",
            SportType.Bicycle,
            listOf("place3"),
            listOf("country3"),
            "comment3",
            ElevationData(33, 3),
            3.1,
            VelocityData(3.3),
            participants = mutableListOf("participant1"),
            activityId = 1
        )
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
    fun parseFromCsvFileLineUsingCSVString() {
        val newFormatLineToParse =
            "2019-10-18;summitNewFormat;IndoorTrainer;placeNewFormat;countryNewFormat;commentNewFormat;62;11.73;12.6;24.3;569;48.05205764248967;11.60579879768192;;1;0\n"
        Assert.assertEquals(parseFromCsvFileLine(newFormatLineToParse), entryNewFormat)
        val lineToParse =
            "2019-11-13;summit1;Bicycle;place1;country1;comment1;1;1.1;1.2;1.3;11;;;participant1,equipment1:eq;1;;;;;;;;;;;;;;;0\n"
        Assert.assertEquals(parseFromCsvFileLine(lineToParse), entry1)
        Assert.assertEquals(
            parseFromCsvFileLine(lineToParse).equipments,
            listOf("equipment1")
        )
        Assert.assertNotEquals(parseFromCsvFileLine(lineToParse), entry2)
        val lineToParse2 =
            "2019-10-13;Taubensee;Hike;Reit im Winkel;D/AUT;;420;6.9;2.9;-1;1000;;;participants1;1;;;;;;;;;;;;;;;0\n"
        Assert.assertNotEquals(parseFromCsvFileLine(lineToParse2), entry1)
    }

    @Test
    @Throws(Exception::class)
    fun parseFromCsvFileLineUsingRegex() {
        Assert.assertEquals(entry1, parseFromCsvFileLine(entry1.toString(), CSV_FILE_VERSION))
        Assert.assertEquals(entry2, parseFromCsvFileLine(entry2.toString(), CSV_FILE_VERSION))
        Assert.assertEquals(
            entryNewFormat,
            parseFromCsvFileLine(entryNewFormat.toString(), CSV_FILE_VERSION)
        )
        Assert.assertEquals(
            entryNotInList,
            parseFromCsvFileLine(entryNotInList.toString(), CSV_FILE_VERSION)
        )
    }

    @Test
    @Throws(Exception::class)
    fun parseGarminDataFromCsvFileLine() {
        val lineWithGarminToParse =
            "2020-01-02;Isar-Radweg;Bicycle;Unterbiberg;D;Mirjam Jonah;125;28.68;16.45;41.8;570;48.0764305405319;11.622356493026;participant1;210479206;4393181740;952;129;155;75,100,50,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15;236;53;2.7;0;0;0;0;1\n"
        val entry: Summit = parseFromCsvFileLine(lineWithGarminToParse)
        Assert.assertEquals(
            entry.garminData,
            GarminData(
                mutableListOf("4393181740"),
                952F,
                129F,
                155F,
                PowerData(75F, 100F, 50F, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
                236,
                53f,
                2.7f,
                0f,
                0f,
                0f,
                0f
            )
        )
        Assert.assertTrue(entry.isFavorite)
    }

    @Test(expected = Exception::class)
    @Throws(Exception::class)
    fun parseFromInvalidCsvFileLineWithWrongNumberOfElements() {
        val line = "summit1;Bicycle;;AUT;;1;14.22;3.4;;1225\n"
        parseFromCsvFileLine(line)
    }

    @Test(expected = Exception::class)
    @Throws(Exception::class)
    fun parseFromInvalidCsvFileLineWithWrongSportTyp() {
        val line = "2019-11-13;summit1;NonExistingSportType;;AUT;;1;14.22;3.4;;1225\n"
        parseFromCsvFileLine(line)
    }

    @Test(expected = Exception::class)
    @Throws(Exception::class)
    fun parseFromInvalidCsvFileLineWithInvalidDateFormat() {
        val line = "Nov-13;summit1;Bicycle;;AUT;;1;14.22;3.4;;1225\n"
        parseFromCsvFileLine(line)
    }

    @Test
    @Throws(ParseException::class)
    fun isDuplicate() {
        Assert.assertTrue(entry1.isDuplicate(summitEntries))
        Assert.assertTrue(
            Summit(
                Summit.parseDate("2019-11-13"),
                "summit1",
                SportType.Bicycle,
                listOf("place1"),
                listOf("country1"),
                "comment1",
                ElevationData(11, 1),
                1.1,
                VelocityData(1.3),
                participants = mutableListOf("participant1"),
            ).isDuplicate(summitEntries)
        )
        Assert.assertTrue(entry2.isDuplicate(summitEntries))
        Assert.assertFalse(entryNotInList.isDuplicate(summitEntries))
    }
}
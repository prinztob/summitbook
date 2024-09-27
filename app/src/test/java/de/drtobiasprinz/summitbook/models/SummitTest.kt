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
        val linesToParse = listOf(
            "2024-02-12;Hahnenkamm;Mountainbike;1707769318078;55.72;11322;1000;444;55.8;50.07818299345672;9.109598798677325;0;0;;;Peter's Bike;;Deutschland",
            "2024-03-03;Große Reibn Tag 2;Skitour;1709566527019;28.46;35452;2340;2368;40.7;47.4718857742846;12.973065488040447;0;1;;Hanno Kaupp,Florian Seilmeier;K2-Ski;ac_id:1709566524164,Schneidstein,Windschattenkopf;Deutschland,Österreich",
            "2024-02-02;Münster;Mountainbike;1706890376241;22.8;4067;485;629;50.7;47.95985151082277;11.83500093407929;0;0;;;Canyon-MTB;;Deutschland",
            "2024-01-27;Gerstinger Joch ;Skitour;1706379248278;21.05;23534;1490;2035;35.4;47.348364014178514;12.260999428108335;0;1;;Hanno Kaupp,Florian Seilmeier,Christian Kellner;K2-Ski;Aschau;Österreich"
        )
        linesToParse.forEach {
            Assert.assertNotNull(parseFromCsvFileLine(it, CSV_FILE_VERSION))
        }

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
package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.ElevationData
import de.drtobiasprinz.summitbook.db.entities.GarminData
import de.drtobiasprinz.summitbook.db.entities.PowerData
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.VelocityData
import kotlinx.coroutines.test.runTest
import org.joda.time.DateTimeZone
import org.joda.time.tz.UTCProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.io.path.createTempDirectory

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ZipFileWriterTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries()
            .build()
        DateTimeZone.setProvider(UTCProvider())
    }

    @After
    fun closeDb() {
        db.close()
    }

    companion object {
        private var segmentDetail1 = SegmentDetails(0, "start1", "end1")
        private val segmentEntry1 = SegmentEntry(
            0,
            0,
            Summit.parseDate("2019-11-13"),
            1L,
            1,
            44.44,
            33.33,
            10,
            44.94,
            33.94,
            18.1,
            2.9,
            220,
            0,
            157,
            1
        )
        private val segmentEntry2 = SegmentEntry(
            0,
            0,
            Summit.parseDate("2019-11-14"),
            1L,
            1,
            44.44,
            33.33,
            10,
            44.94,
            33.94,
            18.1,
            2.9,
            220,
            0,
            157,
            1
        )
        private var entry1 = Summit(
            Summit.parseDate("2019-11-13"),
            "summit1",
            SportType.Bicycle,
            listOf("place1"),
            listOf("country1"),
            "comment1",
            ElevationData.Companion.parse(11, 1),
            1.1,
            VelocityData.Companion.parse(1.2, 1.3),
            participants = mutableListOf("participant1"),
            equipments = mutableListOf("equipment1"),
            isFavorite = false,
            isPeak = false,
            imageIds = mutableListOf(),
            garminData = null,
            trackBoundingBox = null
        )
        private var entry2 = Summit(
            Summit.parseDate("2018-11-18"),
            "summit2",
            SportType.Bicycle,
            listOf("place2"),
            listOf("country2"),
            "comment2",
            ElevationData.Companion.parse(22, 2),
            2.1,
            VelocityData.Companion.parse(2.2, 2.3),
            participants = mutableListOf("participant1"),
            isFavorite = false,
            isPeak = false,
            imageIds = mutableListOf(),
            garminData = null,
            trackBoundingBox = null
        )
        private var entry3 = Summit(
            Summit.parseDate("2019-10-18"),
            "summitNewFormat",
            SportType.IndoorTrainer,
            listOf("placeNewFormat"),
            listOf("countryNewFormat"),
            "commentNewFormat",
            ElevationData.Companion.parse(569, 62),
            11.73,
            VelocityData.Companion.parse(12.6, 24.3),
            48.05205764248967,
            11.60579879768192,
            isFavorite = false,
            isPeak = false,
            imageIds = mutableListOf(),
            garminData = null,
            trackBoundingBox = null
        )
        private var entry4 = Summit(
            Summit.parseDate("2019-10-18"),
            "summit3",
            SportType.Bicycle,
            listOf("place3"),
            listOf("country3"),
            "comment3",
            ElevationData.Companion.parse(33, 3),
            3.1,
            VelocityData.Companion.parse(3.2, 3.3),
            participants = mutableListOf("participant1"),
            isFavorite = false,
            isPeak = false,
            imageIds = mutableListOf(),
            garminData = GarminData(
                mutableListOf("123456789"),
                100f, 120f, 160f,
                PowerData(100f, 300f, 200f),
                255, 50f, 2f, 3f, 4f, 3f, 111f
            ),
            trackBoundingBox = null
        )

    }

    @Test
    fun testExportAndImportFromZipFile() = runTest(testBody = {
        // given
        db.summitsDao().addSummit(entry1)
        db.summitsDao().addSummit(entry2)
        db.summitsDao().addSummit(entry3)
        db.summitsDao().addSummit(entry4)
        val id = db.segmentsDao().addSegmentDetails(segmentDetail1)
        segmentEntry1.segmentId = id
        segmentEntry2.segmentId = id
        db.segmentsDao().addSegmentEntry(segmentEntry1)
        db.segmentsDao().addSegmentEntry(segmentEntry2)
        val summits = db.summitsDao().allSummit ?: emptyList()
        val segments = db.segmentsDao().getAllSegmentsDeprecated() ?: emptyList()
        val file = kotlin.io.path.createTempFile(suffix = ".zip").toFile()


        val writer = ZipFileWriter(
            summits,
            segments,
            emptyList(),
            context,
            exportThirdPartyData = true,
            exportCalculatedData = true
        )
        writer.dir = createTempDirectory().toFile()
        writer.writeToZipFile(file.outputStream())


        val reader = ZipFileReader(
            createTempDirectory().toFile()
        )
        reader.extractAndImport(file.inputStream())

        assert(summits == db.summitsDao().allSummit)
        assert(segments == db.segmentsDao().getAllSegmentsDeprecated())

    })

}
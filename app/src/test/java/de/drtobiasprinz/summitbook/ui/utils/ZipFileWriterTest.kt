package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.drtobiasprinz.summitbook.dao.SegmentsDao
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.ArrayList
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.math.round

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
class ZipFileWriterTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    companion object {
        private var segmentDetail1 = SegmentDetails(0, "start1", "end1")
        private val segmentEntry1 = SegmentEntry(0, 0, Summit.parseDate("2019-11-13"), 1L, 1,
                44.44, 33.33, 10, 44.94, 33.94,
                18.1, 2.9, 220, 0, 157, 1)
        private val segmentEntry2 = SegmentEntry(0, 0, Summit.parseDate("2019-11-14"), 1L, 1,
                44.44, 33.33, 10, 44.94, 33.94,
                18.1, 2.9, 220, 0, 157, 1)
        private var entry1 = Summit(Summit.parseDate("2019-11-13"), "summit1", SportType.Bicycle, listOf("place1"), listOf("country1"),
                "comment1", ElevationData.Companion.parse(11, 1), 1.1,
                VelocityData.Companion.parse(1.2, 1.3), 0.0, 0.0, mutableListOf("participant1"), mutableListOf("equipment1"),
                isFavorite = false, isPeak = false, imageIds = mutableListOf(), garminData = null, trackBoundingBox = null)
        private var entry2 = Summit(Summit.parseDate("2018-11-18"), "summit2", SportType.Bicycle, listOf("place2"), listOf("country2"),
                "comment2", ElevationData.Companion.parse(22, 2), 2.1,
                VelocityData.Companion.parse(2.2, 2.3), 0.0, 0.0, mutableListOf("participant1"), mutableListOf(),
                isFavorite = false, isPeak = false, imageIds = mutableListOf(), garminData = null, trackBoundingBox = null)
        private var entry3 = Summit(Summit.parseDate("2019-10-18"), "summitNewFormat", SportType.IndoorTrainer, listOf("placeNewFormat"),
                listOf("countryNewFormat"), "commentNewFormat", ElevationData.Companion.parse(569, 62), 11.73,
                VelocityData.Companion.parse(12.6, 24.3), 48.05205764248967, 11.60579879768192, mutableListOf(), mutableListOf(),
                isFavorite = false, isPeak = false, imageIds = mutableListOf(), garminData = null, trackBoundingBox = null)
        private var entry4 = Summit(Summit.parseDate("2019-10-18"), "summit3", SportType.Bicycle, listOf("place3"), listOf("country3"),
                "comment3", ElevationData.Companion.parse(33, 3), 3.1,
                VelocityData.Companion.parse(3.2, 3.3), 0.0, 0.0, mutableListOf("participant1"), mutableListOf(),
                isFavorite = false, isPeak = false, imageIds = mutableListOf(), garminData = null, trackBoundingBox = null)

    }

    @ExperimentalPathApi
    @Test
    @Throws(Exception::class)
    fun exportAndImportFromZipFile() {
        // given
        db.summitDao()?.addSummit(entry1)
        db.summitDao()?.addSummit(entry2)
        db.summitDao()?.addSummit(entry3)
        db.summitDao()?.addSummit(entry4)
        val id = db.segmentsDao()?.addSegmentDetails(segmentDetail1)
        if (id != null) {
            segmentEntry1.segmentId = id
            segmentEntry2.segmentId = id
            db.segmentsDao()?.addSegmentEntry(segmentEntry1)
            db.segmentsDao()?.addSegmentEntry(segmentEntry2)
        }
        val summits = db.summitDao()?.allSummit?: emptyList()
        val segments = db.segmentsDao()?.getAllSegments()?: emptyList()
        val file = kotlin.io.path.createTempFile(suffix = ".zip").toFile()

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        val writer = ZipFileWriter(summits, segments, context, true, true)
        writer.dir = createTempDirectory().toFile()
        writer.writeToZipFile(file.outputStream())


        val reader = ZipFileReader(createTempDirectory().toFile(), db)
        reader.extractAndImport(file.inputStream())

        assert(summits == db.summitDao()?.allSummit)
        assert(segments == db.segmentsDao()?.getAllSegments())

    }

}
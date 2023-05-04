package de.drtobiasprinz.summitbook.models

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.dao.SegmentsDao
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.Summit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
class SegmentTest {

    private lateinit var db: AppDatabase
    private var dao: SegmentsDao? = null

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = db.segmentsDao()
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

    }

    @Test
    @Throws(Exception::class)
    fun parseNewSegmentFromCsvFileLine() {
        val newFormatLineToParse = "start1;end1;2019-11-13;1;1;44.44;33.33;10;44.94;33.94;18.1;2.9;220;0;157;1"
        Segment.parseFromCsvFileLine(newFormatLineToParse, mutableListOf(), db)
        val segments = dao?.getAllSegmentsDeprecated()
        assert(segments?.size == 1)
        assert(segments?.firstOrNull()?.segmentEntries?.size == 1)
        assert(segments?.firstOrNull() == Segment(segmentDetail1, mutableListOf(segmentEntry1)))
    }

    @Test
    @Throws(Exception::class)
    fun parseSegmentFromCsvFileLineWithKnownSegmentDetail() {
        val segmentsBefore = dao?.getAllSegmentsDeprecated()
        val line1ToParse = "start1;end1;2019-11-13;1;1;44.44;33.33;10;44.94;33.94;18.1;2.9;220;0;157;1"
        Segment.parseFromCsvFileLine(line1ToParse, segmentsBefore, db)
        val line2ToParse = "start1;end1;2019-11-14;1;1;44.44;33.33;10;44.94;33.94;18.1;2.9;220;0;157;1"
        Segment.parseFromCsvFileLine(line2ToParse, segmentsBefore, db)
        val segmentsAfter = dao?.getAllSegmentsDeprecated()
        assert(segmentsAfter?.size == 1)
        assert(segmentsAfter == segmentsBefore)
        assert(segmentsAfter?.firstOrNull()?.segmentEntries?.size == 2)
        assert(segmentsAfter?.firstOrNull() == Segment(segmentDetail1, mutableListOf(segmentEntry1, segmentEntry2)))
    }

    @Test
    @Throws(Exception::class)
    fun parseSegmentFromCsvFileLineDuplication() {
        val segmentsBefore = dao?.getAllSegmentsDeprecated()
        val line1ToParse = "start1;end1;2019-11-13;1;1;44.44;33.33;10;44.94;33.94;18.1;2.9;220;0;157;1"
        Segment.parseFromCsvFileLine(line1ToParse, segmentsBefore, db)
        val line2ToParse = "start1;end1;2019-11-13;1;1;44.44;33.33;10;44.94;33.94;18.1;2.9;220;0;157;1"
        Segment.parseFromCsvFileLine(line2ToParse, segmentsBefore, db)
        val segmentsAfter = dao?.getAllSegmentsDeprecated()
        assert(segmentsAfter?.size == 1)
        assert(segmentsAfter == segmentsBefore)
        assert(segmentsAfter?.firstOrNull()?.segmentEntries?.size == 1)
        assert(segmentsAfter?.firstOrNull() == Segment(segmentDetail1, mutableListOf(segmentEntry1)))
    }


}
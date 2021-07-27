package de.drtobiasprinz.summitbook.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.google.android.gms.maps.model.LatLng
import de.drtobiasprinz.summitbook.models.*
import java.text.ParseException
import java.util.*

class SummitBookDatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        updateMyDatabase(db, 0)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        updateMyDatabase(db, oldVersion)
    }

    fun insertSummit(db: SQLiteDatabase, entry: SummitEntry): Long {
        val summitValues = setContentValues(entry)
        val _id = db.insert(SUMMITS_DB_TABLE_NAME, null, summitValues)
        val trackBoundingBox = entry.trackBoundingBox
        if (trackBoundingBox != null) {
            insertTrackBoundingBox(db, _id.toInt(), trackBoundingBox)
        }
        return _id
    }

    fun insertIgnoredActivityId(db: SQLiteDatabase, activityId: String): Long {
        val values = ContentValues()
        values.put("ACTIVITY_ID", activityId)
        return db.insert(IGNORED_ACTIVITIES, null, values)
    }

    private fun insertTrackBoundingBox(db: SQLiteDatabase, id: Int, trackBoundingBox: TrackBoundingBox): Long {
        val values = setContentValues(id, trackBoundingBox)
        return db.insert(TRACK_BOUNDING_BOX_DB_TABLE_NAME, null, values)
    }

    fun insertBookmark(db: SQLiteDatabase, entry: BookmarkEntry): Long {
        val summitValues = setContentValues(entry)
        return db.insert(BOOKMARKS_DB_TABLE_NAME, null, summitValues)
    }

    fun updateSummit(db: SQLiteDatabase, entry: SummitEntry) {
        val summitValues = setContentValues(entry)
        db.update(SUMMITS_DB_TABLE_NAME, summitValues, "_id=" + entry._id, null)
        val trackBoundingBox = entry.trackBoundingBox
        if (trackBoundingBox != null) {
            updateTrackBoundingBox(db, entry._id, trackBoundingBox)
        }
    }

    fun updateTrackBoundingBox(db: SQLiteDatabase, id: Int, trackBoundingBox: TrackBoundingBox) {
        val values = setContentValues(id, trackBoundingBox)
        val updateId = db.update(TRACK_BOUNDING_BOX_DB_TABLE_NAME, values, "_id=$id", null)
        if (updateId == 0) {
            insertTrackBoundingBox(db, id, trackBoundingBox)
        }
    }

    fun updateBookmark(db: SQLiteDatabase, entry: BookmarkEntry) {
        val summitValues = setContentValues(entry)
        db.update(BOOKMARKS_DB_TABLE_NAME, summitValues, "_id=" + entry._id, null)
    }

    private fun setContentValues(entry: BookmarkEntry): ContentValues {
        val bookmarkValues = ContentValues()
        bookmarkValues.put("NAME", entry.name)
        bookmarkValues.put("SPORT_TYPE", entry.sportType.toString())
        bookmarkValues.put("COMMENTS", entry.comments)
        bookmarkValues.put("HEIGHT_METERS", entry.heightMeter)
        bookmarkValues.put("KILOMETERS", entry.kilometers)
        bookmarkValues.put("ACTIVITY_ID", entry.activityId)
        return bookmarkValues
    }

    private fun setContentValues(id: Int, entry: TrackBoundingBox): ContentValues {
        val values = ContentValues()
        values.put("_id", id)
        values.put("LAT_NORTH", entry.latNorth)
        values.put("LAT_SOUTH", entry.latSouth)
        values.put("LON_WEST", entry.lonWest)
        values.put("LON_EAST", entry.lonEast)
        return values
    }

    private fun setContentValues(entry: SummitEntry): ContentValues {
        val summitValues = ContentValues()
        summitValues.put("TOUR_DATE", entry.getDateAsString())
        summitValues.put("NAME", entry.name)
        summitValues.put("SPORT_TYPE", entry.sportType.toString())
        summitValues.put("PLACE", entry.places.joinToString(","))
        summitValues.put("COUNTRY", entry.countries.joinToString(","))
        summitValues.put("COMMENTS", entry.comments)
        summitValues.put("HEIGHT_METERS", entry.elevationData.toString())
        summitValues.put("KILOMETERS", entry.kilometers)
        summitValues.put("PACE", entry.velocityData.toString())
        summitValues.put("TOP_SPEED", entry.velocityData.maxVelocity)
        summitValues.put("TOP_ELEVATION", entry.elevationData.maxElevation)
        summitValues.put("ACTIVITY_ID", entry.activityId)
        summitValues.put("FAVORITE", if (entry.isFavorite) 1 else 0)
        summitValues.put("IMAGE_ORDER", entry.imageIds.joinToString(","))
        if (entry.latLng != null) {
            summitValues.put("LATITUDE", entry.latLng?.latitude)
            summitValues.put("LONGITUDE", entry.latLng?.longitude)
        }
        summitValues.put("PARTICIPANTS", entry.participants.joinToString(","))
        if (entry.activityData != null) {
            summitValues.put("GARMIN_ACTIVITY_ID", entry.activityData?.activityIds?.joinToString(","))
            summitValues.put("CALORIES", entry.activityData?.calories)
            summitValues.put("AVERAGE_HR", entry.activityData?.averageHR)
            summitValues.put("MAX_HR", entry.activityData?.maxHR)
            summitValues.put("POWER", entry.activityData?.power?.toString())
            summitValues.put("FTP", entry.activityData?.ftp)
            summitValues.put("VO2MAX", entry.activityData?.vo2max)
            summitValues.put("AEROBIC_TRAININGS_EFFECT", entry.activityData?.aerobicTrainingEffect)
            summitValues.put("ANAEROBIC_TRAININGS_EFFECT", entry.activityData?.anaerobicTrainingEffect)
            summitValues.put("FLOW", entry.activityData?.flow)
            summitValues.put("GRIT", entry.activityData?.grit)
            summitValues.put("TRAININGS_LOAD", entry.activityData?.trainingLoad)
        }
        return summitValues
    }

    fun deleteSummit(db: SQLiteDatabase, entry: SummitEntry): Boolean {
        val sql = "delete from " + SUMMITS_DB_TABLE_NAME + " where _id='" + entry._id + "'"
        return try {
            db.execSQL(sql)
            true
        } catch (exception: SQLiteException) {
            false
        }
    }

    fun deleteBookmark(db: SQLiteDatabase, entry: BookmarkEntry): Boolean {
        val sql = "delete from " + BOOKMARKS_DB_TABLE_NAME + " where _id='" + entry._id + "'"
        return try {
            db.execSQL(sql)
            true
        } catch (exception: SQLiteException) {
            false
        }
    }

    fun dropIgnoredActivities(db: SQLiteDatabase?) {
        db?.execSQL("DELETE FROM $IGNORED_ACTIVITIES;")
    }

    fun getSummitsWithId(id: Int, db: SQLiteDatabase?): SummitEntry? {
        val selectionArgs = arrayOf<String?>(id.toString())
        val cursor = db?.query(SUMMITS_DB_TABLE_NAME,
                getColumnsOfSummits(),
                "_id =?", selectionArgs,
                null, null, "TOUR_DATE DESC")
        cursor?.moveToNext()
        val entry = getSummitEntry(cursor)
        if (entry != null) {
            entry.trackBoundingBox = getSummitsBoundingBoxWithId(entry._id, db)
        }
        return entry
    }

    private fun getSummitsBoundingBoxWithId(id: Int, db: SQLiteDatabase?): TrackBoundingBox? {
        val selectionArgs = arrayOf<String?>(id.toString())
        val cursor = db?.query(TRACK_BOUNDING_BOX_DB_TABLE_NAME,
                getColumnsOfBoundingBox(),
                "_id =?", selectionArgs,
                null, null, null)
        cursor?.moveToNext()
        if (cursor != null && cursor.count > 0) {
            val trackBoundingBox = TrackBoundingBox(cursor.getDouble(0), cursor.getDouble(1), cursor.getDouble(2), cursor.getDouble(3))
            cursor.close()
            return trackBoundingBox
        } else {
            cursor?.close()
            return null
        }
    }

    fun getSummitsWithActivityId(activityId: Int, db: SQLiteDatabase?): SummitEntry? {
        val selectionArgs = arrayOf<String?>(activityId.toString())
        val cursor = db?.query(SUMMITS_DB_TABLE_NAME,
                getColumnsOfSummits(),
                "ACTIVITY_ID =?", selectionArgs,
                null, null, "TOUR_DATE DESC")
        cursor?.moveToNext()
        return getSummitEntry(cursor)
    }


    fun getSummitsWithConnectedActivityId(id: Int, db: SQLiteDatabase?): SummitEntry? {
        val selectionArgs = arrayOf<String?>("%${SummitEntry.CONNECTED_ACTIVITY_PREFIX}${id}%")
        val cursor = db?.query(SUMMITS_DB_TABLE_NAME,
                getColumnsOfSummits(),
                "PLACE LIKE ?", selectionArgs,
                null, null, "TOUR_DATE DESC")
        cursor?.moveToNext()
        return getSummitEntry(cursor)
    }

    fun getBookmarkWithId(id: Int, db: SQLiteDatabase?): BookmarkEntry? {
        val selectionArgs = arrayOf<String?>(id.toString())
        val cursor = db?.query(BOOKMARKS_DB_TABLE_NAME,
                getColumnsOfBookmarks(),
                "_id =?", selectionArgs,
                null, null, "NAME DESC")
        cursor?.moveToNext()
        return if (cursor != null) {
            getBookmarkEntry(cursor)
        } else {
            null
        }
    }

    fun getAllBookmarks(db: SQLiteDatabase?): ArrayList<BookmarkEntry> {
        val bookmarks = ArrayList<BookmarkEntry>()
        val cursor = db?.query(BOOKMARKS_DB_TABLE_NAME,
                getColumnsOfBookmarks(),
                null, null,
                null, null, "NAME DESC")
        if (cursor != null) {
            while (cursor.moveToNext()) {
                bookmarks.add(getBookmarkEntry(cursor))
            }
            cursor.close()
        }
        return bookmarks
    }

    fun getAllIgnoredActivities(db: SQLiteDatabase?): ArrayList<String> {
        val activityIds = ArrayList<String>()
        val cursor = db?.query(IGNORED_ACTIVITIES,
                arrayOf("ACTIVITY_ID"),
                null, null,
                null, null, "ACTIVITY_ID DESC")
        if (cursor != null) {
            while (cursor.moveToNext()) {
                activityIds.add(cursor.getString(0))
            }
            cursor.close()
        }
        return activityIds
    }

    fun getAllSummits(db: SQLiteDatabase?, maxEntries: Int = -1): ArrayList<SummitEntry> {
        val summitEntries = ArrayList<SummitEntry>()
        val cursor = db?.query(SUMMITS_DB_TABLE_NAME,
                getColumnsOfSummits(),
                null, null,
                null, null, "TOUR_DATE DESC")
        var i = 0
        if (cursor != null) {
            while (cursor.moveToNext() && (maxEntries < 0 || (maxEntries >= 0 && i <= maxEntries))) {
                val entry = getSummitEntry(cursor)
                if (entry != null) {
                    updateImageIdsSummit(db, entry._id, entry.imageIds)
                    summitEntries.add(entry)
                }
                i++
            }
            cursor.close()
        }
        summitEntries.forEach {
            it.trackBoundingBox = getSummitsBoundingBoxWithId(it._id, db)
        }
        return summitEntries
    }

    private fun getBookmarkEntry(cursor: Cursor): BookmarkEntry {
        return BookmarkEntry(
                cursor.getInt(0),
                cursor.getString(1),
                SportType.valueOf(cursor.getString(2)),
                cursor.getString(3),
                cursor.getInt(4),
                cursor.getDouble(5),
                cursor.getInt(6)
        )
    }

    private fun getSummitEntry(cursor: Cursor?): SummitEntry? {
        var entry: SummitEntry? = null
        try {
            if (cursor != null && cursor.count > 0) {
                val imageIdsString = cursor.getString(29) ?: ""
                val imageIds = if (imageIdsString != "") imageIdsString.split(",").map { it.toInt() } as MutableList<Int> else mutableListOf()
                entry = SummitEntry(
                        cursor.getInt(0),
                        SummitEntry.parseDate(cursor.getString(1)),
                        cursor.getString(2),
                        SportType.valueOf(cursor.getString(3)),
                        if (cursor.getString(4) != null && cursor.getString(4) != "") cursor.getString(4).split(",") else emptyList(),
                        if (cursor.getString(5) != null && cursor.getString(5) != "") cursor.getString(5).split(",") else emptyList(),
                        cursor.getString(6),
                        ElevationData.parse(cursor.getString(7).split(","), cursor.getInt(11)),
                        cursor.getDouble(8),
                        VelocityData.parse(cursor.getString(9).split(","), cursor.getDouble(10)),
                        if (cursor.getString(15) != null && cursor.getString(15) != "") cursor.getString(15).split(",") else emptyList(),
                        imageIds,
                        cursor.getInt(12)
                )
                entry.isFavorite = cursor.getInt(28) == 1
                if (cursor.getFloat(16) != 0f) {
                    val activityData = GarminActivityData(
                            cursor.getString(16).split(",") as MutableList<String>,
                            cursor.getFloat(17),
                            cursor.getFloat(18),
                            cursor.getFloat(19),
                            PowerData.parse(cursor.getString(20).split(",")),
                            cursor.getInt(21),
                            cursor.getInt(22),
                            cursor.getFloat(23),
                            cursor.getFloat(24),
                            cursor.getFloat(25),
                            cursor.getFloat(26),
                            cursor.getFloat(27)
                    )
                    entry.activityData = activityData
                }


                if (cursor.getDouble(13) != 0.0 && cursor.getDouble(14) != 0.0) {
                    entry.latLng = LatLng(cursor.getDouble(13), cursor.getDouble(14))
                }
                if (entry.activityId <= 0) {
                    entry.activityId = SummitEntry.getActivityId(entry.date, entry.name, entry.sportType, entry.elevationData.elevationGain, entry.kilometers)
                }
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return entry
    }

    private fun getColumnsOfBoundingBox(): Array<String?> {
        return arrayOf("LAT_NORTH",
                "LAT_SOUTH",
                "LON_WEST",
                "LON_EAST")
    }

    private fun getColumnsOfBookmarks(): Array<String?> {
        return arrayOf("_id",
                "NAME",
                "SPORT_TYPE",
                "COMMENTS",
                "HEIGHT_METERS",
                "KILOMETERS",
                "ACTIVITY_ID")
    }

    private fun getColumnsOfSummits(): Array<String?> {
        return arrayOf("_id",
                "TOUR_DATE",
                "NAME",
                "SPORT_TYPE",
                "PLACE",
                "COUNTRY",
                "COMMENTS",
                "HEIGHT_METERS",
                "KILOMETERS",
                "PACE",
                "TOP_SPEED",
                "TOP_ELEVATION",
                "ACTIVITY_ID",
                "LATITUDE",
                "LONGITUDE",
                "PARTICIPANTS",
                "GARMIN_ACTIVITY_ID",
                "CALORIES",
                "AVERAGE_HR",
                "MAX_HR",
                "POWER",
                "FTP",
                "VO2MAX",
                "AEROBIC_TRAININGS_EFFECT",
                "ANAEROBIC_TRAININGS_EFFECT",
                "GRIT",
                "FLOW",
                "TRAININGS_LOAD",
                "FAVORITE",
                "IMAGE_ORDER")
    }

    private fun updateMyDatabase(db: SQLiteDatabase, oldVersion: Int) {
        if (oldVersion < 1) {
            db.execSQL("CREATE TABLE " + SUMMITS_DB_TABLE_NAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "TOUR_DATE DATE," +
                    "NAME TEXT, " +
                    "SPORT_TYPE TEXT," +
                    "PLACE TEXT," +
                    "COUNTRY TEXT," +
                    "COMMENTS TEXT," +
                    "HEIGHT_METERS INTEGER, " +
                    "KILOMETERS REAL, " +
                    "PACE REAL, " +
                    "TOP_SPEED REAL, " +
                    "TOP_ELEVATION INTEGER, " +
                    "LATITUDE REAL, " +
                    "LONGITUDE REAL," +
                    "PARTICIPANTS TEXT," +
                    "ACTIVITY_ID INTEGER," +
                    "GARMIN_ACTIVITY_ID TEXT," +
                    "CALORIES REAL," +
                    "AVERAGE_HR REAL," +
                    "MAX_HR REAL," +
                    "POWER INTEGER," +
                    "FTP INTEGER," +
                    "VO2MAX INTEGER," +
                    "AEROBIC_TRAININGS_EFFECT REAL," +
                    "ANAEROBIC_TRAININGS_EFFECT REAL," +
                    "GRIT REAL," +
                    "FLOW REAL," +
                    "TRAININGS_LOAD REAL," +
                    "FAVORITE BOOLEAN NOT NULL CHECK (FAVORITE IN (0,1))," +
                    "IMAGE_ORDER TEXT" +
                    ");")

            db.execSQL("CREATE TABLE " + BOOKMARKS_DB_TABLE_NAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "NAME TEXT, " +
                    "SPORT_TYPE TEXT," +
                    "COMMENTS TEXT," +
                    "HEIGHT_METERS INTEGER, " +
                    "KILOMETERS REAL, " +
                    "ACTIVITY_ID INTEGER" +
                    ");")

            db.execSQL("CREATE TABLE " + TRACK_BOUNDING_BOX_DB_TABLE_NAME + " (_id INTEGER NOT NULL PRIMARY KEY, " +
                    "LAT_NORTH REAL, " +
                    "LAT_SOUTH REAL," +
                    "LON_WEST REAL," +
                    "LON_EAST REAL " +
                    ");")
        }
        db.execSQL("CREATE TABLE $IGNORED_ACTIVITIES (_id INTEGER NOT NULL PRIMARY KEY, " +
                "ACTIVITY_ID INTEGER NOT NULL);")
    }

    fun updatePositionOfSummit(db: SQLiteDatabase?, summitEntryId: Int, latLng: LatLng) {
        val summitValues = ContentValues()
        summitValues.put("LATITUDE", latLng.latitude)
        summitValues.put("LONGITUDE", latLng.longitude)
        db?.update(SUMMITS_DB_TABLE_NAME, summitValues, "_id=$summitEntryId", null)
    }

    fun updateIsFavoriteSummit(db: SQLiteDatabase?, summitEntryId: Int, isFavorite: Boolean) {
        val summitValues = ContentValues()
        summitValues.put("FAVORITE", if (isFavorite) 1 else 0)
        db?.update(SUMMITS_DB_TABLE_NAME, summitValues, "_id=$summitEntryId", null)
    }

    fun updateVelocityDataSummit(db: SQLiteDatabase?, summitEntryId: Int, velocityData: VelocityData) {
        val summitValues = ContentValues()
        summitValues.put("PACE", velocityData.toString())
        db?.update(SUMMITS_DB_TABLE_NAME, summitValues, "_id=$summitEntryId", null)
    }

    fun updateElevationDataSummit(db: SQLiteDatabase?, summitEntryId: Int, elevationData: ElevationData) {
        val summitValues = ContentValues()
        summitValues.put("HEIGHT_METERS", elevationData.toString())
        db?.update(SUMMITS_DB_TABLE_NAME, summitValues, "_id=$summitEntryId", null)
    }

    fun updateImageIdsSummit(db: SQLiteDatabase?, summitEntryId: Int, imageIds: MutableList<Int>) {
        val summitValues = ContentValues()
        summitValues.put("IMAGE_ORDER", imageIds.joinToString(","))
        db?.update(SUMMITS_DB_TABLE_NAME, summitValues, "_id=$summitEntryId", null)
    }

    companion object {
        private const val DB_NAME: String = "summitbook"
        private const val SUMMITS_DB_TABLE_NAME: String = "SUMMITS"
        private const val TRACK_BOUNDING_BOX_DB_TABLE_NAME: String = "BOUNDINGBOX"
        private const val IGNORED_ACTIVITIES: String = "IGNOREDACTIVITIES"
        private const val BOOKMARKS_DB_TABLE_NAME: String = "BOOKMARKS"
        private const val DB_VERSION = 1
    }
}
package de.drtobiasprinz.summitbook.models

import android.content.Context
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.database.AppDatabase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


@Entity
class Summit(
        var date: Date, var name: String, var sportType: SportType, var places: List<String>,
        var countries: List<String>, var comments: String,
        @Embedded var elevationData: ElevationData, var kilometers: Double,
        @Embedded var velocityData: VelocityData, var lat: Double?, var lng: Double?,
        var participants: List<String>, var isFavorite: Boolean, var imageIds: MutableList<Int>,
        @Embedded var garminData: GarminData?, @Embedded var trackBoundingBox: TrackBoundingBox?,
        var activityId: Long = System.currentTimeMillis()
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    @Ignore
    var latLng = lat?.let { lng?.let { it1 -> LatLng(it, it1) } }
    @Ignore
    var duration = getWellDefinedDuration()
    @Ignore
    var gpsTrack: GpsTrack? = null
    @Ignore
    var isSelected: Boolean = false

    private fun getWellDefinedDuration(): Double {
        val dur = if (velocityData.avgVelocity > 0) kilometers / velocityData.avgVelocity else 0.0
        if (dur < 24) {
            return dur
        } else {
            return 0.0
        }
    }

    fun getImagePath(imageId: Int): Path {
        val rootDirectoryImages = File(MainActivity.storage, "${Summit.subDirForImages}/${id}")
        return Paths.get(rootDirectoryImages.toString(), String.format(Locale.ENGLISH, "%s.jpg", imageId))
    }

    fun getImageUrl(imageId: Int): String {
        val rootDirectoryImages = File(MainActivity.storage, "${Summit.subDirForImages}/${id}")
        return "file://" + Paths.get(rootDirectoryImages.toString(), String.format(Locale.ENGLISH, "%s.jpg", imageId)).toString()
    }

    fun getNextImagePath(addIdToImageIds: Boolean = false): Path {
        val rootDirectoryImages = File(MainActivity.storage, "${Summit.subDirForImages}/${id}")
        var imageId = 1001
        if (imageIds.isEmpty()) {
            if (!rootDirectoryImages.exists()) {
                rootDirectoryImages.mkdir()
            }
        } else {
            imageId = (imageIds.maxOrNull() ?: imageId) + 1
        }
        if (addIdToImageIds) {
            imageIds.add(imageId)
        }
        return getImagePath(imageId)
    }

    fun getGpsTrackPath(): Path? {
        return Paths.get(MainActivity.storage.toString(), subDirForGpsTracks, String.format(Locale.ENGLISH, "id_%s.gpx", activityId))
    }

    @Throws(IOException::class)
    fun copyGpsTrackToTempFile(dir: File?): File? {
        val tempFile = File.createTempFile(String.format(Locale.ENGLISH, "id_%s", activityId),
                ".gpx", dir)
        if (hasGpsTrack()) {
            val gpsTrackPath = getGpsTrackPath()
            if (gpsTrackPath != null) {
                Files.copy(gpsTrackPath, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
        return tempFile
    }

    fun getExportImagePath(): String {
        return Paths.get(subDirForImages, String.format("%s_%s.jpg", getDateAsString(), name.replace(" ".toRegex(), "_"))).toString()
    }

    fun getExportImageFolderPath(): String {
        return Paths.get(subDirForImages, "${getDateAsString()}_${name.replace(" ".toRegex(), "_")}").toString()
    }

    fun getExportImagePath(id: Int): String {
        return Paths.get(getExportImageFolderPath(), "${id}.jpg").toString()
    }

    fun getExportTrackPath(): String {
        return Paths.get(subDirForGpsTracks, String.format("%s_%s.gpx", getDateAsString(), name.replace(" ".toRegex(), "_").replace("/".toRegex(), "_"))).toString()
    }

    fun hasImagePath(): Boolean {
        return imageIds.isNotEmpty()
    }

    fun hasGpsTrack(): Boolean {
        return getGpsTrackPath()?.toFile()?.exists() ?: false
    }

    fun isDuplicate(allExistingEntries: List<Summit>?): Boolean {
        var isInList = false
        if (allExistingEntries != null) {
            for (entry in allExistingEntries) {
                if (entry == this) {
                    isInList = true
                }
            }
        }
        return isInList
    }

    fun getDateAsString(): String? {
        val dateFormat: DateFormat = SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH)
        return dateFormat.format(date)
    }

    fun getDateAsFloat(): Float {
        return ((date.time - REFERENCE_VALUE_DATE) / 1e8).toFloat()
    }

    fun setGpsTrack() {
        if (hasGpsTrack()) {
            val path: Path? = getGpsTrackPath()
            if (gpsTrack == null && path != null) {
                gpsTrack = GpsTrack(path)
            }
            if (gpsTrack != null && gpsTrack?.hasNoTrackPoints() == true) {
                gpsTrack?.parseTrack()
            }
        }
    }

    override fun toString(): String {
        val lat = latLng?.latitude?.toString() ?: ""
        val lng = latLng?.longitude?.toString() ?: ""
        var entryToString = getDateAsString() + ';' +
                name + ';' +
                sportType + ';' +
                places.joinToString(",") + ';' +
                countries.joinToString(",") + ';' +
                comments + ';' +
                elevationData.toString() + ';' +
                kilometers + ';' +
                velocityData.toString() + ';' +
                velocityData.maxVelocity + ';' +
                elevationData.maxElevation + ';' +
                lat + ';' +
                lng + ';' +
                participants.joinToString(",") + ';' +
                activityId + ';'
        entryToString += if (garminData != null) {
            garminData.toString()
        } else {
            ";;;;;;;;;;;"
        }
        entryToString += ";" + if (isFavorite) "1\n" else "0\n"
        return entryToString
    }

    fun toReadableString(context: Context): String {
        return "${context.getString(R.string.tour_date)}: ${getDateAsString()}, ${context.getString(R.string.name)}: ${name}, ${context.getString(R.string.type)}: $sportType, ${elevationData.elevationGain} hm, $kilometers km"
    }

    fun getConnectedEntryString(context: Context): String {
        return "${context.getString(R.string.end_of)} $name"
    }


    fun getPlacesWithConnectedEntryString(context: Context, database: AppDatabase): List<String> {
        val updatedPlaces = mutableListOf<String>()
        for (place in places) {
            val matchResult = "${CONNECTED_ACTIVITY_PREFIX}([0-9]*)".toRegex().find(place)
            if (matchResult?.groupValues != null) {
                val connectedSummit = database.summitDao()?.getSummitFromActivityId(matchResult.groupValues[1].toLong())
                if (connectedSummit != null) {
                    updatedPlaces.add(connectedSummit.getConnectedEntryString(context))
                } else {
                    updatedPlaces.add(place)
                }
            } else {
                updatedPlaces.add(place)
            }
        }
        return updatedPlaces
    }

    fun setConnectedEntries(connectedEntries: MutableList<Summit>, database: AppDatabase) {
        setConnectedEntriesFromPlaces(database, connectedEntries)
        setConnectedEntriesWhichReferenceThisEntry(database, connectedEntries)
    }

    private fun setConnectedEntriesWhichReferenceThisEntry(database: AppDatabase, connectedEntries: MutableList<Summit>) {
        val connectedSummit = database.summitDao()?.getSummitsWithConnectedId("${CONNECTED_ACTIVITY_PREFIX}${activityId}")
        if (connectedSummit != null) {
            connectedEntries.add(connectedSummit)
            connectedSummit.setConnectedEntriesWhichReferenceThisEntry(database, connectedEntries)
        }
    }

    private fun setConnectedEntriesFromPlaces(database: AppDatabase, connectedEntries: MutableList<Summit>) {
        for (place in places) {
            val matchResult = "${CONNECTED_ACTIVITY_PREFIX}([0-9]*)".toRegex().find(place)
            if (matchResult?.groupValues != null) {
                val connectedSummit = database.summitDao()?.getSummitFromActivityId(matchResult.groupValues[1].toLong())
                if (connectedSummit != null) {
                    connectedEntries.add(connectedSummit)
                    connectedSummit.setConnectedEntriesFromPlaces(database, connectedEntries)
                }
            }
        }
    }

    fun setBoundingBoxFromTrack() {
        if (hasGpsTrack()) {
            setGpsTrack()
            if (gpsTrack?.trackGeoPoints?.size?:0 > 0) {
                val boundingBox = BoundingBox.fromGeoPoints(gpsTrack?.trackGeoPoints)
                trackBoundingBox = TrackBoundingBox(boundingBox.latNorth, boundingBox.latSouth, boundingBox.lonWest, boundingBox.lonEast)
            }
        }
    }

    fun isInBoundingBox(boundingBox: BoundingBox): Boolean {
        val latLngLocal = latLng
        if (latLngLocal != null && hasGpsTrack()) {
            return boundingBox.contains(GeoPoint(latLngLocal.latitude, latLngLocal.longitude)) || trackBoundingBox?.intersects(boundingBox) == true
        } else {
            return false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Summit
        return that.kilometers == kilometers && that.getDateAsString() == getDateAsString() && name == that.name && sportType == that.sportType && elevationData == that.elevationData
    }

    fun equalsInAllProperties(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Summit
        return that.kilometers == kilometers && that.getDateAsString() == getDateAsString()
                && name == that.name && sportType == that.sportType
                && elevationData == that.elevationData && velocityData == that.velocityData && comments == that.comments
    }

    override fun hashCode(): Int {
        return Objects.hash(date, name, sportType, elevationData, kilometers)
    }

    companion object {
        const val DATE_FORMAT: String = "yyyy-MM-dd"
        const val DATETIME_FORMAT: String = "yyyy-MM-dd HH:mm:ss"
        const val CONNECTED_ACTIVITY_PREFIX: String = "ac_id:"
        private const val NUMBER_OF_ELEMENTS = 28
        private const val REFERENCE_VALUE_DATE = 946681200000f
        var subDirForGpsTracks: String = "summitbook_tracks"
        var subDirForImages: String = "summitbook_images"

        @Throws(Exception::class)
        fun parseFromCsvFileLine(line: String): Summit {
            val cvsSplitBy = ";"
            val splitLine: Array<String> = line.trim { it <= ' ' }.split(cvsSplitBy.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            checkValidNumberOfElements(splitLine)
            isDateValid(splitLine)
            areNumbersValid(splitLine)
            isSportTypeValid(splitLine)
            val date = parseDate(splitLine[0])
            val elevation = if (splitLine[10].trim { it <= ' ' } != "") splitLine[10].toInt() else 0
            val elevationData = ElevationData.parse(splitLine[6].split(","), elevation)
            val sportType = splitLine[2].let { SportType.valueOf(it) }
            val km: Double = (if (splitLine[7].trim { it <= ' ' } != "") splitLine[7].toDouble() else 0.0)
            val topSpeed: Double = (if (splitLine[9].trim { it <= ' ' } != "") splitLine[9].toDouble() else 0.0)
            val countries = splitLine[3].split(",")
            val places = splitLine[4].split(",")
            val participants = splitLine[13].split(",")
            val activityId = if (splitLine[14].trim { it <= ' ' } != "") splitLine[14].toLong() else System.currentTimeMillis()
            val garminData = if (splitLine[15].trim { it <= ' ' } != "") getGarminData(splitLine) else null
            val latLng = if (splitLine[11].trim { it <= ' ' } != "" && splitLine[12].trim { it <= ' ' } != "") splitLine[11].toDouble().let { LatLng(it, splitLine[12].toDouble()) } else null
            val isFavorite = if (splitLine.size == NUMBER_OF_ELEMENTS) splitLine[27] else splitLine[29]
            return Summit(
                    date,
                    splitLine[1],
                    sportType,
                    countries,
                    places,
                    splitLine[5],
                    elevationData,
                    km,
                    VelocityData.parse(splitLine[8].split(","), topSpeed),
                    latLng?.latitude,latLng?.longitude,
                    participants,
                    isFavorite == "1",
                    mutableListOf(),
                    garminData,
                    null,
                    activityId
            )
        }

        private fun getGarminData(splitLine: Array<String>): GarminData {
            if (splitLine.size == 30) {
                return GarminData(splitLine[15].split(",") as MutableList<String>,
                        splitLine[16].toFloat(), splitLine[17].toFloat(), splitLine[18].toFloat(),
                        PowerData(splitLine[19].toFloat(), splitLine[20].toFloat(), splitLine[21].toFloat(),
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                splitLine[22].toInt(), 0, splitLine[23].toInt(), 0, 0),
                        0, 0,
                        splitLine[24].toFloat(), splitLine[25].toFloat(),
                        splitLine[26].toFloat(), splitLine[27].toFloat(), splitLine[28].toFloat())
            } else {
                return GarminData(splitLine[15].split(",") as MutableList<String>,
                        splitLine[16].toFloat(), splitLine[17].toFloat(), splitLine[18].toFloat(),
                        PowerData.parse(splitLine[19].split(",")),
                        splitLine[20].toInt(), splitLine[21].toInt(),
                        splitLine[22].toFloat(), splitLine[23].toFloat(),
                        splitLine[24].toFloat(), splitLine[25].toFloat(), splitLine[26].toFloat())
            }
        }

        @Throws(Exception::class)
        private fun checkValidNumberOfElements(splitLine: Array<String>) {
            if (splitLine.size != NUMBER_OF_ELEMENTS && splitLine.size != 30) {
                throw Exception("Line " + splitLine.contentToString() + " has " + splitLine.size +
                        " number of elements. Expected are " + NUMBER_OF_ELEMENTS)
            }
        }

        @Throws(Exception::class)
        private fun isDateValid(splitLine: Array<String>) {
            val date = splitLine[0]
            try {
                parseDate(date)
            } catch (e: ParseException) {
                throw Exception("Line " + splitLine.contentToString() + " has invalid date " + date)
            }
        }

        @Throws(ParseException::class)
        fun parseDate(date: String): Date {
            val df: DateFormat = SimpleDateFormat(DATETIME_FORMAT, Locale.ENGLISH)
            df.isLenient = false
            return df.parse(String.format("%s 12:00:00", date)) ?: Date()
        }

        @Throws(Exception::class)
        private fun areNumbersValid(splitLine: Array<String>) {
            try {
                val elevation = if (splitLine[10].trim { it <= ' ' } != "") splitLine[10].toInt() else 0
                ElevationData.parse(splitLine[6].split(","), elevation)
            } catch (e: Exception) {
                throw Exception("Line " + splitLine.contentToString() + " has no valid value for required parameter height meter.")
            }
        }

        @Throws(Exception::class)
        private fun isSportTypeValid(splitLine: Array<String>) {
            val sportType = splitLine[2]
            try {
                SportType.valueOf(sportType)
            } catch (e: Exception) {
                throw Exception("Line " + splitLine.contentToString() + " has no valid sport type " + sportType)
            }
        }

        fun getDateFromFloat(dateAsFloat: Float): Date {
            return Date((dateAsFloat * 1e8 + REFERENCE_VALUE_DATE).toLong())
        }

        fun getCsvHeadline(): String {
            return "Date; Name; SportType; place; country; comments; hm; km; pace; topSpeed; topElevation; lat; lng; participants; activityId; garminid; calories; averageHR; maxHR; powerData; FTP; VO2MAX; aerobicTrainingEffect; anaerobicTrainingEffect; grit; flow; trainingsload; isFavorite".trimIndent() + "\n"
        }

    }

}

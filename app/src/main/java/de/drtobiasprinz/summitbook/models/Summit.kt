package de.drtobiasprinz.summitbook.models

import android.content.Context
import android.content.res.Resources
import androidx.room.*
import de.drtobiasprinz.gpx.TrackPoint
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
        var participants: List<String>, @ColumnInfo(defaultValue = "") var equipments: List<String>,
        var isFavorite: Boolean, @ColumnInfo(defaultValue = "false") var isPeak: Boolean,
        var imageIds: MutableList<Int>, @Embedded var garminData: GarminData?,
        @Embedded var trackBoundingBox: TrackBoundingBox?,
        var activityId: Long = System.currentTimeMillis(),
        @ColumnInfo(defaultValue = "false") var isBookmark: Boolean = false
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @Ignore
    var latLng = lat?.let { lng?.let { it1 -> TrackPoint(it, it1) } }

    @Ignore
    var duration = getWellDefinedDuration()

    @Ignore
    var gpsTrack: GpsTrack? = null

    @Ignore
    var isSelected: Boolean = false

    private fun getWellDefinedDuration(): Double {
        val dur = if (velocityData.avgVelocity > 0) kilometers / velocityData.avgVelocity else 0.0
        return if (dur < 24) {
            dur
        } else {
            0.0
        }
    }

    fun getImagePath(imageId: Int): Path {
        val rootDirectoryImages = File(MainActivity.storage, "$subDirForImages/${id}")
        return Paths.get(rootDirectoryImages.toString(), String.format(Locale.ENGLISH, "%s.jpg", imageId))
    }

    fun getImageUrl(imageId: Int): String {
        val rootDirectoryImages = File(MainActivity.storage, "$subDirForImages/${id}")
        return "file://" + Paths.get(rootDirectoryImages.toString(), String.format(Locale.ENGLISH, "%s.jpg", imageId)).toString()
    }

    fun getNextImagePath(addIdToImageIds: Boolean = false): Path {
        val rootDirectoryImages = File(MainActivity.storage, "$subDirForImages/${id}")
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

    fun getGpsTrackPath(simplified: Boolean = false): Path {
        val folder = MainActivity.storage
        val fileName = if (simplified) "id_${activityId}_simplified.gpx" else "id_${activityId}.gpx"
        return if (isBookmark) {
            Paths.get(folder.toString(), subDirForGpsTracksBookmark, fileName)
        } else {
            Paths.get(folder.toString(), subDirForGpsTracks, fileName)
        }
    }

    fun getGpxPyPath(): Path {
        val fileName = "id_${activityId}_gpxpy.json"
        return if (isBookmark) {
            Paths.get(MainActivity.storage.toString(), subDirForGpsTracksBookmark, fileName)
        } else {
            Paths.get(MainActivity.storage.toString(), subDirForGpsTracks, fileName)
        }
    }

    @Throws(IOException::class)
    fun copyGpsTrackToTempFile(dir: File?): File? {
        val tempFile = File.createTempFile(String.format(Locale.ENGLISH, "id_%s", activityId),
                ".gpx", dir)
        if (hasGpsTrack()) {
            Files.copy(getGpsTrackPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
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

    fun hasGpsTrack(simplified: Boolean = false): Boolean {
        return getGpsTrackPath(simplified).toFile()?.exists() ?: false
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

    fun setGpsTrack(useSimplifiedTrack: Boolean = true, updateTrack: Boolean = false, loadFullTrackAsynchronous: Boolean=false) {
        if (hasGpsTrack()) {
            if (gpsTrack == null || updateTrack) {
                gpsTrack = GpsTrack(getGpsTrackPath(), getGpsTrackPath(simplified = useSimplifiedTrack))
            }
            if (gpsTrack != null && gpsTrack?.hasNoTrackPoints() == true) {
                gpsTrack?.parseTrack(loadFullTrackAsynchronous = loadFullTrackAsynchronous)
            }
            if (gpsTrack?.trackPoints?.isEmpty() == true) {
                if (getGpsTrackPath().toFile().exists()) {
                    getGpsTrackPath().toFile().delete()
                }
                if (getGpsTrackPath(simplified = true).toFile().exists()) {
                    getGpsTrackPath(simplified = true).toFile().delete()
                }
            }
        }
    }

    fun getImageDescription(resources: Resources, index: Int): String {
        return "${getDateAsString()}\n$name\n" +
                (if (elevationData.maxElevation != 0) "${elevationData.maxElevation} ${resources.getString(R.string.masl)}\n" else "") +
                (if (elevationData.elevationGain != 0) "${elevationData.elevationGain} ${resources.getString(R.string.hm)}\n" else "") +
                (if (kilometers != 0.0) "$kilometers ${resources.getString(R.string.km)}\n" else "") +
                "#${index + 1}\n"
    }

    override fun toString(): String {
        return getStringRepresentation()
    }

    fun getStringRepresentation(exportThirdPartyData: Boolean = true, exportCalculatedData: Boolean = true): String {
        val lat = latLng?.lat?.toString() ?: ""
        val lng = latLng?.lon?.toString() ?: ""
        var entryToString = getDateAsString() + ';' +
                name + ';' +
                sportType + ';' +
                places.joinToString(",") + ';' +
                countries.joinToString(",") + ';' +
                comments + ';' +
                (if (exportCalculatedData) elevationData.toString() else elevationData.elevationGain) + ';' +
                kilometers + ';' +
                (if (exportCalculatedData) velocityData.toString() else velocityData.avgVelocity) + ';' +
                velocityData.maxVelocity + ';' +
                elevationData.maxElevation + ';' +
                lat + ';' +
                lng + ';' +
                participants.joinToString(",") + ',' + equipments.joinToString(",") { "${it}${EQUIPMENT_SUFFIX}" } + ';' +
                activityId + ';'
        entryToString += if (exportThirdPartyData && garminData != null) {
            garminData.toString()
        } else {
            GarminData.emptyLine(exportThirdPartyData)
        }
        entryToString += if (exportThirdPartyData) ";" else ""
        entryToString += if (isFavorite) "1\n" else "0\n"
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
        val connectedSummit = database.summitDao()?.getSummitsWithConnectedId("%${CONNECTED_ACTIVITY_PREFIX}${activityId}%")
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
            if (gpsTrack?.trackGeoPoints?.size ?: 0 > 0) {
                val boundingBox = BoundingBox.fromGeoPoints(gpsTrack?.trackGeoPoints)
                trackBoundingBox = TrackBoundingBox(boundingBox.latNorth, boundingBox.latSouth, boundingBox.lonWest, boundingBox.lonEast)
            }
        }
    }

    fun isInBoundingBox(boundingBox: BoundingBox): Boolean {
        val latLngLocal = latLng
        return if (latLngLocal != null && hasGpsTrack()) {
            boundingBox.contains(GeoPoint(latLngLocal.lat, latLngLocal.lon)) || trackBoundingBox?.intersects(boundingBox) == true
        } else {
            false
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
        const val EQUIPMENT_SUFFIX: String = ":eq"
        const val DATETIME_FORMAT: String = "yyyy-MM-dd HH:mm:ss"
        const val CONNECTED_ACTIVITY_PREFIX: String = "ac_id:"
        const val SUMMIT_ID_EXTRA_IDENTIFIER = "SUMMIT_ID"
        private const val NUMBER_OF_ELEMENTS_WITH_THIRD_PARTY = 28
        private const val NUMBER_OF_ELEMENTS_WITHOUT_THIRD_PARTY = 16
        private const val REFERENCE_VALUE_DATE = 946681200000f

        var subDirForGpsTracks: String = "summitbook_tracks"
        var subDirForGpsTracksBookmark: String = "summitbook_tracks_bookmark"
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
            val participantsAndEquipments = splitLine[13].split(",")
            val activityId = if (splitLine[14].trim { it <= ' ' } != "") splitLine[14].toLong() else System.currentTimeMillis()
            val garminData = getGarminData(splitLine)
            val latLng = if (splitLine[11].trim { it <= ' ' } != "" && splitLine[12].trim { it <= ' ' } != "") TrackPoint(splitLine[11].toDouble(), splitLine[12].toDouble()) else null
            val isFavoriteAndOrPeak = (if (splitLine.size == NUMBER_OF_ELEMENTS_WITH_THIRD_PARTY) splitLine[27] else (if (splitLine.size == NUMBER_OF_ELEMENTS_WITHOUT_THIRD_PARTY) splitLine[15] else splitLine[29])).split(",")
            val isFavorite = if (isFavoriteAndOrPeak.isEmpty()) false else isFavoriteAndOrPeak[0] == "1"
            val isPeak = if (isFavoriteAndOrPeak.size <2) false else isFavoriteAndOrPeak[1] == "1"
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
                    latLng?.lat, latLng?.lon,
                    participantsAndEquipments.filter { !it.contains(EQUIPMENT_SUFFIX) },
                    participantsAndEquipments.filter { it.contains(EQUIPMENT_SUFFIX) }.map { it.replace(EQUIPMENT_SUFFIX, "") },
                    isFavorite,
                    isPeak,
                    mutableListOf(),
                    garminData,
                    null,
                    activityId
            )
        }

        private fun getGarminData(splitLine: Array<String>): GarminData? {
            if (splitLine.size == NUMBER_OF_ELEMENTS_WITHOUT_THIRD_PARTY || splitLine[15].trim { it <= ' ' } == "") {
                return null
            } else {
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
        }

        @Throws(Exception::class)
        private fun checkValidNumberOfElements(splitLine: Array<String>) {
            if (splitLine.size != NUMBER_OF_ELEMENTS_WITH_THIRD_PARTY && splitLine.size != 30 && splitLine.size != NUMBER_OF_ELEMENTS_WITHOUT_THIRD_PARTY) {
                throw Exception("Line ${splitLine.contentToString()} has ${splitLine.size} number " +
                        "of elements. Expected are $NUMBER_OF_ELEMENTS_WITH_THIRD_PARTY or if no " +
                        "third party data was added $NUMBER_OF_ELEMENTS_WITHOUT_THIRD_PARTY")
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

        fun getCsvHeadline(resources: Resources, withThirdPartyData: Boolean = true): String {
            return ("${resources.getString(R.string.tour_date)}; " +
                    "${resources.getString(R.string.name)}; " +
                    "${resources.getString(R.string.sport_type)}; " +
                    "${resources.getString(R.string.place_hint)}; " +
                    "${resources.getString(R.string.country_hint)}; " +
                    "${resources.getString(R.string.comment_hint)}; " +
                    "${resources.getString(R.string.height_meter)}; " +
                    "${resources.getString(R.string.kilometers_hint)} (${resources.getString(R.string.km)}); " +
                    "${resources.getString(R.string.pace_hint)} (${resources.getString(R.string.kmh)}); " +
                    "${resources.getString(R.string.top_speed)} (${resources.getString(R.string.kmh)}); " +
                    "${resources.getString(R.string.top_elevation_hint)} (${resources.getString(R.string.hm)}); " +
                    "${resources.getString(R.string.latitude)}; " +
                    "${resources.getString(R.string.longitude)}; " +
                    "${resources.getString(R.string.participants)}; " +
                    "activityId; " +
                    (if (withThirdPartyData) GarminData.getCsvHeadline(resources) else "") +
                    "isFavorite").trimIndent() + "\n"
        }

        fun getCsvDescription(resources: Resources, withThirdPartyData: Boolean = true): String {
            return ("${resources.getString(R.string.required)}; " +
                    "${resources.getString(R.string.required)}; " +
                    "${resources.getString(R.string.required)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.required)}; " +
                    "${resources.getString(R.string.required)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    (if (withThirdPartyData) GarminData.getCsvDescription(resources) else "") +
                    "${resources.getString(R.string.required)}; ").trimIndent() + "\n"
        }

    }

}

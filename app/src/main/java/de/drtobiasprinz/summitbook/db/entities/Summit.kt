package de.drtobiasprinz.summitbook.db.entities

import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.room.*
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.*
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_VERSION
import de.drtobiasprinz.summitbook.utils.Constants
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
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
import kotlin.io.path.name
import kotlin.math.roundToInt


@Entity(tableName = Constants.SUMMITS_TABLE)
class Summit(
    var date: Date = Date(),
    var name: String = "",
    var sportType: SportType = SportType.Other,
    var places: List<String> = mutableListOf(),
    var countries: List<String> = mutableListOf(),
    var comments: String = "",
    @Embedded var elevationData: ElevationData = ElevationData(0, 0),
    var kilometers: Double = 0.0,
    @Embedded var velocityData: VelocityData = VelocityData(0.0, 0.0),
    var lat: Double? = null,
    var lng: Double? = null,
    var participants: List<String> = mutableListOf(),
    @ColumnInfo(defaultValue = "") var equipments: List<String> = mutableListOf(),
    var isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "false") var isPeak: Boolean = false,
    var imageIds: MutableList<Int> = mutableListOf(),
    @Embedded var garminData: GarminData? = null,
    @Embedded var trackBoundingBox: TrackBoundingBox? = null,
    var activityId: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") var duration: Int = 0,
    @ColumnInfo(defaultValue = "false") var isBookmark: Boolean = false,
    @ColumnInfo(defaultValue = "false") var hasTrack: Boolean = false,
    @ColumnInfo(defaultValue = "false") var ignoreSimplifyingTrack: Boolean = false,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @Ignore
    var latLng = lat?.let { lng?.let { it1 -> GeoPoint(it, it1) } }

    @Ignore
    var gpsTrack: GpsTrack? = null

    @Ignore
    var isSelected: Boolean = false

    @Ignore
    var updated: Int = 0

    @Ignore
    var segmentInfo: List<Triple<SegmentEntry, SegmentDetails, Int>> = mutableListOf()

    @Ignore
    var hasPowerRecord: Boolean = false

    @Ignore
    var bestPositionInSegment: Int = -1

    fun getImagePath(imageId: Int): Path {
        return Paths.get(
            getRootDirectoryImages().toString(),
            String.format(Locale.ENGLISH, "%s.jpg", imageId)
        )
    }

    fun getImageUrl(imageId: Int): String {
        return "file://" + Paths.get(
            getRootDirectoryImages().toString(),
            String.format(Locale.ENGLISH, "%s.jpg", imageId)
        ).toString()
    }

    private fun getRootDirectoryImages(): File {
        val rootDirectoryImages = File(MainActivity.storage, "$subDirForImages/${activityId}")
        if (!rootDirectoryImages.exists()) {
            rootDirectoryImages.mkdirs()
        }
        return rootDirectoryImages
    }

    fun getNextImagePath(addIdToImageIds: Boolean = false): Path {
        val rootDirectoryImages = getRootDirectoryImages()
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
        val folder = if (simplified) MainActivity.cache else MainActivity.storage
        val fileName = if (simplified) "id_${activityId}_simplified.gpx" else "id_${activityId}.gpx"
        val baseFolder = if (isBookmark) {
            File(folder, subDirForGpsTracksBookmark)
        } else {
            File(folder, subDirForGpsTracks)
        }
        if (!baseFolder.exists()) {
            baseFolder.mkdirs()
        }
        return Paths.get(baseFolder.toString(), fileName)
    }

    fun getYamlExtensionsFile(): File {
        return File(File(MainActivity.cache, subDirForGpsTracks), getGpsTrackPath().name.replace(".gpx", "_extensions.yaml"))
    }

    fun getGpxPyPath(): Path {
        val fileName = "id_${activityId}_gpxpy.json"
        return if (isBookmark) {
            Paths.get(MainActivity.cache.toString(), subDirForGpsTracksBookmark, fileName)
        } else {
            Paths.get(MainActivity.cache.toString(), subDirForGpsTracks, fileName)
        }
    }

    @Throws(IOException::class)
    fun copyGpsTrackToTempFile(cacheDir: File?): File? {
        val tempFile = File(
            cacheDir,
            String.format(Locale.ENGLISH, "Summit_%s.gpx", name)
        )
        if (hasGpsTrack(true)) {
            Files.copy(
                getGpsTrackPath(true),
                tempFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            return tempFile
        } else if (hasGpsTrack()) {
            Files.copy(getGpsTrackPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            return tempFile
        }
        return null
    }

    fun getExportImagePath(): String {
        return Paths.get(
            subDirForImages,
            String.format("%s_%s.jpg", getDateAsString(), name.replace(" ".toRegex(), "_"))
        ).toString()
    }

    fun getExportImageFolderPath(): String {
        return Paths.get(
            subDirForImages,
            "${getDateAsString()}_${name.replace(" ".toRegex(), "_")}"
        ).toString()
    }

    fun getExportImagePath(id: Int): String {
        return Paths.get(getExportImageFolderPath(), "${id}.jpg").toString()
    }

    fun getExportTrackPath(): String {
        return Paths.get(
            subDirForGpsTracks,
            String.format(
                "%s_%s.gpx",
                getDateAsString(),
                name.replace(" ".toRegex(), "_").replace("/".toRegex(), "_")
            )
        ).toString()
    }

    fun hasImagePath(): Boolean {
        return imageIds.isNotEmpty()
    }

    fun hasGpsTrack(simplified: Boolean = false): Boolean {
        val checkIfHasTrack = getGpsTrackPath(simplified).toFile()?.exists() ?: false
        if (!simplified) {
            hasTrack = checkIfHasTrack
        }
        return checkIfHasTrack
    }

    fun hasTrackData(): Boolean {
        val checkIfHasTrack = getYamlExtensionsFile().exists() && getGpxPyPath().toFile().exists()
        return checkIfHasTrack
    }

    fun isDuplicate(allExistingEntries: List<Summit>?): Boolean {
        var isInList = false
        if (allExistingEntries != null) {
            for (entry in allExistingEntries) {
                if (entry.equalsInBaseProperties(this)) {
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

    fun setGpsTrack(
        useSimplifiedTrack: Boolean = true,
        updateTrack: Boolean = false,
        deleteEmptyTrack: Boolean = false
    ) {
        if (hasGpsTrack()) {
            if (gpsTrack == null || updateTrack) {
                gpsTrack =
                    GpsTrack(getGpsTrackPath(), getGpsTrackPath(simplified = useSimplifiedTrack), getYamlExtensionsFile())
            }
            if (gpsTrack?.hasNoTrackPoints() == true) {
                gpsTrack?.parseTrack(useSimplifiedIfExists = useSimplifiedTrack)
            }
            if (gpsTrack?.trackPoints?.isEmpty() == true && deleteEmptyTrack) {
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
                (if (elevationData.maxElevation != 0) "${elevationData.maxElevation} ${
                    resources.getString(
                        R.string.masl
                    )
                }\n" else "") +
                (if (elevationData.elevationGain != 0) "${elevationData.elevationGain} ${
                    resources.getString(
                        R.string.hm
                    )
                }\n" else "") +
                (if (kilometers != 0.0) "$kilometers ${resources.getString(R.string.km)}\n" else "") +
                "#${index + 1}\n"
    }

    override fun toString(): String {
        return getStringRepresentation()
    }

    fun getStringRepresentation(): String {
        return getDateAsString() + ';' +
                name + ';' +
                sportType + ';' +
                activityId + ';' +
                kilometers + ';' +
                duration + ';' +
                elevationData.elevationGain + ';' +
                elevationData.maxElevation + ';' +
                velocityData.maxVelocity + ';' +
                (if (lat != null && lat != 0.0) lat else "") + ';' +
                (if (lng != null && lng != 0.0) lng else "") + ';' +
                (if (isFavorite) "1" else "0") + ';' +
                (if (isPeak) "1" else "0") + ';' +
                comments.replace(";", ",").replace("\n", ",") + ';' +
                participants.joinToString(",") + ';' +
                equipments.joinToString(",") + ';' +
                places.joinToString(",") + ';' +
                countries.joinToString(",") + '\n'
    }

    fun toReadableString(context: Context): String {
        return "${context.getString(R.string.tour_date)}: ${getDateAsString()}, ${
            context.getString(
                R.string.name
            )
        }: ${name}, " +
                "${context.getString(R.string.type)}: $sportType, ${elevationData.elevationGain} hm, $kilometers km"
    }

    fun getConnectedEntryString(context: Context): String {
        return "${context.getString(R.string.end_of)} $name"
    }


    fun getPlacesWithConnectedEntryString(context: Context, summits: List<Summit>): List<String> {
        val updatedPlaces = mutableListOf<String>()
        for (place in places) {
            val matchResult = "$CONNECTED_ACTIVITY_PREFIX(\\d*)".toRegex().find(place)
            if (matchResult?.groupValues != null) {
                val connectedSummit =
                    summits.firstOrNull { it.activityId == matchResult.groupValues[1].toLong() }
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

    fun getConnectedEntries(summits: List<Summit>?): MutableList<Summit> {
        val connectedEntries = getConnectedEntriesFromPlaces(summits)
        connectedEntries.addAll(getConnectedEntriesWhichReferenceThisEntry(summits))
        return connectedEntries
    }

    private fun getConnectedEntriesWhichReferenceThisEntry(summits: List<Summit>?): MutableList<Summit> {
        val connectedEntries = mutableListOf<Summit>()
        val connectedSummit =
            summits?.firstOrNull { it.places.contains("$CONNECTED_ACTIVITY_PREFIX${activityId}") }
        if (connectedSummit != null) {
            connectedEntries.add(connectedSummit)
            connectedEntries.addAll(
                connectedSummit.getConnectedEntriesWhichReferenceThisEntry(
                    summits
                )
            )
        }
        return connectedEntries
    }

    private fun getConnectedEntriesFromPlaces(summits: List<Summit>?): MutableList<Summit> {
        val connectedEntries = mutableListOf<Summit>()
        for (place in places) {
            val matchResult = "$CONNECTED_ACTIVITY_PREFIX(\\d*)".toRegex().find(place)
            if (matchResult?.groupValues != null) {
                val connectedSummit =
                    summits?.firstOrNull { it.activityId == (matchResult.groupValues[1].toLong()) }
                if (connectedSummit != null) {
                    connectedEntries.add(connectedSummit)
                    connectedEntries.addAll(connectedSummit.getConnectedEntriesFromPlaces(summits))
                }
            }
        }
        return connectedEntries
    }

    fun setBoundingBoxFromTrack() {
        if (hasGpsTrack()) {
            setGpsTrack()
            if ((gpsTrack?.trackGeoPoints?.size ?: 0) > 0) {
                val boundingBox = BoundingBox.fromGeoPoints(gpsTrack?.trackGeoPoints)
                trackBoundingBox = TrackBoundingBox(
                    boundingBox.latNorth,
                    boundingBox.latSouth,
                    boundingBox.lonWest,
                    boundingBox.lonEast
                )
            }
        }
    }

    fun getAverageVelocity(): Double {
        return if (duration > 0) {
            kilometers / (duration / 3600.0)
        } else {
            0.0
        }
    }

    fun isInBoundingBox(boundingBox: BoundingBox): Boolean {
        val latLngLocal = latLng
        return if (latLngLocal != null && hasGpsTrack()) {
            boundingBox.contains(
                GeoPoint(
                    latLngLocal.latitude,
                    latLngLocal.longitude
                )
            ) || trackBoundingBox?.intersects(boundingBox) == true
        } else {
            false
        }
    }

    fun updateSegmentInfo(segments: List<Segment>) {
        val segmentsForSummit =
            segments.filter { summit ->
                summit.segmentEntries
                    .any { entry -> entry.activityId == this.activityId }
            }
        if (segmentsForSummit.isNotEmpty()) {
            val list = mutableListOf<Triple<SegmentEntry, SegmentDetails, Int>>()
            segmentsForSummit.forEach { segment ->
                segment.segmentEntries.sortBy { entry -> entry.duration }
                val relevantEntries = segment.segmentEntries
                    .filter { entry -> entry.activityId == this.activityId }
                relevantEntries.forEach { segmentEntry ->
                    list.add(
                        Triple(
                            segmentEntry, segment.segmentDetails,
                            segment.segmentEntries.indexOf(segmentEntry) + 1
                        )
                    )
                }
            }
            segmentInfo = list
        }
    }


    override fun hashCode(): Int {
        return Objects.hash(date, name, sportType, elevationData, kilometers)
    }

    fun equalsInBaseProperties(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Summit
        Log.i("TAG", that.toString())
        Log.i("TAG", this.toString())
        return that.kilometers == kilometers &&
                that.getDateAsString() == getDateAsString() &&
                name == that.name &&
                sportType == that.sportType &&
                velocityData.maxVelocity == that.velocityData.maxVelocity &&
                elevationData.elevationGain == that.elevationData.elevationGain &&
                elevationData.maxElevation == that.elevationData.maxElevation &&
                imageIds == that.imageIds
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Summit

        if (date != other.date) return false
        if (name != other.name) return false
        if (sportType != other.sportType) return false
        if (places != listOf("") && other.places != listOf("") && places != other.places) return false
        if (countries != listOf("") && other.countries != listOf("") && countries != other.countries) return false
        if (comments != other.comments) return false
        if (elevationData != other.elevationData) return false
        if (kilometers != other.kilometers) return false
        if (velocityData != other.velocityData) return false
        if (lat != other.lat) return false
        if (lng != other.lng) return false
        if (participants != listOf("") && other.participants != listOf("") && participants != other.participants) return false
        if (equipments != listOf("") && other.equipments != listOf("") && equipments != other.equipments) return false
        if (imageIds != other.imageIds) return false
        if (garminData != other.garminData) return false
        if (activityId != other.activityId) return false
        if (isBookmark != other.isBookmark) return false
        if (hasTrack != other.hasTrack) return false
        if (latLng != other.latLng) return false
        if (duration != other.duration) return false
        if (gpsTrack != other.gpsTrack) return false
        if (isSelected != other.isSelected) return false
        return updated == other.updated
    }

    fun clone(): Summit {
        val summit = Summit(
            date,
            name,
            sportType,
            places,
            countries,
            comments,
            elevationData.clone(),
            kilometers,
            velocityData.clone(),
            lat,
            lng,
            participants,
            equipments,
            isFavorite,
            isPeak,
            imageIds,
            garminData,
            trackBoundingBox,
            activityId,
            duration,
            isBookmark,
            hasTrack,
            ignoreSimplifyingTrack
        )
        summit.id = id
        return summit
    }

    companion object {
        const val DATE_FORMAT: String = "yyyy-MM-dd"
        private const val EQUIPMENT_SUFFIX: String = ":eq"
        const val DATETIME_FORMAT_SIMPLE: String = "yyyy-MM-dd HH:mm:ss"
        const val DATETIME_FORMAT_COMPLEX: String = "yyyy-MM-dd'T'HH:mm:ss.s"
        const val CONNECTED_ACTIVITY_PREFIX: String = "ac_id:"
        const val SUMMIT_ID_EXTRA_IDENTIFIER = "SUMMIT_ID"
        private const val NUMBER_OF_ELEMENTS_WITH_THIRD_PARTY = 28
        private const val NUMBER_OF_ELEMENTS_WITHOUT_THIRD_PARTY = 16
        const val REFERENCE_VALUE_DATE: Long = 946681200000

        var subDirForGpsTracks: String = "summitbook_tracks"
        var subDirForGpsTracksBookmark: String = "summitbook_tracks_bookmark"
        var subDirForImages: String = "summitbook_images"

        fun parseFromCsvFileLine(line: String, version: String): Summit {
            if (version == CSV_FILE_VERSION) {
                val listPatternOnce = "[^;]+"
                val listPatternNullOrOnce = "[^;]*"
                val regex =
                    """(?<date>(\d{4}-\d{2}-\d{2}));(?<name>($listPatternOnce));(?<sportType>(\w+));(?<activityId>(\d+));(?<kilometers>([\d.]+));(?<duration>([\d.]*));(?<elevationGain>([\d.]+));(?<maxElevation>([\d.]+));(?<maxVelocity>([\d.]+));(?<lat>([\d.]*));(?<long>([\d.]*));(?<isFavorite>([01]));(?<isPeak>([01]));(?<comments>(.*));(?<participants>($listPatternNullOrOnce));(?<equipments>($listPatternNullOrOnce));(?<places>($listPatternNullOrOnce));(?<countries>($listPatternNullOrOnce))""".toRegex()
                val matchResult = regex.find(line.replace("\n", ""))
                return if (matchResult != null) {
                    Summit(
                        parseDate(matchResult.groups["date"]!!.value),
                        matchResult.groups["name"]!!.value,
                        SportType.valueOf(matchResult.groups["sportType"]!!.value),
                        if (matchResult.groups["places"]!!.value != "") matchResult.groups["places"]!!.value.split(
                            ","
                        ) else emptyList(),
                        if (matchResult.groups["countries"]!!.value != "") matchResult.groups["countries"]!!.value.split(
                            ","
                        ) else emptyList(),
                        matchResult.groups["comments"]!!.value,
                        ElevationData(
                            maxElevation = matchResult.groups["maxElevation"]!!.value.toInt(),
                            elevationGain = matchResult.groups["elevationGain"]!!.value.toInt()
                        ),
                        matchResult.groups["kilometers"]!!.value.toDouble(),
                        VelocityData(matchResult.groups["maxVelocity"]!!.value.toDouble()),
                        if (matchResult.groups["lat"]!!.value != "") matchResult.groups["lat"]!!.value.toDouble() else null,
                        if (matchResult.groups["long"]!!.value != "") matchResult.groups["long"]!!.value.toDouble() else null,
                        if (matchResult.groups["participants"]!!.value != "") matchResult.groups["participants"]!!.value.split(
                            ","
                        ) else emptyList(),
                        if (matchResult.groups["equipments"]!!.value != "") matchResult.groups["equipments"]!!.value.split(
                            ","
                        ) else emptyList(),
                        matchResult.groups["isFavorite"]!!.value == "1",
                        matchResult.groups["isPeak"]!!.value == "1",
                        activityId = if (matchResult.groups["activityId"]!!.value != "") matchResult.groups["activityId"]!!.value.toLong() else System.currentTimeMillis(),
                        duration = matchResult.groups["duration"]!!.value.toInt()
                    )
                } else {
                    parseFromCsvFileLine(line)
                }
            } else {
                return parseFromCsvFileLine(line)
            }

        }

        @Throws(Exception::class)
        fun parseFromCsvFileLine(line: String): Summit {
            val cvsSplitBy = ";"
            val splitLine: Array<String> =
                line.trim { it <= ' ' }.split(cvsSplitBy.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            checkValidNumberOfElements(splitLine)
            isDateValid(splitLine)
            areNumbersValid(splitLine)
            isSportTypeValid(splitLine)
            val date = parseDate(splitLine[0])
            val elevation = if (splitLine[10].trim { it <= ' ' } != "") splitLine[10].toInt() else 0
            val elevationData = ElevationData.parse(splitLine[6].split(","), elevation)
            val sportType = splitLine[2].let { SportType.valueOf(it) }
            val km: Double =
                (if (splitLine[7].trim { it <= ' ' } != "") splitLine[7].toDouble() else 0.0)
            val topSpeed: Double =
                (if (splitLine[9].trim { it <= ' ' } != "") splitLine[9].toDouble() else 0.0)
            val countries = splitLine[3].split(",")
            val places = splitLine[4].split(",")
            val participantsAndEquipments = splitLine[13].split(",")
            val activityId =
                if (splitLine[14].trim { it <= ' ' } != "") splitLine[14].toLong() else System.currentTimeMillis()
            val garminData = getGarminData(splitLine)
            val latLng =
                if (splitLine[11].trim { it <= ' ' } != "" && splitLine[12].trim { it <= ' ' } != "") {
                    TrackPoint
                        .Builder()
                        .setLatitude(splitLine[11].toDouble())
                        .setLongitude(splitLine[12].toDouble())
                        .build()
                } else null
            val isFavoriteAndOrPeak =
                (if (splitLine.size == NUMBER_OF_ELEMENTS_WITH_THIRD_PARTY) {
                    splitLine[27]
                } else (if (splitLine.size == NUMBER_OF_ELEMENTS_WITHOUT_THIRD_PARTY) {
                    splitLine[15]
                } else {
                    splitLine[29]
                })).split(",")
            val isFavorite =
                if (isFavoriteAndOrPeak.isEmpty()) false else isFavoriteAndOrPeak[0] == "1"
            val isPeak = if (isFavoriteAndOrPeak.size < 2) false else isFavoriteAndOrPeak[1] == "1"
            val velocityDataSplit = splitLine[8].split(",")
            val avgVelocity = velocityDataSplit[0].toDouble()
            val duration = if (avgVelocity == 0.0) 0 else ((km / avgVelocity) * 3600.0).roundToInt()
            val summit = Summit(
                date,
                splitLine[1],
                sportType,
                countries,
                places,
                splitLine[5],
                elevationData,
                km,
                VelocityData.parse(splitLine[8].split(","), topSpeed),
                latLng?.latitude, latLng?.longitude,
                isFavorite = isFavorite,
                isPeak = isPeak,
                garminData = garminData,
                activityId = activityId,
                duration = duration
            )
            val participants = participantsAndEquipments.filter { !it.contains(EQUIPMENT_SUFFIX) }
            val equipments =
                participantsAndEquipments.filter { it.contains(EQUIPMENT_SUFFIX) }.map {
                    it.replace(
                        EQUIPMENT_SUFFIX, ""
                    )
                }
            if (participants.isNotEmpty() && participants[0] != "") {
                summit.participants = participants
            }
            if (equipments.isNotEmpty() && equipments[0] != "") {
                summit.equipments = equipments
            }
            return summit
        }

        private fun getGarminData(splitLine: Array<String>): GarminData? {
            if (splitLine.size == NUMBER_OF_ELEMENTS_WITHOUT_THIRD_PARTY || splitLine[15].trim { it <= ' ' } == "") {
                return null
            } else {
                if (splitLine.size == 30) {
                    return GarminData(
                        splitLine[15].split(",") as MutableList<String>,
                        splitLine[16].toFloat(), splitLine[17].toFloat(), splitLine[18].toFloat(),
                        PowerData(
                            splitLine[19].toFloat(),
                            splitLine[20].toFloat(),
                            splitLine[21].toFloat(),
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            splitLine[22].toInt(),
                            0,
                            splitLine[23].toInt(),
                            0,
                            0
                        ),
                        0, 0f,
                        splitLine[24].toFloat(), splitLine[25].toFloat(),
                        splitLine[26].toFloat(), splitLine[27].toFloat(), splitLine[28].toFloat()
                    )
                } else {
                    return GarminData(
                        splitLine[15].split(",") as MutableList<String>,
                        splitLine[16].toFloat(), splitLine[17].toFloat(), splitLine[18].toFloat(),
                        PowerData.parse(splitLine[19].split(",")),
                        splitLine[20].toInt(), splitLine[21].toFloat(),
                        splitLine[22].toFloat(), splitLine[23].toFloat(),
                        splitLine[24].toFloat(), splitLine[25].toFloat(), splitLine[26].toFloat()
                    )
                }
            }
        }

        @Throws(Exception::class)
        private fun checkValidNumberOfElements(splitLine: Array<String>) {
            if (splitLine.size != NUMBER_OF_ELEMENTS_WITH_THIRD_PARTY && splitLine.size != 30 && splitLine.size != NUMBER_OF_ELEMENTS_WITHOUT_THIRD_PARTY) {
                throw Exception(
                    "Line ${splitLine.contentToString()} has ${splitLine.size} number " +
                            "of elements. Expected are $NUMBER_OF_ELEMENTS_WITH_THIRD_PARTY or if no " +
                            "third party data was added $NUMBER_OF_ELEMENTS_WITHOUT_THIRD_PARTY"
                )
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
            val df: DateFormat = SimpleDateFormat(DATETIME_FORMAT_SIMPLE, Locale.getDefault())
            df.isLenient = false
            return df.parse(String.format("%s 07:00:00", date)) ?: Date()
        }

        @Throws(Exception::class)
        private fun areNumbersValid(splitLine: Array<String>) {
            try {
                val elevation =
                    if (splitLine[10].trim { it <= ' ' } != "") splitLine[10].toInt() else 0
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

        fun getCsvHeadline(resources: Resources): String {
            return ("${resources.getString(R.string.tour_date)}; " +
                    "${resources.getString(R.string.name)}; " +
                    "${resources.getString(R.string.sport_type)}; " +
                    "${resources.getString(R.string.place_hint)}; " +
                    "${resources.getString(R.string.country_hint)}; " +
                    "${resources.getString(R.string.comment_hint)}; " +
                    "${resources.getString(R.string.elevationGain)}; " +
                    "${resources.getString(R.string.kilometers_hint)} (${resources.getString(R.string.km)}); " +
                    "${resources.getString(R.string.duration)} (${resources.getString(R.string.sec)}); " +
                    "${resources.getString(R.string.top_speed)} (${resources.getString(R.string.kmh)}); " +
                    "${resources.getString(R.string.top_elevation_hint)} (${resources.getString(R.string.hm)}); " +
                    "${resources.getString(R.string.latitude)}; " +
                    "${resources.getString(R.string.longitude)}; " +
                    "${resources.getString(R.string.participants)}; " +
                    "activityId; " +
                    "isFavorite").trimIndent() + "\n"
        }

        fun getCsvDescription(resources: Resources): String {
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
                    "${resources.getString(R.string.required)}; ").trimIndent() + "\n"
        }

        fun parseCalculatedDataFromCsvFileLineAndSave(
            line: String,
            summits: MutableList<Summit>,
            saveSummit: (Boolean, Summit) -> Unit
        ): Boolean {
            val activityId = line.split(";").first().toLong()
            val summit = summits.find { it.activityId == activityId }
            if (summit != null) {
                val updatedVelocityData = summit.velocityData.parseCalculatedData(
                    line.split(";")[2].split(","),
                )
                val updatedElevationData = summit.elevationData.parseCalculatedData(
                    line.split(";")[1].split(",")
                )
                if (updatedVelocityData || updatedElevationData) {
                    saveSummit(true, summit)
                    return true
                }
            }
            return false
        }

    }

}

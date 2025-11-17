package de.drtobiasprinz.summitbook.utils

import android.content.Context
import android.util.Log
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.TrackBoundingBox
import de.drtobiasprinz.summitbook.models.ExtensionFromYaml
import de.drtobiasprinz.summitbook.models.ExtensionsFromYaml
import de.drtobiasprinz.summitbook.models.LocationInfo
import de.drtobiasprinz.summitbook.models.RoadInfo
import de.drtobiasprinz.summitbook.models.RoadType
import de.drtobiasprinz.summitbook.models.Surface
import de.drtobiasprinz.summitbook.ui.utils.GpsUtils
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import net.mamoe.yamlkt.Yaml
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Tile
import org.mapsforge.map.datastore.MapReadResult
import org.mapsforge.map.datastore.Way
import org.mapsforge.map.reader.MapFile
import org.osmdroid.util.GeoPoint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.time.measureTime


class OfflineMapAnalyzer(var mapFiles: List<MapFile>, var searchRadiusMeters: Double) {

    fun getRoadTypeSummaryFromTrackPoints(
        trackPointsWithExtension: List<Pair<TrackPoint, ExtensionFromYaml>>?,
    ): Pair<Map<Surface, Int>, Map<RoadType, Int>> {
        val distancePerSurface: MutableMap<Surface, Double> =
            Surface.entries.associateWith { 0.0 }.toMutableMap()
        val distancePerRoadTypes: MutableMap<RoadType, Double> =
            RoadType.entries.associateWith { 0.0 }.toMutableMap()
        if (trackPointsWithExtension != null) {
            val time = measureTime {
                val roadInfos = extractRoadInfos(trackPointsWithExtension)
                filterWronglySelectedRoadTypes(roadInfos, trackPointsWithExtension)
                setDistances(trackPointsWithExtension, distancePerSurface, distancePerRoadTypes)
            }
            Log.i(TAG, "getRoadTypeAtPosition took $time")
        }
        val surfaceSummary = distancePerSurface.map { it.key to it.value.roundToInt() }.toMap()
        val roadTypeSummary = distancePerRoadTypes.map { it.key to it.value.roundToInt() }.toMap()
        Log.i(
            TAG, "distancePerSurface: $surfaceSummary and distancePerRoadType: $roadTypeSummary"
        )
        return Pair(surfaceSummary, roadTypeSummary)
    }

    fun getInfosForLocation(
        geoPoint: GeoPoint,
    ): Pair<RoadInfo?, LocationInfo?> {
        for (mapFile in mapFiles) {
            val roadInfo = getRoadInfoForLatLong(
                LatLong(
                    geoPoint.latitude, geoPoint.longitude
                ), mapFile
            )
            val locationInfo = getClosestLocationInfo(
                LatLong(
                    geoPoint.latitude, geoPoint.longitude
                ), mapFile
            )
            if (roadInfo != null || locationInfo != null) {
                return Pair(roadInfo, locationInfo)
            }
        }
        return Pair(null, null)
    }

    /**
     * Check if a given TrackBoundingBox overlaps with any of the available map files
     * @param trackBoundingBox The bounding box to check
     * @return true if the bounding box overlaps with at least one map file, false otherwise
     */
    fun hasMapCoverageForBoundingBox(trackBoundingBox: TrackBoundingBox): Boolean {
        if (mapFiles.isEmpty()) {
            Log.w(TAG, "No map files available to check coverage.")
            return false
        }

        for (mapFile in mapFiles) {
            try {
                val mapBoundingBox = mapFile.mapFileInfo.boundingBox

                val osmBoundingBox = org.osmdroid.util.BoundingBox(
                    mapBoundingBox.maxLatitude,
                    mapBoundingBox.maxLongitude,
                    mapBoundingBox.minLatitude,
                    mapBoundingBox.minLongitude
                )

                if (trackBoundingBox.intersects(osmBoundingBox)) {
                    Log.d(TAG, "Track bounding box overlaps with map file: ${mapFile.mapFileInfo.fileVersion}")
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking map coverage", e)
            }
        }

        Log.w(TAG, "Track bounding box does not overlap with any available map files.")
        return false
    }

    private fun extractRoadInfos(
        trackPointsWithExtension: List<Pair<TrackPoint, ExtensionFromYaml>>,
    ): MutableList<RoadInfo?> {
        if (mapFiles.isEmpty()) {
            Log.w(TAG, "No map files available to extract road infos.")
            return MutableList(trackPointsWithExtension.size) { null }
        }

        val roadInfos: MutableList<RoadInfo?> = MutableList(trackPointsWithExtension.size) { null }
        try {
            for ((index, trackPointPair) in trackPointsWithExtension.withIndex()) {
                val latLong = LatLong(trackPointPair.first.latitude, trackPointPair.first.longitude)
                val bestRoadInfoForPoint = mapFiles.mapNotNull { mapFile ->
                    getRoadInfoForLatLong(latLong, mapFile)
                }.minByOrNull { it.minDistance }
                roadInfos[index] = bestRoadInfoForPoint
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading road type at position", e)
        } finally {
            mapFiles.forEach {
                try {
                    it.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing map file", e)
                }
            }
        }
        return roadInfos
    }

    private fun setDistances(
        trackPointsWithExtension: List<Pair<TrackPoint, ExtensionFromYaml>>,
        distancePerSurface: MutableMap<Surface, Double>,
        distancePerRoadTypes: MutableMap<RoadType, Double>
    ) {
        trackPointsWithExtension.forEachIndexed { i, e ->
            val distanceI = trackPointsWithExtension[i].second.distance
            val distanceBetween = if (i > 0) {
                val distanceBefore = trackPointsWithExtension[i - 1].second.distance

                if (distanceI != null && distanceI != 0.0 && distanceBefore != null && distanceBefore != 0.0) {
                    distanceI - distanceBefore
                } else {
                    calculateDistance(trackPointsWithExtension, i)
                }
            } else {
                0.0
            }

            distancePerSurface[trackPointsWithExtension[i].second.surface] =
                (distancePerSurface[trackPointsWithExtension[i].second.surface]
                    ?: 0.0) + distanceBetween
            distancePerRoadTypes[trackPointsWithExtension[i].second.roadType] =
                (distancePerRoadTypes[trackPointsWithExtension[i].second.roadType]
                    ?: 0.0) + distanceBetween
        }
    }

    fun filterWronglySelectedRoadTypes(
        roadInfos: MutableList<RoadInfo?>,
        trackPointsWithExtension: List<Pair<TrackPoint, ExtensionFromYaml>>
    ) {
        val roadInfoIndexed = mutableListOf<Triple<RoadInfo, Int, Int>>()
        roadInfos.forEachIndexed { i, e ->
            if (i == 0) {
                roadInfoIndexed.add(Triple(e ?: RoadInfo(""), i, i))
            } else {
                if (e == null || (roadInfoIndexed.last().first.name == e.name && e.roadType != "" && e.surface != null && roadInfoIndexed.last().first.roadType == e.roadType && roadInfoIndexed.last().first.surface == e.surface)) {
                    roadInfoIndexed[roadInfoIndexed.size - 1] = Triple(
                        roadInfoIndexed.last().first, roadInfoIndexed.last().second, i
                    )
                } else {
                    roadInfoIndexed.add(Triple(e, i, i))
                }
            }
        }
        val filtered = mutableListOf<Triple<RoadInfo, Int, Int>>()
        roadInfoIndexed.forEachIndexed { i, e ->
            if (i == 0 || i == roadInfoIndexed.size - 1) {
                filtered.add(e)
            } else {
                if (e.third - e.second > 10 || roadInfoIndexed[i - 1].first.name != roadInfoIndexed[i + 1].first.name) {
                    filtered.add(e)
                } else {
                    filtered[filtered.size - 1] = Triple(
                        filtered[filtered.size - 1].first,
                        filtered[filtered.size - 1].second,
                        e.third
                    )
                }
            }
        }
        filtered.forEach {
            (it.second..it.third).forEach { index ->
                if (index < trackPointsWithExtension.size) {
                    trackPointsWithExtension[index].second.surface =
                        Surface.mapFromRoadInfo(it.first)
                    trackPointsWithExtension[index].second.roadType =
                        RoadType.mapFromRoadInfo(it.first)
                }
            }
        }
    }

    private fun calculateDistance(
        trackPointsWithExtension: List<Pair<TrackPoint, ExtensionFromYaml>>, i: Int
    ): Double = abs(
        GpsUtils.getLocationFromTrackPoint(
            trackPointsWithExtension[i - 1].first
        ).distanceTo(
            GpsUtils.getLocationFromTrackPoint(
                trackPointsWithExtension[i].first
            )
        ).toDouble()
    )

    fun getRoadInfoForLatLong(
        latLong: LatLong, mapFile: MapFile? = mapFiles.firstOrNull()
    ): RoadInfo? {
        if (mapFile != null) {
            val tile = Tile(
                latLongToTileX(latLong.longitude),
                latLongToTileY(latLong.latitude),
                16.toByte(),
                256
            )

            val mapReadResult: MapReadResult = mapFile.readMapData(tile)
            val roadInfo = processWays(mapReadResult.ways, latLong)
            if (roadInfo != null && roadInfo.minDistance < searchRadiusMeters) {
                return roadInfo
            }
        }
        return null
    }

    /**
     * Get the closest village/city/mountain peak and its country for a given location
     * @param latLong The location to search from
     * @param mapFile The map file to search in (defaults to first available map file)
     * @return LocationInfo containing the closest place name, country, and type, or null if none found
     */
    fun getClosestLocationInfo(
        latLong: LatLong, mapFile: MapFile? = mapFiles.firstOrNull()
    ): LocationInfo? {
        if (mapFile == null) {
            Log.w(TAG, "No map file available to get location info.")
            return null
        }

        val zoomLevel: Byte = 16
        val tile = Tile(
            latLongToTileX(latLong.longitude, zoomLevel.toInt()),
            latLongToTileY(latLong.latitude, zoomLevel.toInt()),
            zoomLevel,
            mapFile.mapFileInfo.tilePixelSize
        )

        try {
            val mapReadResult: MapReadResult = mapFile.readPoiData(tile)
            val locationInfo = processPointsOfInterest(
                mapReadResult.pointOfInterests, latLong
            )

            if (locationInfo != null) {
                return locationInfo
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading location info at position", e)
        }

        return null
    }

    /**
     * Process points of interest to find the closest village/city/mountain peak
     */
    private fun processPointsOfInterest(
        pointsOfInterest: List<org.mapsforge.map.datastore.PointOfInterest>, latLong: LatLong
    ): LocationInfo? {
        val locationInfoList: MutableList<LocationInfo> = mutableListOf()
        val hutTags = listOf("alpine_hut", "wilderness_hut", "basic_hut")
        val placeTypePriority = mapOf(
            "peak" to 80,
            "mountain_pass" to 70,
            "saddle" to 60,
            "hut" to 50,
            "city" to 40,
            "town" to 30,
            "village" to 20,
            "hamlet" to 11,
            "suburb" to 10,
            "neighbourhood" to 5,
            "isolated_dwelling" to 0
        )
        for (poi in pointsOfInterest) {
            val tags = poi.tags
            val placeTag = tags.firstOrNull { it.key == "place" }
            val naturalTag = tags.firstOrNull { it.key == "natural" }
            val tourismTag =
                tags.firstOrNull { it.key == "tourism" || it.key == "shelter_type" || it.key == "winter_room" }
            // Check for place tags (cities, towns, villages, etc.)
            if (placeTag != null && placeTypePriority.containsKey(placeTag.value)) {
                val nameTag = tags.firstOrNull { it.key == "name" }?.value

                if (nameTag != null) {
                    val distance = latLong.vincentyDistance(poi.position)

                    if (distance <= 5000) {
                        val locationInfo = LocationInfo(
                            name = nameTag,
                            placeType = placeTag.value,
                            minDistance = distance.roundToInt(),
                            additionalTags = tags.associate { it.key to it.value })
                        locationInfoList.add(locationInfo)
                    }
                }
            }

            // Check for mountain peaks
            if (naturalTag != null && (naturalTag.value == "peak" || naturalTag.value == "mountain_pass" || naturalTag.value == "saddle")) {
                val nameTag = tags.firstOrNull { it.key == "name" }?.value
                if (nameTag != null) {
                    val distance = latLong.vincentyDistance(poi.position)
                    if (distance <= 250) {
                        val locationInfo = LocationInfo(
                            name = nameTag,
                            placeType = naturalTag.value,
                            minDistance = distance.roundToInt(),
                            additionalTags = tags.associate { it.key to it.value })
                        locationInfoList.add(locationInfo)
                    }
                }
            }
            if (tourismTag != null && tourismTag.value in hutTags) {
                val nameTag = tags.firstOrNull { it.key == "name" }?.value
                if (nameTag != null) {
                    val distance = latLong.vincentyDistance(poi.position)
                    if (distance <= 250) {
                        val locationInfo = LocationInfo(
                            name = nameTag,
                            placeType = "hut",
                            minDistance = distance.roundToInt(),
                            additionalTags = tags.associate { it.key to it.value })
                        locationInfoList.add(locationInfo)
                    }
                }
            }
        }

        // Sort by priority first (higher priority = better), then by distance (closer = better)
        return locationInfoList.sortedWith(
            compareBy({ -(placeTypePriority[it.placeType] ?: 0) }, { it.minDistance })
        ).firstOrNull()
    }

    private fun getMinimalDistance(way: Way, latLong: LatLong): Int {
        val allPoints = way.latLongs.flatMap { it.toList() }
        val distances = mutableListOf<Int>()
        for (i in 0 until allPoints.size - 1) {
            val start = allPoints[i]
            val end = allPoints[i + 1]

            // Calculate distance from position to line segment
            distances.add(distanceToLineSegment(latLong, start, end).roundToInt())
        }

        return distances.min()
    }

    private fun processWays(
        ways: List<Way>,
        latLong: LatLong,
    ): RoadInfo? {
        val roadInfoList: MutableList<RoadInfo> = mutableListOf()
        for (way in ways) {
            val tags = way.tags
            val highwayTag = tags.firstOrNull { it.key == "highway" }

            if (highwayTag != null) {
                // Check if the way is close to our position
                val roadInfo = RoadInfo(
                    roadType = highwayTag.value,
                    name = tags.firstOrNull { it.key == "name" }?.value,
                    surface = tags.firstOrNull { it.key == "surface" }?.value,
                    trackType = tags.firstOrNull { it.key == "tracktype" }?.value,
                    minDistance = getMinimalDistance(way, latLong),
                    additionalTags = tags.associate { it.key to it.value })
                roadInfoList.add(roadInfo)
            }
        }
        return roadInfoList.minByOrNull { it.minDistance }
    }

    /**
     * Calculate distance from a point to a line segment
     */
    private fun distanceToLineSegment(
        point: LatLong, lineStart: LatLong, lineEnd: LatLong
    ): Double {
        val x = point.longitude
        val y = point.latitude
        val x1 = lineStart.longitude
        val y1 = lineStart.latitude
        val x2 = lineEnd.longitude
        val y2 = lineEnd.latitude

        val a = x - x1
        val b = y - y1
        val c = x2 - x1
        val d = y2 - y1

        val dot = a * c + b * d
        val lenSq = c * c + d * d
        var param = -1.0

        if (lenSq != 0.0) {
            param = dot / lenSq
        }

        val xx: Double
        val yy: Double

        when {
            param < 0 -> {
                xx = x1
                yy = y1
            }

            param > 1 -> {
                xx = x2
                yy = y2
            }

            else -> {
                xx = x1 + param * c
                yy = y1 + param * d
            }
        }

        val dx = x - xx
        val dy = y - yy

        // Convert to meters (approximate)
        val distanceInDegrees = sqrt(dx * dx + dy * dy)
        return distanceInDegrees * 111320.0 // approximate conversion to meters
    }

    /**
     * Convert longitude to tile X coordinate
     */
    private fun latLongToTileX(lon: Double, zoom: Int = 16): Int {
        return ((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
    }

    /**
     * Convert latitude to tile Y coordinate
     */
    private fun latLongToTileY(lat: Double, zoom: Int = 16): Int {
        val latRad = Math.toRadians(lat)
        return ((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * (1 shl zoom)).toInt()
    }

    companion object {
        const val TAG = "RoadSurfaceAnalyzer"
        fun from(context: Context, searchRadiusMeters: Double = 15.0): OfflineMapAnalyzer {
            val mapFiles = FileHelper.getOnDeviceMapFiles(context)
            return OfflineMapAnalyzer(
                FileHelper.getOnDeviceMapFileInputStreams(context, mapFiles).map { MapFile(it) },
                searchRadiusMeters
            )
        }


        fun setDistancePerSurfacesAndRoadType(
            context: Context, summit: Summit
        ): Boolean {
            summit.setGpsTrack(useSimplifiedTrack = false, updateTrack = true)
            val analyzer = from(context)
            val boundingBox = summit.trackBoundingBox
            if (boundingBox != null && !analyzer.hasMapCoverageForBoundingBox(boundingBox)) {
                Log.w(TAG, "Track does not overlap with any available map files. Skipping road type analysis.")
                return false
            }
            val distancePerSurfacesAndRoadType = analyzer.getRoadTypeSummaryFromTrackPoints(
                summit.gpsTrack?.trackPoints
            )
            if (valuesAreNotEmpty(distancePerSurfacesAndRoadType)) {
                val extensions = summit.gpsTrack?.trackPoints?.map { it.second }
                if (extensions?.isNotEmpty() == true) {
                    val extensionsFromYaml = ExtensionsFromYaml(extensions)
                    try {
                        val yamlString = Yaml.Default.encodeToString(extensionsFromYaml)
                        summit.getYamlExtensionsFile().writeText(yamlString)
                        Log.i(
                            TAG,
                            "Successfully wrote extensions to ${summit.getYamlExtensionsFile().absolutePath}"
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to write YAML extensions file", e)
                    }
                }
                summit.distancePerSurface = distancePerSurfacesAndRoadType.first
                summit.distancePerRoadType = distancePerSurfacesAndRoadType.second
                return true
            } else {
                Log.e(TAG, "No road information found in map")
                return false
            }
        }

        private fun valuesAreNotEmpty(distancePerSurfacesAndRoadType: Pair<Map<Surface, Int>, Map<RoadType, Int>>): Boolean =
            distancePerSurfacesAndRoadType.first.map { it.value }
                .toSet() != setOf(0) && distancePerSurfacesAndRoadType.second.map { it.value }
                .toSet() != setOf(0)
    }
}

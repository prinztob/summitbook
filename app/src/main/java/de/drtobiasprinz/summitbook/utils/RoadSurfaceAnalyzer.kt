package de.drtobiasprinz.summitbook.utils

import android.content.Context
import android.util.Log
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.ExtensionFromYaml
import de.drtobiasprinz.summitbook.models.ExtensionsFromYaml
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.time.measureTime


class RoadSurfaceAnalyzer(var mapFiles: List<MapFile>, var searchRadiusMeters: Double) {

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
                Log.i(
                    TAG,
                    "distancePerRoadType: $distancePerRoadTypes and distancePerSurface: $distancePerSurface"
                )
            }
            Log.i(TAG, "getRoadTypeAtPosition took $time")
        }
        return Pair(
            distancePerSurface.map { it.key to it.value.roundToInt() }.toMap(),
            distancePerRoadTypes.map { it.key to it.value.roundToInt() }.toMap()
        )
    }

    private fun extractRoadInfos(
        trackPointsWithExtension: List<Pair<TrackPoint, ExtensionFromYaml>>,
    ): MutableList<RoadInfo?> {
        val roadInfos: MutableList<RoadInfo?> = mutableListOf()
        try {
            for (mapFile in mapFiles) {
                trackPointsWithExtension.map {
                    roadInfos.add(
                        getRoadInfoForLatLong(
                            LatLong(
                                it.first.latitude, it.first.longitude
                            ), mapFile
                        )
                    )
                }
                mapFile.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading road type at position", e)
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
                if (e == null || roadInfoIndexed.last().first.name == e.name) {
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
                if (e.third - e.second > 10 && roadInfoIndexed[i - 1].first.name != roadInfoIndexed[i + 1].first.name) {
                    filtered.add(e)
                } else {
                    filtered[filtered.size - 1] = Triple(filtered[filtered.size - 1].first, filtered[filtered.size - 1].second, e.third)
                }
            }
        }
        filtered.forEach {
            (it.second..it.third).forEach { index ->
                trackPointsWithExtension[index].second.surface = Surface.mapFromRoadInfo(it.first)
                trackPointsWithExtension[index].second.roadType = RoadType.mapFromRoadInfo(it.first)
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
            Log.d(TAG, "Unknown type for road $roadInfo")
        }
        return null
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
        fun from(context: Context, searchRadiusMeters: Double = 25.0): RoadSurfaceAnalyzer {
            val mapFiles = FileHelper.getOnDeviceMapFiles(context)
            return RoadSurfaceAnalyzer(
                FileHelper.getOnDeviceMapFileInputStreams(context, mapFiles).map { MapFile(it) },
                searchRadiusMeters
            )
        }


        fun setDistancePerSurfacesAndRoadType(
            context: Context, summit: Summit
        ): Boolean {
            summit.setGpsTrack(useSimplifiedTrack = false, updateTrack = true)
            val distancePerSurfacesAndRoadType = from(context).getRoadTypeSummaryFromTrackPoints(
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

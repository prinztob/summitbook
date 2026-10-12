package de.drtobiasprinz.summitbook.data.analytics

import de.drtobiasprinz.summitbook.data.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.model.ExtensionFromYaml
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import org.osmdroid.util.GeoPoint
import kotlin.math.abs

/**
 * Creates segment entries and mountain passes for an activity from a template
 * entry of another activity that was ridden on the same course.
 *
 * The slice of the new track is found by proximity to the template's start
 * and end coordinates (same heuristic as the manual selection in the add
 * segment entry screen), so the created entry has the same start/end points
 * but stats computed from the new activity's own track.
 */
class SegmentEntryCreator {

    companion object {

        /** Max distance from the template start/end for a track point to match. */
        const val POINT_MATCH_TOLERANCE_METERS = 10.0

        /** Window distance used for mountain passes whose template has none stored. */
        private const val DEFAULT_WINDOW_DISTANCE_METERS = 500.0

        /**
         * Finds the slice of the given track that corresponds to the template
         * entry: the first track point within [POINT_MATCH_TOLERANCE_METERS]
         * of the template start point and the last track point within the
         * tolerance of the template end point, or null if no such slice
         * exists.
         */
        fun findSliceIndices(
            trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
            template: SegmentEntry,
            toleranceMeters: Double = POINT_MATCH_TOLERANCE_METERS
        ): Pair<Int, Int>? {
            if (trackPoints.isEmpty()) {
                return null
            }
            val templateStartPoint =
                GeoPoint(template.startPositionLatitude, template.startPositionLongitude)
            val templateEndPoint =
                GeoPoint(template.endPositionLatitude, template.endPositionLongitude)
            val startPointId = trackPoints.indexOfFirst {
                abs(
                    GpsUtils.getDistance(
                        GeoPoint(it.first.latitude, it.first.longitude),
                        templateStartPoint
                    )
                ) < toleranceMeters
            }
            if (startPointId < 0) {
                return null
            }
            val endPointId = trackPoints.indexOfLast {
                abs(
                    GpsUtils.getDistance(
                        GeoPoint(it.first.latitude, it.first.longitude),
                        templateEndPoint
                    )
                ) < toleranceMeters
            }
            if (endPointId <= startPointId) {
                return null
            }
            return Pair(startPointId, endPointId)
        }

        /**
         * Computes the stats of the track slice between startPointId and
         * endPointId (both inclusive, either order), the same way the manual
         * add segment entry screen does. Pass a window distance to also
         * analyze the elevation window (mountain pass mode).
         */
        fun computeStats(
            trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>?,
            startPointId: Int,
            endPointId: Int,
            windowDistanceMeters: Double? = null
        ): SegmentStats? {
            if (trackPoints.isNullOrEmpty() ||
                startPointId < 0 || endPointId < 0 ||
                startPointId >= trackPoints.size || endPointId >= trackPoints.size
            ) {
                return null
            }
            val startTrackPoint = trackPoints[startPointId]
            val endTrackPoint = trackPoints[endPointId]
            val selectedTrackPoints = trackPoints.subList(
                startPointId.coerceAtMost(endPointId),
                endPointId.coerceAtLeast(startPointId) + 1
            )

            val averageHeartRate = if (selectedTrackPoints.isNotEmpty()) {
                selectedTrackPoints.sumOf { it.second.hr ?: 0 } / selectedTrackPoints.size
            } else 0

            val averagePower = if (selectedTrackPoints.isNotEmpty()) {
                selectedTrackPoints.sumOf { it.second.power ?: 0 } / selectedTrackPoints.size
            } else 0

            val pointsOnlyWithMaximalValues =
                TrackUtils.keepOnlyMaximalValues(selectedTrackPoints)
            val heightMeterResult =
                TrackUtils.removeDeltasSmallerAs(10, pointsOnlyWithMaximalValues)

            val duration =
                ((endTrackPoint.first.time.millis - startTrackPoint.first.time.millis).toDouble() / 60000.0).coerceAtLeast(
                    0.0
                )
            val kilometers =
                (((endTrackPoint.second.distance ?: 0.0) - (startTrackPoint.second.distance
                    ?: 0.0)) / 1000.0).coerceAtLeast(0.0)

            val elevationWindowResult = if (windowDistanceMeters != null) {
                try {
                    ElevationTrackAnalyzer.analyzeWindow(
                        trackPoints,
                        startPointId.coerceAtMost(endPointId),
                        endPointId.coerceAtLeast(startPointId),
                        windowDistanceMeters
                    )
                } catch (_: Exception) {
                    null
                }
            } else null

            return SegmentStats(
                averageHeartRate = averageHeartRate,
                averagePower = averagePower,
                heightMetersUp = heightMeterResult.second,
                heightMetersDown = heightMeterResult.third,
                duration = duration,
                kilometers = kilometers,
                elevationWindowResult = elevationWindowResult
            )
        }

        /**
         * Creates a ready-to-save [SegmentEntry] for the new summit's track
         * from the template entry, or null if the template's start/end points
         * cannot be found in the new track. The new summit's full (non
         * simplified) track is parsed on demand.
         */
        fun createSegmentEntry(newSummit: Summit, template: SegmentEntry): SegmentEntry? {
            newSummit.setGpsTrack(useSimplifiedTrack = false)
            val trackPoints = newSummit.gpsTrack?.trackPoints
            if (trackPoints.isNullOrEmpty()) {
                return null
            }
            val sliceIndices = findSliceIndices(trackPoints, template) ?: return null
            val startPointId = sliceIndices.first
            val endPointId = sliceIndices.second
            val windowDistanceMeters =
                if (template.windowDistanceMeters > 0) template.windowDistanceMeters
                else DEFAULT_WINDOW_DISTANCE_METERS
            val stats = computeStats(
                trackPoints,
                startPointId,
                endPointId,
                if (template.isMountainPass) windowDistanceMeters else null
            ) ?: return null
            val startTrackPoint = trackPoints[startPointId].first
            val endTrackPoint = trackPoints[endPointId].first
            val elevationWindowResult = stats.elevationWindowResult
            return SegmentEntry(
                segmentId = template.segmentId,
                date = newSummit.date,
                activityId = newSummit.activityId,
                startPositionInTrack = startPointId,
                startPositionLatitude = startTrackPoint.latitude,
                startPositionLongitude = startTrackPoint.longitude,
                endPositionInTrack = endPointId,
                endPositionLatitude = endTrackPoint.latitude,
                endPositionLongitude = endTrackPoint.longitude,
                duration = stats.duration,
                kilometers = stats.kilometers,
                heightMetersUp = elevationWindowResult?.elevationGain ?: stats.heightMetersUp,
                heightMetersDown = elevationWindowResult?.elevationLoss ?: stats.heightMetersDown,
                averageHeartRate = if (template.isMountainPass) 0 else stats.averageHeartRate,
                averagePower = if (template.isMountainPass) 0 else stats.averagePower,
                isMountainPass = template.isMountainPass,
                name = template.name,
                avgGradient = elevationWindowResult?.avgGradient ?: 0.0,
                maxGradeInWindow = elevationWindowResult?.maxGradeInWindow ?: 0.0,
                windowDistanceMeters = if (template.isMountainPass) windowDistanceMeters
                else template.windowDistanceMeters,
                durationInMotion = elevationWindowResult?.durationInMotion ?: 0.0
            )
        }

        /**
         * Whether an entry equivalent to the template already exists for the
         * activity: for regular segments the same segment must not have an
         * entry yet, for mountain passes no pass with (nearly) the same
         * start/end points must exist.
         */
        fun isAlreadyCreated(
            existingEntries: List<SegmentEntry>,
            template: SegmentEntry
        ): Boolean {
            return if (template.isMountainPass) {
                val templateStartPoint =
                    GeoPoint(template.startPositionLatitude, template.startPositionLongitude)
                val templateEndPoint =
                    GeoPoint(template.endPositionLatitude, template.endPositionLongitude)
                existingEntries.any { existing ->
                    existing.isMountainPass &&
                            abs(
                                GpsUtils.getDistance(
                                    GeoPoint(
                                        existing.startPositionLatitude,
                                        existing.startPositionLongitude
                                    ),
                                    templateStartPoint
                                )
                            ) < POINT_MATCH_TOLERANCE_METERS &&
                            abs(
                                GpsUtils.getDistance(
                                    GeoPoint(
                                        existing.endPositionLatitude,
                                        existing.endPositionLongitude
                                    ),
                                    templateEndPoint
                                )
                            ) < POINT_MATCH_TOLERANCE_METERS
                }
            } else {
                existingEntries.any { !it.isMountainPass && it.segmentId == template.segmentId }
            }
        }
    }
}

data class SegmentStats(
    val averageHeartRate: Int,
    val averagePower: Int,
    val heightMetersUp: Double,
    val heightMetersDown: Double,
    val duration: Double,
    val kilometers: Double,
    val elevationWindowResult: ElevationTrackAnalyzer.ElevationWindowResult? = null
)

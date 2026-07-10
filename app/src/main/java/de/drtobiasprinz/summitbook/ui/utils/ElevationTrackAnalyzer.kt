package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.models.ExtensionFromYaml
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sign

/**
 * Analyzes elevation data for a track window.
 *
 * Converted from Python ElevationTrackAnalyzer.analyze_window and the related
 * get_cleaned_track_elevation utility in entry_point.py / utils.py.
 */
class ElevationTrackAnalyzer(
    private val points: List<Pair<TrackPoint, ExtensionFromYaml>>
) {

    /**
     * Result of analyzing an elevation window.
     *
     * @param elevationGain Sum of positive cleaned elevation deltas (meters)
     * @param elevationLoss Sum of absolute negative cleaned elevation deltas (meters)
     * @param avgGradient Net elevation change / total distance * 100 (percent)
     * @param maxGradeInWindow Maximum grade over a sliding window of the given distance, sign preserved (percent)
     * @param windowLength Total distance of the window (meters)
     */
    data class ElevationWindowResult(
        val elevationGain: Double,
        val elevationLoss: Double,
        val avgGradient: Double,
        val maxGradeInWindow: Double,
        val windowLength: Double,
        val durationInMotion: Double
    )

    /**
     * Internal helper to track the original index of a point after filtering.
     */
    private data class IndexedPoint(
        val index: Int,
        val point: Pair<TrackPoint, ExtensionFromYaml>
    )

    companion object {
        /**
         * Convenience method to analyze an elevation window in one call.
         */
        fun analyzeWindow(
            points: List<Pair<TrackPoint, ExtensionFromYaml>>,
            startIdx: Int,
            endIdx: Int,
            windowDistanceMeters: Double = 500.0
        ): ElevationWindowResult {
            return ElevationTrackAnalyzer(points).analyzeWindow(startIdx, endIdx, windowDistanceMeters)
        }
    }

    /**
     * Analyze elevation data for a window of the track between startIdx and endIdx (inclusive).
     *
     * Converted from Python ElevationTrackAnalyzer.analyze_window.
     *
     * @param startIdx Start index of the window (inclusive, 0-based).
     * @param endIdx End index of the window (inclusive, 0-based).
     * @param windowDistanceMeters Distance in meters for the sliding window used to compute maxGradeInWindow.
     * @return ElevationWindowResult with elevation gain, loss, average gradient, max grade in window, and window length.
     * @throws IllegalArgumentException If indices are out of range or startIdx >= endIdx.
     */
    fun analyzeWindow(startIdx: Int, endIdx: Int, windowDistanceMeters: Double = 500.0): ElevationWindowResult {
        require(startIdx >= 0 && endIdx < points.size) {
            "Index out of range: startIdx=$startIdx, endIdx=$endIdx, track length=${points.size}"
        }
        require(startIdx < endIdx) {
            "startIdx must be less than endIdx: startIdx=$startIdx, endIdx=$endIdx"
        }

        val windowPoints = points.subList(startIdx, endIdx + 1)

        // Reuse getCleanedTrackElevation for cleaned elevation deltas
        val deltas = getCleanedTrackElevation(windowPoints)

        // Elevation gain and loss (same pattern as Python analyze())
        var elevationGain = 0.0
        var elevationLoss = 0.0
        for (delta in deltas) {
            if (delta > 0) elevationGain += delta
            else if (delta < 0) elevationLoss += abs(delta)
        }

        // Average gradient: net elevation change / total distance * 100
        val totalDistance =
            (windowPoints.last().second.distance ?: 0.0) - (windowPoints.first().second.distance ?: 0.0)
        val startElevation = windowPoints.first().first.elevation ?: 0.0
        val endElevation = windowPoints.last().first.elevation ?: 0.0

        val avgGradient = if (totalDistance > 0) {
            (endElevation - startElevation) / totalDistance * 100.0
        } else 0.0

        // Max grade in window: sliding window max grade (elevation diff / horizontal distance * 100)
        val maxGradeInWindow = computeMaxGradeInWindow(windowPoints, windowDistanceMeters)

        // Duration in motion: sum of time deltas between consecutive points, excluding gaps > 10 seconds
        val durationInMotion = computeDurationInMotion(windowPoints)

        return ElevationWindowResult(
            elevationGain = roundTo3(elevationGain),
            elevationLoss = roundTo3(elevationLoss),
            avgGradient = roundTo3(avgGradient),
            maxGradeInWindow = roundTo3(maxGradeInWindow),
            windowLength = roundTo3(totalDistance),
            durationInMotion = roundTo3(durationInMotion)
        )
    }

    /**
     * Compute the maximum grade (percentage) over a sliding window of the given distance in meters.
     * Grade is computed as elevation difference / horizontal distance * 100.
     * The sign is preserved (positive for uphill, negative for downhill), and the result
     * with the largest absolute value is returned.
     */
    private fun computeMaxGradeInWindow(
        windowPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
        windowDistanceMeters: Double
    ): Double {
        if (windowPoints.size < 2 || windowDistanceMeters <= 0.0) return 0.0

        var maxGrade = 0.0
        var endIndex = 0

        for (startIndex in windowPoints.indices) {
            val startDist = windowPoints[startIndex].second.distance ?: 0.0
            val startElev = windowPoints[startIndex].first.elevation ?: 0.0

            // Advance endIndex until the window distance is reached
            while (endIndex < windowPoints.size - 1) {
                val endDist = windowPoints[endIndex].second.distance ?: 0.0
                if (endDist - startDist >= windowDistanceMeters) break
                endIndex++
            }

            val endDist = windowPoints[endIndex].second.distance ?: 0.0
            val endElev = windowPoints[endIndex].first.elevation ?: 0.0

            val distDiff = endDist - startDist
            if (distDiff > 0) {
                val elevDiff = endElev - startElev
                val grade = (elevDiff / distDiff) * 100.0
                if (abs(grade) > abs(maxGrade)) {
                    maxGrade = grade
                }
            }

            // Reset endIndex if it fell behind startIndex
            if (endIndex < startIndex) {
                endIndex = startIndex
            }
        }

        return maxGrade
    }

    private fun roundTo3(value: Double): Double = round(value * 1000.0) / 1000.0

    /**
     * Compute the duration in motion by summing time deltas between consecutive
     * track points, excluding gaps larger than [maxGapSeconds] (e.g. stops/pauses).
     *
     * @param windowPoints The track points in the window.
     * @param maxGapSeconds Maximum gap in seconds between two consecutive points to be counted as motion.
     * @return Duration in motion in minutes.
     */
    private fun computeDurationInMotion(
        windowPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
        maxGapSeconds: Int = 10
    ): Double {
        if (windowPoints.size < 2) return 0.0

        var totalMotionMillis = 0L
        for (i in 1 until windowPoints.size) {
            val prevTime = windowPoints[i - 1].first.time
            val currTime = windowPoints[i].first.time
            if (prevTime != null && currTime != null) {
                val deltaMillis = currTime.millis - prevTime.millis
                if (deltaMillis > 0 && deltaMillis <= maxGapSeconds * 1000L) {
                    totalMotionMillis += deltaMillis
                }
            }
        }
        return totalMotionMillis.toDouble() / 60000.0
    }

    /**
     * Get cleaned elevation deltas for a list of track points.
     *
     * Converted from Python get_cleaned_track_elevation in utils.py.
     * Applies elevation smoothing by reducing to relevant points, removing small deltas,
     * and filling missing points with monotonic interpolation.
     */
    private fun getCleanedTrackElevation(points: List<Pair<TrackPoint, ExtensionFromYaml>>): List<Double> {
        if (points.isEmpty()) return emptyList()

        // Step 1: Reduce to relevant elevation points (direction changes only)
        val reducedPoints = reduceTrackToRelevantElevationPoints(points)

        // Step 2: Remove elevation differences smaller than 10m
        val relevantPoints = removeElevationDifferencesSmallerAs(reducedPoints)

        // Step 3: Build flattened elevation list with filled missing points
        val flattenedElevations = mutableListOf<Double>()

        for (i in relevantPoints.indices) {
            val pointWithIndex = relevantPoints[i]
            flattenedElevations.add(pointWithIndex.point.first.elevation ?: 0.0)

            if (i < relevantPoints.size - 1) {
                val nextPointWithIndex = relevantPoints[i + 1]
                fillMissingElevations(
                    pointWithIndex.index,
                    pointWithIndex.point.first.elevation ?: 0.0,
                    nextPointWithIndex.index,
                    nextPointWithIndex.point.first.elevation ?: 0.0,
                    points,
                    flattenedElevations
                )
            }
        }

        // Fill after last relevant point if needed
        if (relevantPoints.isNotEmpty() && relevantPoints.last().index < points.size - 1) {
            fillMissingElevations(
                relevantPoints.last().index,
                relevantPoints.last().point.first.elevation ?: 0.0,
                points.size,
                points.last().first.elevation ?: 0.0,
                points,
                flattenedElevations
            )
        }

        // Step 4: Compute deltas from flattened elevations
        return flattenedElevations.mapIndexed { i, elev ->
            if (i > 0) elev - flattenedElevations[i - 1] else 0.0
        }
    }

    /**
     * Reduce track to relevant elevation points (direction changes only).
     *
     * Converted from Python reduce_track_to_relevant_elevation_points in utils.py.
     * First pass: keep points where rounded elevation differs from previous (and first/last).
     * Second pass: keep points where the direction of elevation change changes (and first/last).
     */
    private fun reduceTrackToRelevantElevationPoints(
        points: List<Pair<TrackPoint, ExtensionFromYaml>>
    ): List<IndexedPoint> {
        // First pass: keep points where elevation changes from previous (and first/last)
        val pointsWithDoubles = mutableListOf<IndexedPoint>()
        for (i in points.indices) {
            val currentElevation = round(points[i].first.elevation ?: 0.0)
            if (i == 0 || i == points.size - 1) {
                pointsWithDoubles.add(IndexedPoint(i, points[i]))
            } else {
                val lastElevation = round(points[i - 1].first.elevation ?: 0.0)
                if (currentElevation != lastElevation) {
                    pointsWithDoubles.add(IndexedPoint(i, points[i]))
                }
            }
        }

        // Second pass: keep points where direction changes (and first/last)
        val reducedPoints = mutableListOf<IndexedPoint>()
        for (j in pointsWithDoubles.indices) {
            val currentElevation = round(pointsWithDoubles[j].point.first.elevation ?: 0.0)
            if (j == 0 || j == pointsWithDoubles.size - 1) {
                reducedPoints.add(pointsWithDoubles[j])
            } else {
                val lastElevation = round(pointsWithDoubles[j - 1].point.first.elevation ?: 0.0)
                val nextElevation = round(pointsWithDoubles[j + 1].point.first.elevation ?: 0.0)
                if (currentElevation != lastElevation && currentElevation != nextElevation) {
                    if (sign(currentElevation - lastElevation) != sign(nextElevation - currentElevation)) {
                        reducedPoints.add(pointsWithDoubles[j])
                    }
                }
            }
        }

        return reducedPoints
    }

    /**
     * Remove elevation differences smaller than minimalDelta.
     *
     * Converted from Python remove_elevation_differences_smaller_as in utils.py.
     * If a delta is smaller than minimalDelta but the delta to the second-to-last
     * filtered point is larger than the delta from the last to second-to-last,
     * the last filtered point is replaced with the current point.
     */
    private fun removeElevationDifferencesSmallerAs(
        points: List<IndexedPoint>,
        minimalDelta: Int = 10
    ): List<IndexedPoint> {
        val filteredPoints = mutableListOf<IndexedPoint>()

        for ((i, point) in points.withIndex()) {
            if (i == 0) {
                filteredPoints.add(point)
            } else {
                val delta =
                    if (point.point.first.elevation != null && filteredPoints.last().point.first.elevation != null) {
                        point.point.first.elevation!! - filteredPoints.last().point.first.elevation!!
                    } else 0.0

                val deltaToSecondLast =
                    if (filteredPoints.size > 1 &&
                        point.point.first.elevation != null &&
                        filteredPoints[filteredPoints.size - 2].point.first.elevation != null
                    ) {
                        point.point.first.elevation!! - filteredPoints[filteredPoints.size - 2].point.first.elevation!!
                    } else 0.0

                val deltaFromLast =
                    if (filteredPoints.size > 1 &&
                        filteredPoints.last().point.first.elevation != null &&
                        filteredPoints[filteredPoints.size - 2].point.first.elevation != null
                    ) {
                        filteredPoints.last().point.first.elevation!! - filteredPoints[filteredPoints.size - 2].point.first.elevation!!
                    } else 0.0

                if (abs(delta) >= minimalDelta) {
                    filteredPoints.add(point)
                } else if (abs(deltaToSecondLast) > abs(deltaFromLast)) {
                    filteredPoints.removeAt(filteredPoints.size - 1)
                    filteredPoints.add(point)
                }
            }
        }

        return filteredPoints
    }

    /**
     * Fill missing elevations between relevant points with monotonic interpolation.
     *
     * Converted from Python fill_missing_points in utils.py.
     * For increasing sections: if a point's elevation is <= the previous, it's set to the previous.
     * For decreasing sections: if a point's elevation is >= the previous, it's set to the previous.
     * This smooths out noise in the elevation data.
     */
    private fun fillMissingElevations(
        startIndex: Int,
        startElevation: Double,
        endIndex: Int,
        endElevation: Double,
        originalPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
        elevations: MutableList<Double>
    ) {
        val fromIndex = startIndex + 1
        val toIndex = minOf(endIndex - 1, originalPoints.size)
        if (fromIndex >= toIndex) return

        val pointsInBetween = originalPoints.subList(fromIndex, toIndex)
        if (pointsInBetween.isEmpty()) return

        val isIncreasing = startElevation != 0.0 && endElevation != 0.0 && startElevation < endElevation

        // Add first point's elevation
        var lastElevation = pointsInBetween.first().first.elevation ?: 0.0
        elevations.add(lastElevation)

        // Process remaining points (starting from index 1, avoiding the duplicate-first-element
        // bug present in the original Python code)
        for (i in 1 until pointsInBetween.size) {
            val currentElevation = pointsInBetween[i].first.elevation ?: 0.0
            val adjustedElevation = if (isIncreasing) {
                if (currentElevation != 0.0 && lastElevation != 0.0 && currentElevation <= lastElevation) {
                    lastElevation
                } else {
                    currentElevation
                }
            } else {
                if (currentElevation != 0.0 && lastElevation != 0.0 && currentElevation >= lastElevation) {
                    lastElevation
                } else {
                    currentElevation
                }
            }
            elevations.add(adjustedElevation)
            lastElevation = adjustedElevation
        }
    }
}

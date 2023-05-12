package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.gpx.Gpx
import de.drtobiasprinz.gpx.TrackPoint
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

class TrackUtils {


    companion object {

        fun keepOnlyMaximalValues(points: MutableList<TrackPoint>): MutableList<TrackPoint> {
            val reducedPoints = points.filterIndexed { index, trackPoint ->
                val currentElevation = (trackPoint.ele ?: 0.0).roundToInt().toDouble()
                if (index == 0 || index == points.size - 1) {
                    true
                } else {
                    val lastElevation = (points[index - 1].ele ?: 0.0).roundToInt().toDouble()
                    currentElevation != lastElevation
                }
            }
            return reducedPoints.filterIndexed { index, trackPoint ->
                val currentElevation = (trackPoint.ele ?: 0.0).roundToInt().toDouble()
                val lastElevation = if (index != 0) (reducedPoints[index - 1].ele
                    ?: 0.0).roundToInt().toDouble() else currentElevation
                val nextElevation =
                    if (index != reducedPoints.size - 1) (reducedPoints[index + 1].ele
                        ?: 0.0).roundToInt().toDouble() else currentElevation
                if (index == 0 || index == reducedPoints.size - 1) {
                    true
                } else if (currentElevation != lastElevation && currentElevation != nextElevation) {
                    sign(currentElevation - lastElevation) != sign(nextElevation - currentElevation)
                } else {
                    false
                }
            } as MutableList<TrackPoint>
        }

        fun removeDeltasSmallerAs(
            minimalDelta: Int,
            points: MutableList<TrackPoint>
        ): Triple<MutableList<TrackPoint>, Double, Double> {
            val filteredPoints: MutableList<TrackPoint> = mutableListOf()
            var elevationGain = 0.0
            var elevationLoss = 0.0
            for ((index, point) in points.withIndex()) {
                if (index == 0) {
                    filteredPoints.add(point)
                } else {
                    val delta = (point.ele ?: 0.0) - (filteredPoints.last().ele ?: 0.0)
                    if (abs(delta) >= minimalDelta) {
                        filteredPoints.add(point)
                        if (delta > 0) {
                            elevationGain += delta
                        } else {
                            elevationLoss += delta
                        }
                    }
                }
            }
            return Triple(filteredPoints, elevationGain, elevationLoss)
        }

        fun getTrackPoints(gpxTrack: Gpx): MutableList<TrackPoint> {
            val trackPoints = mutableListOf<TrackPoint>()
            for (track in gpxTrack.tracks.toList().blockingGet()) {
                for (segment in track.segments.toList().blockingGet()) {
                    val points = segment.points.toList().blockingGet()
                    trackPoints.addAll(points)
                }
            }
            return trackPoints
        }
    }
}

package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.models.ExtensionFromYaml
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

class TrackUtils {


    companion object {

        fun keepOnlyMaximalValues(points: List<Pair<TrackPoint, ExtensionFromYaml>>): List<Pair<TrackPoint, ExtensionFromYaml>> {
            val reducedPoints = points.filterIndexed { index, trackPoint ->
                val currentElevation = (trackPoint.first.elevation ?: 0.0).roundToInt().toDouble()
                if (index == 0 || index == points.size - 1) {
                    true
                } else {
                    val lastElevation = (points[index - 1].first.elevation ?: 0.0).roundToInt().toDouble()
                    currentElevation != lastElevation
                }
            }
            return reducedPoints.filterIndexed { index, trackPoint ->
                val currentElevation = (trackPoint.first.elevation ?: 0.0).roundToInt().toDouble()
                val lastElevation = if (index != 0) (reducedPoints[index - 1].first.elevation
                    ?: 0.0).roundToInt().toDouble() else currentElevation
                val nextElevation =
                    if (index != reducedPoints.size - 1) (reducedPoints[index + 1].first.elevation
                        ?: 0.0).roundToInt().toDouble() else currentElevation
                if (index == 0 || index == reducedPoints.size - 1) {
                    true
                } else if (currentElevation != lastElevation && currentElevation != nextElevation) {
                    sign(currentElevation - lastElevation) != sign(nextElevation - currentElevation)
                } else {
                    false
                }
            }
        }

        fun removeDeltasSmallerAs(
            minimalDelta: Int,
            points: List<Pair<TrackPoint, ExtensionFromYaml>>
        ): Triple<MutableList<TrackPoint>, Double, Double> {
            val filteredPoints: MutableList<TrackPoint> = mutableListOf()
            var elevationGain = 0.0
            var elevationLoss = 0.0
            for ((index, point) in points.withIndex()) {
                if (index == 0) {
                    filteredPoints.add(point.first)
                } else {
                    val delta = (point.first.elevation ?: 0.0) - (filteredPoints.last().elevation ?: 0.0)
                    if (abs(delta) >= minimalDelta) {
                        filteredPoints.add(point.first)
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

    }
}

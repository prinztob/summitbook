package de.drtobiasprinz.summitbook.ui.utils

import com.github.mikephil.charting.data.Entry
import de.drtobiasprinz.gpx.Gpx
import de.drtobiasprinz.gpx.TrackPoint
import de.drtobiasprinz.summitbook.models.GpsTrack
import org.nield.kotlinstatistics.simpleRegression
import kotlin.math.abs
import kotlin.math.sign

class SummitSlope() {

    var maxSlope: Double = 0.0
    var maxVerticalVelocity: Double = 0.0
    var slopeGraph: MutableList<Entry> = mutableListOf()
    private val REQUIRED_R2 = 0.9

    fun calculateMaxSlope(gpxTrack: Gpx?, binSizeMeter: Double = 100.0, withRegression: Boolean = true, requiredR2: Double = REQUIRED_R2) {
        if (gpxTrack != null) {
            val trackPoints = getTrackPoints(gpxTrack)
            if (trackPoints.size > 0 && trackPoints.first().extension?.distance == null) {
                GpsTrack.setDistanceFromPoints(trackPoints)
            }
            val slopeInMeterInterval = getMaximalValues(binSizeMeter, trackPoints, withRegression, requiredR2)
            val maxSlopeTrackPoint = slopeInMeterInterval.maxBy { it.first.extension?.slope ?: 0.0 }
            maxSlope = (maxSlopeTrackPoint?.first?.extension?.slope ?: 0.0) * 100
            slopeGraph = slopeInMeterInterval.map {
                Entry(it.first.extension?.distance?.toFloat()
                        ?: 0f, (it.first.extension?.slope?.toFloat() ?: 0f) * 100, it.first)
            } as MutableList
        }
    }


    fun calculateMaxVerticalVelocity(gpxTrack: Gpx?, binSizeSeconds: Double = 60.0, minimalDelta: Int = 10) {
        if (gpxTrack != null) {
            val trackPoints = getTrackPoints(gpxTrack)
            if (trackPoints.size > 0 && trackPoints.first().extension?.distance == null) {
                GpsTrack.setDistanceFromPoints(trackPoints)
            }
            val verticalVelocityInSecondsInterval = getMaximalValues(binSizeSeconds, trackPoints, false, useSecondsForBinning = true, minimalDelta = minimalDelta)
            val maxVerticalVelocityTrackPoint = verticalVelocityInSecondsInterval.maxBy {
                it.first.extension?.verticalVelocity ?: 0.0
            }
            maxVerticalVelocity = (maxVerticalVelocityTrackPoint?.first?.extension?.verticalVelocity
                    ?: 0.0)
        }
    }

    private fun getMaximalValues(
            binSize: Double, trackPoints: List<TrackPoint>, withRegression: Boolean,
            requiredR2: Double = REQUIRED_R2, useSecondsForBinning: Boolean = false, minimalDelta: Int = 10
    ): MutableList<Triple<TrackPoint, TrackPoint, TrackPoint>> {
        val pointsInBinInterval = mutableListOf<Triple<TrackPoint, TrackPoint, TrackPoint>>()
        var sumInBinningInterval = 0.0
        var trackPointsForInterval = mutableListOf<TrackPoint>()
        var i = 0
        val steps = when {
            trackPoints.size > 50000 -> 25
            trackPoints.size > 25000 -> 13
            trackPoints.size > 12500 -> 6
            trackPoints.size > 6250 -> 3
            else -> 1
        }
        var middleEntry: TrackPoint? = null
        while (i < trackPoints.size) {
            if (middleEntry != null && sumInBinningInterval >= binSize) {
                if (useSecondsForBinning) {
                    getVerticalVelocity(trackPointsForInterval, middleEntry, pointsInBinInterval, minimalDelta)
                } else {
                    getSlope(withRegression, trackPointsForInterval, middleEntry, requiredR2, pointsInBinInterval)
                }

                middleEntry = trackPointsForInterval.last()
                val useSteps = if (steps < trackPointsForInterval.size) steps else trackPointsForInterval.size - 1
                trackPointsForInterval = trackPointsForInterval.subList(useSteps, trackPointsForInterval.size)
            }
            if (trackPoints[i].extension?.distance != null && trackPoints[i].ele != null) {
                trackPointsForInterval.add(trackPoints[i])
                sumInBinningInterval = if (useSecondsForBinning) {
                    (((trackPointsForInterval.last().time?: 0L) - (trackPointsForInterval.first().time ?: 0L))/1000).toDouble()
                } else {
                        (trackPointsForInterval.last().extension?.distance
                            ?: 0.0) - (trackPointsForInterval.first().extension?.distance ?: 0.0)
                }
                if (sumInBinningInterval > binSize / 2 && middleEntry == null) {
                    middleEntry = trackPoints[i]
                }
            }
            i += 1
        }
        if (middleEntry != null && sumInBinningInterval >= binSize) {
            if (useSecondsForBinning) {
                getVerticalVelocity(trackPointsForInterval, middleEntry, pointsInBinInterval, minimalDelta)
            } else {
                getSlope(withRegression, trackPointsForInterval, middleEntry, requiredR2, pointsInBinInterval)
            }
        }
        return pointsInBinInterval
    }


    private fun getSlope(withRegression: Boolean, trackPointsForInterval: MutableList<TrackPoint>, middleEntry: TrackPoint, requiredR2: Double, slopeInMeterInterval: MutableList<Triple<TrackPoint, TrackPoint, TrackPoint>>) {
        val firstEntry = trackPointsForInterval.first()
        val lastEntry = trackPointsForInterval.last()

        val distance = (lastEntry.extension?.distance ?: 0.0) - (firstEntry.extension?.distance
                ?: 0.0)
        if (withRegression) {
            val regression = trackPointsForInterval.simpleRegression(
                    xSelector = { it.extension!!.distance!! - trackPointsForInterval.first().extension!!.distance!! },
                    ySelector = { it.ele!! }
            )
            middleEntry.extension?.slope = regression.slope
            if (regression.rSquare > requiredR2) {
                slopeInMeterInterval.add(Triple(middleEntry, trackPointsForInterval.first(), trackPointsForInterval.last()))
            }
        } else {
            val elevation = (lastEntry.ele
                    ?: 0.0) - (firstEntry.ele ?: 0.0)
            middleEntry.extension?.slope = if (distance == 0.0) 0.0 else elevation / distance
            slopeInMeterInterval.add(Triple(middleEntry, trackPointsForInterval.first(), trackPointsForInterval.last()))
        }
    }

    private fun getVerticalVelocity(trackPointsForInterval: MutableList<TrackPoint>, middleEntry: TrackPoint, verticalVelocityInMeterInterval: MutableList<Triple<TrackPoint, TrackPoint, TrackPoint>>, minimalDelta: Int) {
        val firstEntry = trackPointsForInterval.first()
        val lastEntry = trackPointsForInterval.last()
        val firstElevation =firstEntry.ele
        val lastElevation =lastEntry.ele
        val firstTime =firstEntry.time
        val lastTime =lastEntry.time
        if (firstElevation != null && lastElevation != null && firstTime != null && lastTime != null) {
            val timeDeltaInSec = (lastTime - firstTime)/1000
            val reducedPoints = keepOnlyMaximalValues(trackPointsForInterval)
            val elevationDelta = removeDeltasSmallerAs(minimalDelta, reducedPoints).second
            middleEntry.extension?.verticalVelocity = if (timeDeltaInSec == 0L) 0.0 else elevationDelta / timeDeltaInSec
            verticalVelocityInMeterInterval.add(Triple(middleEntry, trackPointsForInterval.first(), trackPointsForInterval.last()))
        }
    }

    private fun getTrackPoints(gpxTrack: Gpx): MutableList<TrackPoint> {
        val trackPoints = mutableListOf<TrackPoint>()
        for (track in gpxTrack.tracks.toList().blockingGet()) {
            for (segment in track.segments.toList().blockingGet()) {
                val points = segment.points.toList().blockingGet()
                trackPoints.addAll(points)
            }
        }
        return trackPoints
    }

    companion object {
        fun keepOnlyMaximalValues(points: MutableList<TrackPoint>): MutableList<TrackPoint> {
            return points.filterIndexed { index, trackPoint ->
                if (index == 0 || index == points.size-1) {
                    true
                } else if (trackPoint.ele != points.get(index-1).ele && trackPoint.ele != points.get(index+1).ele){
                    sign((trackPoint.ele?:0.0) - (points.get(index-1).ele?:0.0)) != sign((points.get(index+1).ele?:0.0) - (trackPoint.ele?:0.0))
                } else {
                    false
                }
            } as MutableList<TrackPoint>
        }

        fun removeDeltasSmallerAs(minimalDelta: Int, points: MutableList<TrackPoint>): Triple<MutableList<TrackPoint>, Double, Double> {
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

    }
}
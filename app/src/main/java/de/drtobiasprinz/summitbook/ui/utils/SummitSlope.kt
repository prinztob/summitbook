package de.drtobiasprinz.summitbook.ui.utils

import com.github.mikephil.charting.data.Entry
import de.drtobiasprinz.gpx.Gpx
import de.drtobiasprinz.gpx.TrackPoint
import de.drtobiasprinz.summitbook.models.GpsTrack
import org.nield.kotlinstatistics.simpleRegression

class SummitSlope() {

    var maxSlope = 0.0
    var slopeGraph: MutableList<Entry> = mutableListOf()
    private val REQUIRED_R2 = 0.9

    fun calculateMaxSlope(gpxTrack: Gpx?, binSizeMeter: Double = 100.0, withRegression: Boolean = true, requiredR2: Double = REQUIRED_R2) {
        if (gpxTrack != null) {
            val trackPoints = getTrackPoints(gpxTrack)
            if (trackPoints.size > 0 && trackPoints.first().extension?.distance == null) {
                GpsTrack.setDistanceFromPoints(trackPoints)
            }
            setMaxSlope(binSizeMeter, trackPoints, withRegression, requiredR2)
        }
    }

    private fun setMaxSlope(meter: Double, trackPoints: List<TrackPoint>, withRegression: Boolean, requiredR2: Double) {
        val slopeInMeterInterval = mutableListOf<Triple<TrackPoint, TrackPoint, TrackPoint>>()
        var sumMeters = 0.0
        var i = 0
        val steps = when {
            trackPoints.size > 50000 -> 25
            trackPoints.size > 25000 -> 13
            trackPoints.size > 12500 -> 6
            trackPoints.size > 6250 -> 3
            else -> 1
        }
        var trackPointsForInterval = mutableListOf<TrackPoint>()
        var middleEntry: TrackPoint? = null
        while (i < trackPoints.size) {
            if (middleEntry != null && sumMeters >= meter) {
                getSlope(withRegression, trackPointsForInterval, middleEntry, requiredR2, slopeInMeterInterval)

                middleEntry = trackPointsForInterval.last()
                val useSteps = if (steps < trackPointsForInterval.size) steps else trackPointsForInterval.size-1
                trackPointsForInterval = trackPointsForInterval.subList(useSteps, trackPointsForInterval.size)
            }
            if (trackPoints[i].extension?.distance != null && trackPoints[i].ele != null) {
                trackPointsForInterval.add(trackPoints[i])
                sumMeters = (trackPointsForInterval.last().extension?.distance
                        ?: 0.0) - (trackPointsForInterval.first().extension?.distance ?: 0.0)
                if (sumMeters > meter / 2 && middleEntry == null) {
                    middleEntry = trackPoints[i]
                }
            }
            i += 1
        }
        if (middleEntry != null) {
            getSlope(withRegression, trackPointsForInterval, middleEntry, requiredR2, slopeInMeterInterval)
        }
        val maxSlopeTrackPoint = slopeInMeterInterval.maxBy { it.first.extension?.slope ?: 0.0 }
        maxSlope = (maxSlopeTrackPoint?.first?.extension?.slope ?: 0.0) * 100
        slopeGraph = slopeInMeterInterval.map {
            Entry(it.first.extension?.distance?.toFloat()
                    ?: 0f, (it.first.extension?.slope?.toFloat() ?: 0f) * 100, it.first)
        } as MutableList
    }

    private fun getSlope(withRegression: Boolean, trackPointsForInterval: MutableList<TrackPoint>, middleEntry: TrackPoint, requiredR2: Double, slopeInMeterInterval: MutableList<Triple<TrackPoint, TrackPoint, TrackPoint>>) {
        val entry = middleEntry
        if (withRegression) {
            val regression = trackPointsForInterval.simpleRegression(
                    xSelector = { it.extension!!.distance!! - trackPointsForInterval.first().extension!!.distance!! },
                    ySelector = { it.ele!! }
            )
            middleEntry.extension?.slope = regression.slope
            if (regression.rSquare > requiredR2) {
                slopeInMeterInterval.add(Triple(entry, trackPointsForInterval.first(), trackPointsForInterval.last()))
            }
        } else {
            val distance = (trackPointsForInterval.last().extension?.distance
                    ?: 0.0) - (trackPointsForInterval.first().extension?.distance ?: 0.0)
            val elevation = (trackPointsForInterval.last().ele
                    ?: 0.0) - (trackPointsForInterval.first().ele ?: 0.0)
            middleEntry.extension?.slope = if (distance == 0.0) 0.0 else elevation / distance
            slopeInMeterInterval.add(Triple(entry, trackPointsForInterval.first(), trackPointsForInterval.last()))
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
}

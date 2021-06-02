package de.drtobiasprinz.gpx

import de.drtobiasprinz.gpx.xml.XmlWritable
import de.drtobiasprinz.gpx.xml.XmlWrite
import io.reactivex.Observable

data class TrackSegment(
        val points: Observable<TrackPoint>
) : XmlWritable {

    override val writeOperations: Observable<XmlWrite>
        get() = newTag(TAG_SEGMENT, points.concatMap { it.writeOperations })

    class Builder {
        var trackPoints: List<TrackPoint>? = null

        fun build(): TrackSegment {
            return TrackSegment(points = Observable.fromIterable(trackPoints))
        }
    }

    companion object {
        const val TAG_SEGMENT = "trkseg"
    }
}
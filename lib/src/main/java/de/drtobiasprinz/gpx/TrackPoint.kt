package de.drtobiasprinz.gpx

import de.drtobiasprinz.gpx.xml.XmlWritable
import de.drtobiasprinz.gpx.xml.XmlWrite
import io.reactivex.Observable

data class TrackPoint(
        val lat: Double,
        val lon: Double,
        val time: Long? = null,
        val ele: Double? = null,
        val name: String? = null,
        var extension: PointExtension? = null
) : XmlWritable, Point(time, ele, name, extension) {

    override val writeOperations: Observable<XmlWrite>
        get() = newTag(TAG_TRACK_POINT,
                withAttribute(TAG_LAT, lat.toString()),
                withAttribute(TAG_LON, lon.toString()),
                optionalTagWithText(TAG_ELEVATION, ele?.toString()),
                if (time != null) Time(time).writeOperations else Observable.empty(),
                optionalTagWithText(TAG_NAME, name),
                if (extension != null) extension!!.writeOperations else Observable.empty()
        )

    class Builder(
            mLatitude: Double? = null,
            mLongitude: Double? = null,
            mElevation: Double? = null,
            mName: String? = null,
            mTime: Long? = null,
            mExtension: PointExtension? = null
    ) : Point.Builder(mLatitude, mLongitude, mElevation, mName, mTime, mExtension) {
        override fun build(): TrackPoint {
            return TrackPoint(lat = latitude ?: 0.0, lon = longitude
                    ?: 0.0, ele = elevation, name = name, time = time, extension = extension)
        }
    }

    companion object {
        const val TAG_TRACK_POINT = "trkpt"
    }
}

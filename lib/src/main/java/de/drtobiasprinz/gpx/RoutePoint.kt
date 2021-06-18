package de.drtobiasprinz.gpx

import de.drtobiasprinz.gpx.xml.XmlWritable
import de.drtobiasprinz.gpx.xml.XmlWrite
import io.reactivex.Observable

data class RoutePoint(
        val lat: Double,
        val lon: Double,
        val time: Long? = null,
        val ele: Double? = null,
        val name: String? = null
) : XmlWritable, Point(time = time, ele = ele, name = name) {

    override val writeOperations: Observable<XmlWrite>
        get() = newTag(TAG_ROUTE_POINT,
                withAttribute(TAG_LAT, lat.toString()),
                withAttribute(TAG_LON, lon.toString()),
                optionalTagWithText(TAG_ELEVATION, ele?.toString()),
                if (time != null) Time(time).writeOperations else Observable.empty(),
                optionalTagWithText(TAG_NAME, name)
        )

    class Builder(
            mLatitude: Double? = null,
            mLongitude: Double? = null,
            mElevation: Double? = null,
            mName: String? = null,
            mTime: Long? = null
    ) : Point.Builder(mLatitude, mLongitude, mElevation, mName, mTime) {
        override fun build(): RoutePoint {
            return RoutePoint(lat = latitude ?: 0.0, lon = longitude
                    ?: 0.0, ele = elevation, name = name, time = time)
        }
    }

    companion object {
        const val TAG_ROUTE_POINT = "rtept"
    }
}
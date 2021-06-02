package de.drtobiasprinz.gpx

import de.drtobiasprinz.gpx.xml.XmlWritable
import de.drtobiasprinz.gpx.xml.XmlWrite
import io.reactivex.Observable

data class Waypoint(
        val lat: Double,
        val lon: Double,
        val ele: Double? = null,
        val name: String? = null
) : XmlWritable, Point(ele = ele, name = name) {

    override val writeOperations: Observable<XmlWrite>
        get() = newTag(TAG_WAY_POINT,
                withAttribute(TAG_LAT, lat.toString()),
                withAttribute(TAG_LON, lon.toString()),
                optionalTagWithText(TAG_ELEVATION, ele?.toString()),
                optionalTagWithText(TAG_NAME, name)
        )

    class Builder(
            mLatitude: Double? = null,
            mLongitude: Double? = null,
            mElevation: Double? = null,
            mName: String? = null
    ) : Point.Builder(latitude = mLatitude, longitude = mLongitude, elevation = mElevation, name = mName) {
        override fun build(): Waypoint {
            return Waypoint(lat = latitude ?: 0.0, lon = longitude
                    ?: 0.0, ele = elevation, name = name)
        }
    }


    companion object {
        const val TAG_WAY_POINT = "wpt"
    }
}

package de.drtobiasprinz.gpx

import de.drtobiasprinz.gpx.xml.XmlWritable
import de.drtobiasprinz.gpx.xml.XmlWrite
import io.reactivex.Observable

data class PointExtension(
        val cadence: Int? = null,
        val distance: Double? = null,
        val heartRate: Int? = null,
        val power: Int? = null,
        val speed: Double? = null,
        val atemp: Double? = null
) : XmlWritable {

    override val writeOperations: Observable<XmlWrite>
        get() = newTag(TAG_EXTENSIONS,
                newTag("${TAG_EXTENSION_PREFIX}:${TAG_TRACK_POINT_EXTENSIONS}",
                        optionalTagWithText("$TAG_EXTENSION_PREFIX:${TAG_ATEMP}", atemp?.toString()),
                        optionalTagWithText("${TAG_EXTENSION_PREFIX}:${TAG_CADENCE}", cadence?.toString()),
                        optionalTagWithText("${TAG_EXTENSION_PREFIX}:${TAG_DISTANCE}", distance?.toString()),
                        optionalTagWithText("${TAG_EXTENSION_PREFIX}:${TAG_HR}", heartRate?.toString()),
                        optionalTagWithText("${TAG_EXTENSION_PREFIX}:${TAG_POWER}", power?.toString()),
                        optionalTagWithText("${TAG_EXTENSION_PREFIX}:${TAG_SPEED}", speed?.toString())
                )
        )

    class Builder {
        var distanceMeters: Double? = null
        var speed: Double? = null
        var heartRate: Int? = null
        var power: Int? = null
        var cadence: Int? = null
        var temp: Double? = null

        fun build(): PointExtension {
            return PointExtension(heartRate = heartRate, atemp = temp, cadence = cadence, power = power, speed = speed, distance = distanceMeters)
        }
    }

    companion object {
        const val TAG_EXTENSIONS = "extensions"
        const val TAG_TRACK_POINT_EXTENSIONS = "TrackPointExtension"
        const val TAG_EXTENSION_PREFIX = "ns3"
        const val TAG_CADENCE = "cad"
        const val TAG_DISTANCE = "distance"
        const val TAG_HR = "hr"
        const val TAG_ATEMP = "atemp"
        const val TAG_SPEED = "speed"
        const val TAG_POWER = "power"
    }
}

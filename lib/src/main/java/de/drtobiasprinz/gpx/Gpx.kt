package de.drtobiasprinz.gpx

import de.drtobiasprinz.gpx.xml.XmlWritable
import de.drtobiasprinz.gpx.xml.XmlWrite
import de.drtobiasprinz.gpx.xml.writeTo
import io.reactivex.Observable
import io.reactivex.Single
import java.io.Writer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class Gpx(
        val creator: String,
        val metadata: Metadata = Metadata(),
        val waypoints: Observable<Waypoint> = Observable.empty(),
        val tracks: Observable<Track> = Observable.empty(),
        val routes: Observable<Route> = Observable.empty()
) : XmlWritable {

    override val writeOperations: Observable<XmlWrite>
        get() = newTag(TAG_GPX,
                withAttribute("xmlns", "http://www.topografix.com/GPX/1/1"),
                withAttribute("xmlns:ns3", "http://www.garmin.com/xmlschemas/TrackPointExtension/v1"),
                withAttribute("version", "1.1"),
                withAttribute(TAG_CREATOR, creator),
                metadata.writeOperations,
                waypoints.concatMap { it.writeOperations },
                tracks.concatMap { it.writeOperations },
                routes.concatMap { it.writeOperations }
        )

    fun writeTo(writer: Writer, charset: Charset = StandardCharsets.UTF_8): Single<Writer> =
            writeOperations.writeTo(writer, charset)


    class Builder(
            var wayPoints: List<Waypoint>? = null,
            var routes: List<Route>? = null,
            var tracks: List<Track>? = null,
            var creator: String? = null,
            var metadata: Metadata? = null
    ) {
        fun build(): Gpx {
            return Gpx(
                    creator = creator ?: "unknown",
                    metadata = metadata ?: Metadata(),
                    waypoints = Observable.fromIterable(wayPoints),
                    tracks = Observable.fromIterable(tracks),
                    routes = Observable.fromIterable(routes)
            )
        }
    }

    companion object {
        const val TAG_GPX = "gpx"
        const val TAG_CREATOR = "creator"
    }
}

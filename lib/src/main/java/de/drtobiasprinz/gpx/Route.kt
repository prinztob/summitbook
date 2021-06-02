package de.drtobiasprinz.gpx

import de.drtobiasprinz.gpx.xml.XmlWritable
import de.drtobiasprinz.gpx.xml.XmlWrite
import io.reactivex.Observable

data class Route(
        val name: String? = null,
        val points: Observable<RoutePoint>
) : XmlWritable {

    override val writeOperations: Observable<XmlWrite>
        get() = newTag(TAG_ROUTE,
                optionalTagWithText(TAG_NAME, name),
                points.concatMap { it.writeOperations }
        )

    
    class Builder {
        var routePoints: List<RoutePoint>? = null
        var routeName: String? = null

        fun build(): Route {
            return Route(name = routeName, points = Observable.fromIterable(routePoints))
        }
    }

    companion object {
        const val TAG_ROUTE = "rte"
        const val TAG_NAME = "name"
    }
}
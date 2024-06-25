package de.drtobiasprinz.gpx

import io.ticofab.androidgpxparser.parser.domain.Link


class Route private constructor(builder: Builder) {
    val routePoints: List<RoutePoint> =
        builder.mRoutePoints ?: emptyList()
    val routeName: String? = builder.mRouteName
    val routeDesc: String? = builder.mRouteDesc
    val routeCmt: String? = builder.mRouteCmt
    val routeSrc: String? = builder.mRouteSrc
    val routeNumber: Int? = builder.mRouteNumber
    val routeLink: Link? = builder.mRouteLink
    val routeType: String? = builder.mRouteType

    class Builder {
        var mRoutePoints: List<RoutePoint>? = null
        var mRouteName: String? = null
        var mRouteDesc: String? = null
        var mRouteCmt: String? = null
        var mRouteSrc: String? = null
        var mRouteNumber: Int? = null
        var mRouteLink: Link? = null
        var mRouteType: String? = null

        fun setRoutePoints(routePoints: List<RoutePoint>?): Builder {
            mRoutePoints = routePoints
            return this
        }

        fun setRouteName(routeName: String?): Builder {
            mRouteName = routeName
            return this
        }

        fun setRouteDesc(routeDesc: String?): Builder {
            mRouteDesc = routeDesc
            return this
        }

        fun setRouteCmt(routeCmt: String?): Builder {
            mRouteCmt = routeCmt
            return this
        }

        fun setRouteSrc(routeSrc: String?): Builder {
            mRouteSrc = routeSrc
            return this
        }

        fun setRouteNumber(routeNumber: Int?): Builder {
            mRouteNumber = routeNumber
            return this
        }

        fun setRouteLink(routeLink: Link?): Builder {
            mRouteLink = routeLink
            return this
        }

        fun setRouteType(routeType: String?): Builder {
            mRouteType = routeType
            return this
        }

        fun build(): Route {
            return Route(this)
        }
    }
}

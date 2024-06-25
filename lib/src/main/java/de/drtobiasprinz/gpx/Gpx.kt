package de.drtobiasprinz.gpx

import io.ticofab.androidgpxparser.parser.domain.Metadata


class Gpx private constructor(builder: Builder) {
    val version: String? = builder.mVersion
    val creator: String? = builder.mCreator
    val metadata: Metadata? = builder.mMetadata
    val wayPoints: List<WayPoint> = builder.mWayPoints ?: emptyList()
    val routes: List<Route> = builder.mRoutes ?: emptyList()
    val tracks: List<Track> = builder.mTracks ?: emptyList()

    class Builder {
        var mWayPoints: List<WayPoint>? = null
        var mRoutes: List<Route>? = null
        var mTracks: List<Track>? = null
        var mVersion: String? = null
        var mCreator: String? = null
        var mMetadata: Metadata? = null

        fun setTracks(tracks: List<Track>?): Builder {
            mTracks = tracks
            return this
        }

        fun setWayPoints(wayPoints: List<WayPoint>?): Builder {
            mWayPoints = wayPoints
            return this
        }

        fun setRoutes(routes: List<Route>?): Builder {
            this.mRoutes = routes
            return this
        }

        fun setVersion(version: String?): Builder {
            mVersion = version
            return this
        }

        fun setCreator(creator: String?): Builder {
            mCreator = creator
            return this
        }

        fun setMetadata(mMetadata: Metadata?): Builder {
            this.mMetadata = mMetadata
            return this
        }

        fun build(): Gpx {
            return Gpx(this)
        }
    }
}

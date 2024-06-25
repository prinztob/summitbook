package de.drtobiasprinz.gpx

import io.ticofab.androidgpxparser.parser.domain.Link

class Track private constructor(builder: Builder) {
    val trackName: String? = builder.mTrackName
    val trackSegments: List<TrackSegment> =
        builder.mTrackSegments ?: emptyList()
    val trackDesc: String? = builder.mTrackDesc
    val trackCmt: String? = builder.mTrackCmt
    val trackSrc: String? = builder.mTrackSrc
    val trackNumber: Int? = builder.mTrackNumber
    val trackLink: Link? = builder.mTrackLink
    val trackType: String? = builder.mTrackType

    class Builder {
        var mTrackName: String? = null
        var mTrackSegments: List<TrackSegment>? = null
        var mTrackDesc: String? = null
        var mTrackCmt: String? = null
        var mTrackSrc: String? = null
        var mTrackNumber: Int? = null
        var mTrackLink: Link? = null
        var mTrackType: String? = null

        fun setTrackName(trackName: String?): Builder {
            mTrackName = trackName
            return this
        }

        fun setTrackDesc(trackDesc: String?): Builder {
            mTrackDesc = trackDesc
            return this
        }

        fun setTrackSegments(trackSegments: List<TrackSegment>?): Builder {
            mTrackSegments = trackSegments
            return this
        }

        fun setTrackCmt(trackCmt: String?): Builder {
            mTrackCmt = trackCmt
            return this
        }

        fun setTrackSrc(trackSrc: String?): Builder {
            mTrackSrc = trackSrc
            return this
        }

        fun setTrackNumber(trackNumber: Int?): Builder {
            mTrackNumber = trackNumber
            return this
        }

        fun setTrackLink(link: Link?): Builder {
            mTrackLink = link
            return this
        }

        fun setTrackType(type: String?): Builder {
            mTrackType = type
            return this
        }

        fun build(): Track {
            return Track(this)
        }
    }
}

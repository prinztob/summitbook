package de.drtobiasprinz.gpx

import org.joda.time.DateTime

abstract class Point internal constructor(builder: Builder) {
    val latitude: Double = builder.mLatitude
    val longitude: Double = builder.mLongitude
    val elevation: Double? = builder.mElevation
    val time: DateTime = builder.mTime
    val name: String? = builder.mName
    val desc: String? = builder.mDesc
    val type: String? = builder.mType
    val sym: String? = builder.mSym
    val cmt: String? = builder.mCmt
    var pointExtension: PointExtension? = builder.mPointExtension

    abstract class Builder {
        var mLatitude: Double =  0.0
        var mLongitude: Double = 0.0
        var mElevation: Double? = null
        var mTime: DateTime = DateTime.now()
        var mName: String? = null
        var mDesc: String? = null
        var mType: String? = null
        var mSym: String? = null
        var mCmt: String? = null
        var mPointExtension: PointExtension? = null

        fun setLatitude(latitude: Double): Builder {
            mLatitude = latitude
            return this
        }

        fun setLongitude(longitude: Double): Builder {
            mLongitude = longitude
            return this
        }

        fun setElevation(elevation: Double): Builder {
            mElevation = elevation
            return this
        }

        fun setTime(time: DateTime): Builder {
            mTime = time
            return this
        }

        fun setName(mame: String?): Builder {
            mName = mame
            return this
        }

        fun setDesc(desc: String?): Builder {
            mDesc = desc
            return this
        }

        fun setType(type: String?): Builder {
            mType = type
            return this
        }

        fun setSym(sym: String?): Builder {
            mSym = sym
            return this
        }

        fun setCmt(cmt: String?): Builder {
            mCmt = cmt
            return this
        }

        fun setExtensions(pointExtension: PointExtension?): Builder {
            mPointExtension = pointExtension
            return this
        }

        abstract fun build(): Point
    }
}

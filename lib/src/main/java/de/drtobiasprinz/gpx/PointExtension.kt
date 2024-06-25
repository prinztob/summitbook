package de.drtobiasprinz.gpx

class PointExtension private constructor(builder: Builder) {
    val speed: Double? = builder.mSpeed
    var distance: Double? = builder.mDistance
    val cadence: Int? = builder.mCadence
    val power: Int? = builder.mPower
    val heartRate: Int? = builder.mHeartRate
    val slope: Double? = builder.mSlope
    val verticalVelocity: Double? = builder.mVerticalVelocity

    class Builder {
        var mSpeed: Double? = null
        var mDistance: Double? = null
        var mCadence: Int? = null
        var mPower: Int? = null
        var mHeartRate: Int? = null
        var mSlope: Double? = null
        var mVerticalVelocity: Double? = null

        fun setSpeed(speed: Double?): Builder {
            mSpeed = speed
            return this
        }

        fun setDistance(distance: Double?): Builder {
            mDistance = distance
            return this
        }

        fun setCadence(cadence: Int?): Builder {
            mCadence = cadence
            return this
        }

        fun setPower(power: Int?): Builder {
            mPower = power
            return this
        }

        fun setHeartRate(heartRate: Int?): Builder {
            mHeartRate = heartRate
            return this
        }

        fun setSlope(slope: Double?): Builder {
            mSlope = slope
            return this
        }

        fun setVerticalVelocity(verticalVelocity: Double?): Builder {
            mVerticalVelocity = verticalVelocity
            return this
        }

        fun build(): PointExtension {
            return PointExtension(this)
        }
    }
}

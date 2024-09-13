package de.drtobiasprinz.summitbook.db.entities

import android.content.res.Resources
import de.drtobiasprinz.summitbook.R

class VelocityData(
    var maxVelocity: Double,
    var oneKilometer: Double = 0.0,
    var fiveKilometer: Double = 0.0,
    var tenKilometers: Double = 0.0,
    var fifteenKilometers: Double = 0.0,
    var twentyKilometers: Double = 0.0,
    var thirtyKilometers: Double = 0.0,
    var fortyKilometers: Double = 0.0,
    var fiftyKilometers: Double = 0.0,
    var seventyFiveKilometers: Double = 0.0,
    var hundredKilometers: Double = 0.0
) {

    override fun toString(): String {
        return "${toStringBase()},${toStringCalculated()}"
    }

    fun toStringBase(): String {
        return "$maxVelocity"
    }

    fun toStringCalculated(): String {
        return "$oneKilometer,$fiveKilometer,$tenKilometers," +
                "$fifteenKilometers,$twentyKilometers,$thirtyKilometers,$fortyKilometers," +
                "$fiftyKilometers,$seventyFiveKilometers,$hundredKilometers"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VelocityData

        if (maxVelocity != other.maxVelocity) return false
        if (oneKilometer != other.oneKilometer) return false
        if (fiveKilometer != other.fiveKilometer) return false
        if (tenKilometers != other.tenKilometers) return false
        if (fifteenKilometers != other.fifteenKilometers) return false
        if (twentyKilometers != other.twentyKilometers) return false
        if (thirtyKilometers != other.thirtyKilometers) return false
        if (fortyKilometers != other.fortyKilometers) return false
        if (fiftyKilometers != other.fiftyKilometers) return false
        if (seventyFiveKilometers != other.seventyFiveKilometers) return false
        if (hundredKilometers != other.hundredKilometers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = maxVelocity.hashCode()
        result = 31 * result + oneKilometer.hashCode()
        result = 31 * result + fiveKilometer.hashCode()
        result = 31 * result + tenKilometers.hashCode()
        result = 31 * result + fifteenKilometers.hashCode()
        result = 31 * result + twentyKilometers.hashCode()
        result = 31 * result + thirtyKilometers.hashCode()
        result = 31 * result + fortyKilometers.hashCode()
        result = 31 * result + fiftyKilometers.hashCode()
        result = 31 * result + seventyFiveKilometers.hashCode()
        result = 31 * result + hundredKilometers.hashCode()
        return result
    }

    fun clone(): VelocityData {
        return VelocityData(
            maxVelocity,
            oneKilometer,
            fiveKilometer,
            tenKilometers,
            fifteenKilometers,
            twentyKilometers,
            thirtyKilometers,
            fortyKilometers,
            fiftyKilometers,
            seventyFiveKilometers,
            hundredKilometers
        )
    }

    fun hasAdditionalData(): Boolean {
        return oneKilometer > 0.0 || fiveKilometer > 0.0 || tenKilometers > 0.0 || fifteenKilometers > 0.0 || twentyKilometers > 0.0 || thirtyKilometers > 0.0 || fortyKilometers > 0.0 || fiftyKilometers > 0.0 || seventyFiveKilometers > 0.0 || hundredKilometers > 0.0
    }

    fun parseCalculatedData(inputData: List<String>): Boolean {
        if (inputData.size == ENTRIES && inputData.any { it != "0.0" }) {
            oneKilometer = inputData[0].toDouble()
            fiveKilometer = inputData[1].toDouble()
            tenKilometers = inputData[2].toDouble()
            fifteenKilometers = inputData[3].toDouble()
            twentyKilometers = inputData[4].toDouble()
            thirtyKilometers = inputData[5].toDouble()
            fortyKilometers = inputData[6].toDouble()
            fiftyKilometers = inputData[7].toDouble()
            seventyFiveKilometers = inputData[8].toDouble()
            hundredKilometers = inputData[9].toDouble()
            return true
        }
        return false
    }

    companion object {

        const val ENTRIES: Int = 10

        fun parse(inputData: List<String>, topSpeed: Double): VelocityData {
            return if (inputData.size == 11) {
                VelocityData(
                    topSpeed,
                    inputData[1].toDouble(),
                    inputData[2].toDouble(),
                    inputData[3].toDouble(),
                    inputData[4].toDouble(),
                    inputData[5].toDouble(),
                    inputData[6].toDouble(),
                    inputData[7].toDouble(),
                    inputData[8].toDouble(),
                    inputData[9].toDouble(),
                    inputData[10].toDouble()
                )
            } else {
                VelocityData(
                    topSpeed,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0
                )
            }
        }

        fun parse(maxVelocity: Double): VelocityData {
            return VelocityData(
                maxVelocity,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
            )
        }

        fun getCsvHeadline(): String {
            return "oneKilometer;fiveKilometer;tenKilometers;fifteenKilometers;twentyKilometers;thirtyKilometers;fortyKilometers;fiftyKilometers;seventyFiveKilometers;hundredKilometers"
        }

        fun getCsvDescription(resources: Resources): String {
            return "${resources.getString(R.string.optional)};" +
                    "${resources.getString(R.string.optional)};" +
                    "${resources.getString(R.string.optional)};" +
                    "${resources.getString(R.string.optional)};" +
                    "${resources.getString(R.string.optional)};" +
                    "${resources.getString(R.string.optional)};" +
                    "${resources.getString(R.string.optional)};" +
                    "${resources.getString(R.string.optional)};" +
                    "${resources.getString(R.string.optional)};" +
                    "${resources.getString(R.string.optional)};"
        }

    }

}

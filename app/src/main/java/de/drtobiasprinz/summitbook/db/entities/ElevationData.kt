package de.drtobiasprinz.summitbook.db.entities

import android.content.res.Resources
import de.drtobiasprinz.summitbook.R

class ElevationData(
    var maxElevation: Int = 0,
    var elevationGain: Int = 0,
    var maxVerticalVelocity1Min: Double = 0.0,
    var maxVerticalVelocity10Min: Double = 0.0,
    var maxVerticalVelocity1h: Double = 0.0,
    var maxSlope: Double = 0.0
) {

    var avgVelocity: Double = 0.0

    override fun toString(): String {
        return "${toStringBase()},${toStringCalculated()}"
    }

    fun toStringBase(): String {
        return "$maxElevation,$elevationGain"
    }

    fun toStringCalculated(): String {
        return "$maxVerticalVelocity1Min,$maxVerticalVelocity10Min,$maxVerticalVelocity1h,$maxSlope"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ElevationData

        if (maxElevation != other.maxElevation) return false
        if (elevationGain != other.elevationGain) return false
        if (maxVerticalVelocity1Min != other.maxVerticalVelocity1Min) return false
        if (maxVerticalVelocity10Min != other.maxVerticalVelocity10Min) return false
        if (maxVerticalVelocity1h != other.maxVerticalVelocity1h) return false
        if (maxSlope != other.maxSlope) return false

        return true
    }

    override fun hashCode(): Int {
        var result = maxElevation
        result = 31 * result + elevationGain
        result = 31 * result + maxVerticalVelocity1Min.hashCode()
        result = 31 * result + maxVerticalVelocity10Min.hashCode()
        result = 31 * result + maxVerticalVelocity1h.hashCode()
        result = 31 * result + maxSlope.hashCode()
        return result
    }

    fun clone(): ElevationData {
        return ElevationData(
            maxElevation,
            elevationGain,
            maxVerticalVelocity1Min,
            maxVerticalVelocity10Min,
            maxVerticalVelocity1h,
            maxSlope
        )
    }

    fun hasAdditionalData(): Boolean {
        return maxVerticalVelocity1Min > 0.0 || maxVerticalVelocity10Min > 0.0 || maxVerticalVelocity1h > 0.0 || maxSlope > 0.0
    }

    fun parseCalculatedData(inputData: List<String>): Boolean {
        if (inputData.size == ENTRIES && inputData.any { it != "0.0" }) {
            maxVerticalVelocity1Min = inputData[0].toDouble()
            maxVerticalVelocity10Min = inputData[1].toDouble()
            maxVerticalVelocity1h = inputData[2].toDouble()
            maxSlope = inputData[3].toDouble()
            return true
        }
        return false
    }

    companion object {
        const val ENTRIES: Int = 4

        fun parse(inputData: List<String>, maxElevation: Int): ElevationData {
            return when (inputData.size) {
                3 -> {
                    ElevationData(
                        maxElevation, inputData[0].toInt(),
                        inputData[1].toDouble(), inputData[2].toDouble()
                    )
                }

                5 -> {
                    ElevationData(
                        maxElevation, inputData[0].toInt(),
                        inputData[1].toDouble(), inputData[2].toDouble(), inputData[3].toDouble(),
                        inputData[4].toDouble()
                    )
                }

                else -> {
                    val elevationGain = if (inputData.size == 1) inputData.first().toInt() else 0
                    ElevationData(maxElevation, elevationGain)
                }
            }
        }

        fun parse(maxElevation: Int, elevationGain: Int): ElevationData {
            return ElevationData(maxElevation, elevationGain)
        }

        fun getCsvHeadline(): String {
            return "maxVerticalVelocity1Min;maxVerticalVelocity10Min;maxVerticalVelocity1h;maxSlope"
        }

        fun getCsvDescription(resources: Resources): String {
            return "${resources.getString(R.string.optional)};" +
                    "${resources.getString(R.string.optional)};" +
                    "${resources.getString(R.string.optional)};" +
                    "${resources.getString(R.string.optional)};"
        }

    }

}

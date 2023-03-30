package de.drtobiasprinz.summitbook.db.entities

class ElevationData constructor(
        var maxElevation: Int,
        var elevationGain: Int,
        var maxVerticalVelocity1Min: Double = 0.0,
        var maxVerticalVelocity10Min: Double = 0.0,
        var maxVerticalVelocity1h: Double = 0.0,
        var maxSlope: Double = 0.0
) {


    override fun toString(): String {
        return "$elevationGain,$maxVerticalVelocity1Min,$maxVerticalVelocity10Min,$maxVerticalVelocity1h,$maxSlope"
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
        return ElevationData(maxElevation, elevationGain, maxVerticalVelocity1Min, maxVerticalVelocity10Min, maxVerticalVelocity1h, maxSlope)
    }

    fun hasAdditionalData(): Boolean {
        return maxVerticalVelocity1Min > 0.0 || maxVerticalVelocity10Min > 0.0 || maxVerticalVelocity1h > 0.0 || maxSlope > 0.0
    }

    companion object {

        fun parse(inputData: List<String>, maxElevation: Int): ElevationData {
            return when (inputData.size) {
                3 -> {
                    ElevationData(maxElevation, inputData[0].toInt(),
                            inputData[1].toDouble(), inputData[2].toDouble())
                }
                5 -> {
                    ElevationData(maxElevation, inputData[0].toInt(),
                            inputData[1].toDouble(), inputData[2].toDouble(), inputData[3].toDouble(),
                            inputData[4].toDouble())
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
    }

}

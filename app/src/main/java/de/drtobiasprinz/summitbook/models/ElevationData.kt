package de.drtobiasprinz.summitbook.models

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


    companion object {

        fun parse(inputData: List<String>, maxElevation: Int): ElevationData {
            if (inputData.size == 3) {
                return ElevationData(maxElevation, inputData[0].toInt(),
                        inputData[1].toDouble(), inputData[2].toDouble())
            } else if (inputData.size == 5) {
                return ElevationData(maxElevation, inputData[0].toInt(),
                        inputData[1].toDouble(), inputData[2].toDouble(), inputData[3].toDouble(),
                        inputData[4].toDouble())
            } else {
                val elevationGain = if (inputData.size == 1) inputData.first().toInt() else 0
                return ElevationData(maxElevation, elevationGain)
            }
        }

        fun parse(maxElevation: Int, elevationGain: Int): ElevationData {
            return ElevationData(maxElevation, elevationGain)
        }
    }

}

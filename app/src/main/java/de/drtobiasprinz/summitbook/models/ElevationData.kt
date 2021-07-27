package de.drtobiasprinz.summitbook.models

class ElevationData constructor(
        var maxElevation: Int, var elevationGain: Int, var maxVerticalVelocity: Double = 0.0,
        var maxSlope: Double = 0.0
) {


    override fun toString(): String {
        return "$elevationGain,$maxVerticalVelocity,$maxSlope"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ElevationData

        if (maxElevation != other.maxElevation) return false
        if (elevationGain != other.elevationGain) return false
        if (maxVerticalVelocity != other.maxVerticalVelocity) return false
        if (maxSlope != other.maxSlope) return false

        return true
    }

    override fun hashCode(): Int {
        var result = maxElevation
        result = 31 * result + elevationGain
        result = 31 * result + maxVerticalVelocity.hashCode()
        result = 31 * result + maxSlope.hashCode()
        return result
    }


    companion object {

        fun parse(inputData: List<String>, maxElevation: Int): ElevationData {
            if (inputData.size == 3) {
                return ElevationData(maxElevation, inputData[0].toInt(),
                        inputData[1].toDouble(), inputData[2].toDouble())
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

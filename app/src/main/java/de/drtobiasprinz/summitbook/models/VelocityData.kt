package de.drtobiasprinz.summitbook.models

class VelocityData constructor(
        var avgVelocity: Double, var maxVelocity: Double,
        var oneKilometer: Double, var fiveKilometer: Double, var tenKilometers: Double,
        var fifteenKilometers: Double, var twentyKilometers: Double, var thirtyKilometers: Double,
        var fortyKilometers: Double, var fiftyKilometers: Double, var seventyFiveKilometers: Double,
        var hundredKilometers: Double
) {


    override fun toString(): String {
        return "$avgVelocity,$oneKilometer,$fiveKilometer,$tenKilometers," +
                "$fifteenKilometers,$twentyKilometers,$thirtyKilometers,$fortyKilometers," +
                "$fiftyKilometers,$seventyFiveKilometers,$hundredKilometers"
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VelocityData

        if (avgVelocity != other.avgVelocity) return false
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
        var result = avgVelocity.hashCode()
        result = 31 * result + maxVelocity.hashCode()
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

    companion object {

        fun parse(inputData: List<String>, topSpeed: Double): VelocityData {
            if (inputData.size == 11) {
                return VelocityData(inputData[0].toDouble(), topSpeed,
                        inputData[1].toDouble(), inputData[2].toDouble(), inputData[3].toDouble(), inputData[4].toDouble(),
                        inputData[5].toDouble(), inputData[6].toDouble(), inputData[7].toDouble(), inputData[8].toDouble(),
                        inputData[9].toDouble(), inputData[10].toDouble())
            } else {
                val avgVelocity = if (inputData.size == 1) inputData.first().toDouble() else 0.0
                return VelocityData(avgVelocity,topSpeed,0.0, 0.0, 0.0, 0.0,0.0,0.0,0.0,0.0,0.0,0.0)
            }
        }

        fun parse(avgVelocity: Double, maxVelocity: Double): VelocityData {
            return VelocityData(avgVelocity,maxVelocity,0.0, 0.0, 0.0, 0.0,0.0,0.0,0.0,0.0,0.0,0.0)
        }
    }

}

package de.drtobiasprinz.summitbook.models

class PowerData constructor(
        var avgPower: Float, var maxPower: Float, var normPower: Float,
        var oneSec: Int, var twoSec: Int, var fiveSec: Int, var tenSec: Int, var twentySec: Int, var thirtySec: Int,
        var oneMin: Int, var twoMin: Int, var fiveMin: Int, var tenMin: Int, var twentyMin: Int, var thirtyMin: Int,
        var oneHour: Int, var twoHours: Int, var fiveHours: Int
) {

    override fun toString(): String {
        return "$avgPower,$maxPower,$normPower,$oneSec,$twoSec,$fiveSec,$tenSec,$twentySec,$thirtySec,$oneMin,$twoMin,$fiveMin," +
                "$tenMin,$twentyMin,$thirtyMin,$oneHour,$twoHours,$fiveHours"
    }

    fun hasPowerData(): Boolean {
        return avgPower > 0f
    }

    companion object {

        fun parse(data: List<String>): PowerData {
            return if (data.size == 18) {
                PowerData(data[0].toFloat(), data[1].toFloat(), data[2].toFloat(),
                        data[3].toInt(), data[4].toInt(), data[5].toInt(), data[6].toInt(),
                        data[7].toInt(), data[8].toInt(), data[9].toInt(), data[10].toInt(),
                        data[11].toInt(), data[12].toInt(), data[13].toInt(), data[14].toInt(),
                        data[15].toInt(), data[16].toInt(), data[17].toInt())
            } else {
                PowerData(0f,0f,0f,0, 0, 0, 0,0,0,0,0,0,0,0,0,0,0,0)
            }
        }
    }

}

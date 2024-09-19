package de.drtobiasprinz.summitbook.db.entities

class PowerData(
    var avgPower: Float,
    var maxPower: Float,
    var normPower: Float,
    var oneSec: Int = 0,
    var twoSec: Int = 0,
    var fiveSec: Int = 0,
    var tenSec: Int = 0,
    var twentySec: Int = 0,
    var thirtySec: Int = 0,
    var oneMin: Int = 0,
    var twoMin: Int = 0,
    var fiveMin: Int = 0,
    var tenMin: Int = 0,
    var twentyMin: Int = 0,
    var thirtyMin: Int = 0,
    var oneHour: Int = 0,
    var twoHours: Int = 0,
    var fiveHours: Int = 0
) {

    override fun toString(): String {
        return "$avgPower,$maxPower,$normPower,$oneSec,$twoSec,$fiveSec,$tenSec,$twentySec,$thirtySec,$oneMin,$twoMin,$fiveMin," +
                "$tenMin,$twentyMin,$thirtyMin,$oneHour,$twoHours,$fiveHours"
    }

    fun hasPowerData(): Boolean {
        return avgPower > 0f || oneSec > 0
    }

    companion object {

        fun parse(data: List<String>): PowerData {
            return if (data.size == 18) {
                PowerData(
                    data[0].toFloat(), data[1].toFloat(), data[2].toFloat(),
                    data[3].toInt(), data[4].toInt(), data[5].toInt(), data[6].toInt(),
                    data[7].toInt(), data[8].toInt(), data[9].toInt(), data[10].toInt(),
                    data[11].toInt(), data[12].toInt(), data[13].toInt(), data[14].toInt(),
                    data[15].toInt(), data[16].toInt(), data[17].toInt()
                )
            } else {
                PowerData(0f, 0f, 0f, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
            }
        }
    }

}

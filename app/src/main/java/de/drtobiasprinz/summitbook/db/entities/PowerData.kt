package de.drtobiasprinz.summitbook.db.entities

import com.google.gson.JsonObject
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor.Companion.getJsonObjectEntryNotNull

class PowerData(
    var avgPower: Float = 0f,
    var maxPower: Float = 0f,
    var normPower: Float = 0f,
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
    var threeHours: Int = 0,
    var fourHours: Int = 0,
    var fiveHours: Int = 0,
    var trainingStressScore: Float = 0f,
    var intensityFactor: Float = 0f,

    ) {

    override fun toString(): String {
        return "$avgPower,$maxPower,$normPower,$oneSec,$twoSec,$fiveSec,$tenSec,$twentySec,$thirtySec,$oneMin,$twoMin,$fiveMin," +
                "$tenMin,$twentyMin,$thirtyMin,$oneHour,$twoHours,$threeHours,$fourHours,$fiveHours,$trainingStressScore,$intensityFactor"
    }

    fun hasPowerData(): Boolean {
        return avgPower > 0f || oneSec > 0
    }

    companion object {

        fun parse(data: List<String>): PowerData {
            return when (data.size) {
                18 -> {
                    PowerData(
                        data[0].toFloat(), data[1].toFloat(), data[2].toFloat(),
                        data[3].toInt(), data[4].toInt(), data[5].toInt(), data[6].toInt(),
                        data[7].toInt(), data[8].toInt(), data[9].toInt(), data[10].toInt(),
                        data[11].toInt(), data[12].toInt(), data[13].toInt(), data[14].toInt(),
                        data[15].toInt(), data[16].toInt(), 0, 0, data[17].toInt()
                    )
                }
                22 -> {
                    PowerData(
                        data[0].toFloat(), data[1].toFloat(), data[2].toFloat(),
                        data[3].toInt(), data[4].toInt(), data[5].toInt(), data[6].toInt(),
                        data[7].toInt(), data[8].toInt(), data[9].toInt(), data[10].toInt(),
                        data[11].toInt(), data[12].toInt(), data[13].toInt(), data[14].toInt(),
                        data[15].toInt(), data[16].toInt(), data[17].toInt(), data[18].toInt(),
                        data[19].toInt(), data[20].toFloat(), data[21].toFloat()
                    )
                }
                else -> {
                    PowerData()
                }
            }
        }

        fun parseFromGarminJson(jsonObject: JsonObject): PowerData {
            val garminData = PowerData()
            GarminPowerDataEntity.entries.forEach {
                it.updateGarminPowerData(
                    garminData,
                    jsonObject
                )
            }
            return garminData
        }
    }

}

enum class GarminPowerDataEntity(
    val updateGarminPowerData: (PowerData, JsonObject) -> Unit
) {
    AvgPower({ data, json -> data.avgPower = getJsonObjectEntryNotNull(json, "avgPower") }),
    MaxPower({ data, json -> data.maxPower = getJsonObjectEntryNotNull(json, "maxPower") }),
    NormPower({ data, json -> data.normPower = getJsonObjectEntryNotNull(json, "normPower") }),
    MaxAvgPower1s({ data, json ->
        data.oneSec = getJsonObjectEntryNotNull(json, "maxAvgPower_1").toInt()
    }),
    MaxAvgPower2s({ data, json ->
        data.twoSec = getJsonObjectEntryNotNull(json, "maxAvgPower_2").toInt()
    }),
    MaxAvgPower5s({ data, json ->
        data.fiveSec = getJsonObjectEntryNotNull(json, "maxAvgPower_5").toInt()
    }),
    MaxAvgPower10s({ data, json ->
        data.tenSec = getJsonObjectEntryNotNull(json, "maxAvgPower_10").toInt()
    }),
    MaxAvgPower20s({ data, json ->
        data.twentySec = getJsonObjectEntryNotNull(json, "maxAvgPower_20").toInt()
    }),
    MaxAvgPower30s({ data, json ->
        data.thirtySec = getJsonObjectEntryNotNull(json, "maxAvgPower_30").toInt()
    }),
    MaxAvgPower60s({ data, json ->
        data.oneMin = getJsonObjectEntryNotNull(json, "maxAvgPower_60").toInt()
    }),
    MaxAvgPower120s({ data, json ->
        data.twoMin = getJsonObjectEntryNotNull(json, "maxAvgPower_120").toInt()
    }),
    MaxAvgPower300s({ data, json ->
        data.fiveMin = getJsonObjectEntryNotNull(json, "maxAvgPower_300").toInt()
    }),
    MaxAvgPower600s({ data, json ->
        data.tenMin = getJsonObjectEntryNotNull(json, "maxAvgPower_600").toInt()
    }),
    MaxAvgPower1200s({ data, json ->
        data.twentyMin = getJsonObjectEntryNotNull(json, "maxAvgPower_1200").toInt()
    }),
    MaxAvgPower1800s({ data, json ->
        data.thirtyMin = getJsonObjectEntryNotNull(json, "maxAvgPower_1800").toInt()
    }),
    MaxAvgPower3600s({ data, json ->
        data.oneHour = getJsonObjectEntryNotNull(json, "maxAvgPower_3600").toInt()
    }),
    MaxAvgPower7200s({ data, json ->
        data.twoHours = getJsonObjectEntryNotNull(json, "maxAvgPower_7200").toInt()
    }),
    MaxAvgPower10800s({ data, json ->
        data.threeHours = getJsonObjectEntryNotNull(json, "maxAvgPower_10800").toInt()
    }),
    MaxAvgPower14400s({ data, json ->
        data.fourHours = getJsonObjectEntryNotNull(json, "maxAvgPower_14400").toInt()
    }),
    MaxAvgPower18000s({ data, json ->
        data.fiveHours = getJsonObjectEntryNotNull(json, "maxAvgPower_18000").toInt()
    }),
    TrainingStressScore({ data, json ->
        data.trainingStressScore = getJsonObjectEntryNotNull(json, "trainingStressScore")
    }),
    IntensityFactor({ data, json ->
        data.intensityFactor = getJsonObjectEntryNotNull(json, "intensityFactor")
    }),
}

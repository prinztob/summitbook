package de.drtobiasprinz.summitbook.db.entities

import com.google.gson.JsonObject
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor.Companion.getJsonObjectEntryNotNull
import kotlin.math.roundToInt

class CyclingDynamicsData(
    var leftBalance: Float = 0f,
    var rightBalance: Float = 0f,
    var leftTorqueEffectiveness: Float = 0f,
    var rightTorqueEffectiveness: Float = 0f,
    var leftPedalSmoothness: Float = 0f,
    var rightPedalSmoothness: Float = 0f,
    var totalNumberOfStrokes: Int = 0,
    var standingTime: Int = 0,
    var maxStandingPower: Int = 0,
    var averageStandingPower: Int = 0,
    var leftPowerPhaseStart: Int = 0,
    var leftPowerPhaseEnd: Int = 0,
    var leftPowerPhaseArcCenter: Int = 0,
    var leftPowerPhasePeakStart: Int = 0,
    var leftPowerPhasePeakEnd: Int = 0,
    var leftPowerPhasePeakArcCenter: Int = 0,
    var leftPlatformCenterOffset: Int = 0,
    var rightPowerPhaseStart: Int = 0,
    var rightPowerPhaseEnd: Int = 0,
    var rightPowerPhaseArcCenter: Int = 0,
    var rightPowerPhasePeakStart: Int = 0,
    var rightPowerPhasePeakEnd: Int = 0,
    var rightPowerPhasePeakArcCenter: Int = 0,
    var rightPlatformCenterOffset: Int = 0,

    ) {

    override fun toString(): String {
        return "$leftBalance,$rightBalance,$leftTorqueEffectiveness,$rightTorqueEffectiveness," +
                "$leftPedalSmoothness,$rightPedalSmoothness," +
                "$totalNumberOfStrokes,$standingTime,$maxStandingPower,$averageStandingPower," +
                "$leftPowerPhaseStart,$leftPowerPhaseEnd,$leftPowerPhaseArcCenter," +
                "$leftPowerPhasePeakStart,$leftPowerPhasePeakEnd,$leftPowerPhasePeakArcCenter," +
                "$leftPlatformCenterOffset,$rightPowerPhaseStart,$rightPowerPhaseEnd," +
                "$rightPowerPhaseArcCenter,$rightPowerPhasePeakStart,$rightPowerPhasePeakEnd," +
                "$rightPowerPhasePeakArcCenter,$rightPlatformCenterOffset"
    }

    fun hasCyclingDynamics(): Boolean {
        return leftBalance > 0f || leftTorqueEffectiveness > 0
    }

    companion object {

        fun parse(data: List<String>): CyclingDynamicsData {
            return if (data.size == 24) {
                CyclingDynamicsData(
                    data[0].toFloat(), data[1].toFloat(), data[2].toFloat(),
                    data[3].toFloat(), data[4].toFloat(), data[5].toFloat(), data[6].toInt(),
                    data[7].toInt(), data[8].toInt(), data[9].toInt(), data[10].toInt(),
                    data[11].toInt(), data[12].toInt(), data[13].toInt(), data[14].toInt(),
                    data[15].toInt(), data[16].toInt(), data[17].toInt(),
                    data[18].toInt(), data[19].toInt(), data[20].toInt(), data[21].toInt(),
                    data[22].toInt(), data[23].toInt()
                )
            } else {
                CyclingDynamicsData()
            }
        }

        fun parseCyclingDynamicsFromGarminJson(jsonObject: JsonObject): CyclingDynamicsData {
            val data = CyclingDynamicsData()
            GarminCyclingDynamicsDataEntity.entries.forEach {
                it.updateGarminCyclingDynamicsData(
                    data,
                    jsonObject
                )
            }
            return data
        }
    }

}

enum class GarminCyclingDynamicsDataEntity(
    val updateGarminCyclingDynamicsData: (CyclingDynamicsData, JsonObject) -> Unit
) {
    LeftBalance({ data, json ->
        data.leftBalance = getJsonObjectEntryNotNull(json, "leftBalance")
    }),
    RightBalance({ data, json ->
        data.rightBalance = getJsonObjectEntryNotNull(json, "rightBalance")
    }),
    LeftTorqueEffectiveness({ data, json ->
        data.leftTorqueEffectiveness = getJsonObjectEntryNotNull(json, "leftTorqueEffectiveness")
    }),
    RightTorqueEffectiveness({ data, json ->
        data.rightTorqueEffectiveness = getJsonObjectEntryNotNull(json, "rightTorqueEffectiveness")
    }),
    LeftPedalSmoothness({ data, json ->
        data.leftPedalSmoothness = getJsonObjectEntryNotNull(json, "leftPedalSmoothness")
    }),
    RightPedalSmoothness({ data, json ->
        data.rightPedalSmoothness = getJsonObjectEntryNotNull(json, "rightPedalSmoothness")
    }),
    TotalNumberOfStrokes({ data, json ->
        data.totalNumberOfStrokes =
            getJsonObjectEntryNotNull(json, "totalNumberOfStrokes").roundToInt()
    }),
    StandingTime({ data, json ->
        data.standingTime = getJsonObjectEntryNotNull(json, "standingTime").roundToInt()
    }),
    MaxStandingPower({ data, json ->
        data.maxStandingPower = getJsonObjectEntryNotNull(json, "maxStandingPower").roundToInt()
    }),
    AverageStandingPower({ data, json ->
        data.averageStandingPower =
            getJsonObjectEntryNotNull(json, "averageStandingPower").roundToInt()
    }),
    LeftPowerPhaseStart({ data, json ->
        data.leftPowerPhaseStart =
            getJsonObjectEntryNotNull(json, "leftPowerPhaseStart").roundToInt()
    }),
    LeftPowerPhaseEnd({ data, json ->
        data.leftPowerPhaseEnd = getJsonObjectEntryNotNull(json, "leftPowerPhaseEnd").roundToInt()
    }),
    LeftPowerPhaseArcCenter({ data, json ->
        data.leftPowerPhaseArcCenter =
            getJsonObjectEntryNotNull(json, "leftPowerPhaseArcCenter").roundToInt()
    }),
    LeftPowerPhasePeakStart({ data, json ->
        data.leftPowerPhasePeakStart =
            getJsonObjectEntryNotNull(json, "leftPowerPhasePeakStart").roundToInt()
    }),
    LeftPowerPhasePeakEnd({ data, json ->
        data.leftPowerPhasePeakEnd =
            getJsonObjectEntryNotNull(json, "leftPowerPhasePeakEnd").roundToInt()
    }),
    LeftPowerPhasePeakArcCenter({ data, json ->
        data.leftPowerPhasePeakArcCenter =
            getJsonObjectEntryNotNull(json, "leftPowerPhasePeakArcCenter").roundToInt()
    }),
    LeftPlatformCenterOffset({ data, json ->
        data.leftPlatformCenterOffset =
            getJsonObjectEntryNotNull(json, "leftPlatformCenterOffset").roundToInt()
    }),
    RightPowerPhaseStart({ data, json ->
        data.rightPowerPhaseStart =
            getJsonObjectEntryNotNull(json, "rightPowerPhaseStart").roundToInt()
    }),
    RightPowerPhaseEnd({ data, json ->
        data.rightPowerPhaseEnd = getJsonObjectEntryNotNull(json, "rightPowerPhaseEnd").roundToInt()
    }),
    RightPowerPhaseArcCenter({ data, json ->
        data.rightPowerPhaseArcCenter =
            getJsonObjectEntryNotNull(json, "rightPowerPhaseArcCenter").roundToInt()
    }),
    RightPowerPhasePeakStart({ data, json ->
        data.rightPowerPhasePeakStart =
            getJsonObjectEntryNotNull(json, "rightPowerPhasePeakStart").roundToInt()
    }),
    RightPowerPhasePeakEnd({ data, json ->
        data.rightPowerPhasePeakEnd =
            getJsonObjectEntryNotNull(json, "rightPowerPhasePeakEnd").roundToInt()
    }),
    RightPowerPhasePeakArcCenter({ data, json ->
        data.rightPowerPhasePeakArcCenter =
            getJsonObjectEntryNotNull(json, "rightPowerPhasePeakArcCenter").roundToInt()
    }),
    RightPlatformCenterOffset({ data, json ->
        data.rightPlatformCenterOffset =
            getJsonObjectEntryNotNull(json, "rightPlatformCenterOffset").roundToInt()
    }),
}

package de.drtobiasprinz.summitbook.db.entities

import android.content.res.Resources
import androidx.room.Embedded
import androidx.room.Ignore
import com.google.gson.JsonObject
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor.Companion.getJsonObjectEntryNotNull
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor.Companion.roundToTwoDigits
import de.drtobiasprinz.summitbook.utils.ZipFileVersions
import java.util.Objects
import kotlin.math.roundToInt

class GarminData(
    var activityIds: MutableList<String>,
    var calories: Float = 0f,
    var averageHR: Float = 0f,
    var maxHR: Float = 0f,
    @Embedded var power: PowerData = PowerData(),
    var ftp: Int = 0,
    var vo2max: Float = 0f,
    var aerobicTrainingEffect: Float = 0f,
    var anaerobicTrainingEffect: Float = 0f,
    var grit: Float = 0f,
    var flow: Float = 0f,
    var trainingLoad: Float = 0f,
    var waterEstimated: Int = 0,
    var gainSolarActivityTime: Int = 0,
    var avgSolarChargePercent: Int = 0,
    var surfaceTypeUnpavedPercentage: Float = 0f,
    @Embedded var cyclingDynamics: CyclingDynamicsData = CyclingDynamicsData(),
) {

    @Ignore
    val activityId = activityIds.first()

    @Ignore
    val url = "https://connect.garmin.com/modern/activity/$activityId"

    @Ignore
    var duration = 0.0

    fun getStringRepresentation(summitActivityId: Long): String {
        return "${summitActivityId};${toString()}"
    }

    override fun toString(): String {
        return activityIds.joinToString(",") + ';' +
                calories + ';' +
                averageHR + ';' +
                maxHR + ';' +
                power.toString() + ';' +
                ftp + ';' +
                vo2max + ';' +
                aerobicTrainingEffect + ';' +
                anaerobicTrainingEffect + ';' +
                grit + ';' +
                flow + ';' +
                trainingLoad + ';' +
                waterEstimated + ';' +
                gainSolarActivityTime + ';' +
                avgSolarChargePercent + ';' +
                surfaceTypeUnpavedPercentage + ';' +
                cyclingDynamics.toString() + "\n"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as GarminData?
        return activityIds == that?.activityIds
    }

    override fun hashCode(): Int {
        return Objects.hash(activityIds)
    }

    companion object {
        fun getCsvHeadline(resources: Resources): String {
            return "activityId; " +
                    "${resources.getString(R.string.calories)}; " +
                    "${resources.getString(R.string.average_hr)} (${resources.getString(R.string.bpm)}); " +
                    "${resources.getString(R.string.max_hr)} (${resources.getString(R.string.bpm)}); " +
                    "powerData (W/1s - W/5h); " +
                    "FTP; " +
                    "VO2MAX; " +
                    "aerobicTrainingEffect; " +
                    "anaerobicTrainingEffect; " +
                    "grit; " +
                    "flow; " +
                    "trainingsload;" +
                    "waterEstimated;" +
                    "gainSolarActivityTime;" +
                    "avgSolarChargePercent;" +
                    "surfaceTypeUnpavedPercentage;" +
                    "cyclingDynamics" + "\n"
        }

        fun getCsvDescription(resources: Resources): String {
            return "${resources.getString(R.string.required)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)}; " +
                    "${resources.getString(R.string.optional)};\n"
        }

        fun parseFromCsvFileLineAndSave(
            line: String,
            allSummits: MutableList<Summit>,
            saveSummit: (Boolean, Summit) -> Unit,
            zipFileVersions: ZipFileVersions
        ): Boolean {
            val activityId = line.split(";").first().toLong()
            val summit = allSummits.find { it.activityId == activityId }
            if (summit != null) {
                val garminData = parseFromCsvFileLine(line, zipFileVersions)
                if (garminData != null) {
                    summit.garminData = garminData
                    saveSummit(true, summit)
                    return true
                }
            }
            return false
        }

        fun parseFromCsvFileLine(
            line: String,
            zipFileVersions: ZipFileVersions
        ): GarminData? {
            return zipFileVersions.getGarminData(line)
        }

        fun parseFromGarminJson(
            activityIds: MutableList<String>,
            activityJsonObject: JsonObject,
            parentJsonObject: JsonObject? = null,
            gsonExerciseSet: JsonObject? = null
        ): GarminData {
            val garminData = GarminData(activityIds)
            GarminEntityFromActivityJson.entries.forEach {
                it.updateGarminData(
                    garminData,
                    activityJsonObject,
                    parentJsonObject
                )
            }
            if (gsonExerciseSet != null && gsonExerciseSet.has("summaryDTO")) {
                val summaryDTO = gsonExerciseSet.getAsJsonObject("summaryDTO")
                GarminEntityFromSplitJson.entries.forEach {
                    it.updateGarminData(
                        garminData,
                        summaryDTO
                    )
                }
            }
            return garminData
        }
    }
}

enum class GarminEntityFromActivityJson(
    val updateGarminData: (GarminData, JsonObject, JsonObject?) -> Unit
) {
    Calories({ data, json, _ -> data.calories = getJsonObjectEntryNotNull(json, "calories") }),
    AverageHR({ data, json, _ ->
        data.averageHR = getJsonObjectEntryNotNull(json, "averageHR")
    }),
    MaxHR({ data, json, _ -> data.maxHR = getJsonObjectEntryNotNull(json, "maxHR") }),
    Power({ data, json, _ -> data.power = PowerData.parseFromGarminJson(json) }),
    AerobicTrainingEffect({ data, json, _ ->
        data.maxHR = getJsonObjectEntryNotNull(json, "aerobicTrainingEffect")
    }),
    AnaerobicTrainingEffect({ data, json, _ ->
        data.maxHR = getJsonObjectEntryNotNull(json, "anaerobicTrainingEffect")
    }),
    Grit({ data, json, _ -> data.maxHR = getJsonObjectEntryNotNull(json, "grit") }),
    AvgFlow({ data, json, _ -> data.maxHR = getJsonObjectEntryNotNull(json, "avgFlow") }),
    ActivityTrainingLoad({ data, json, _ ->
        data.maxHR = getJsonObjectEntryNotNull(json, "activityTrainingLoad")
    }),
    Vo2Max({ data, json, jsonParent ->
        val usedJson = jsonParent ?: json
        data.vo2max = if (usedJson.has("vo2MaxPreciseValue")) {
            roundToTwoDigits(getJsonObjectEntryNotNull(usedJson, "vo2MaxPreciseValue"))
        } else if (usedJson.has("vO2MaxValue")) {
            roundToTwoDigits(getJsonObjectEntryNotNull(usedJson, "vO2MaxValue"))
        } else {
            0.0f
        }
    }),
}

enum class GarminEntityFromSplitJson(
    val updateGarminData: (GarminData, JsonObject) -> Unit
) {
    WaterEstimated({ data, json ->
        data.waterEstimated = getJsonObjectEntryNotNull(json, "waterEstimated").roundToInt()
    }),
    GainSolarActivityTime({ data, json ->
        data.gainSolarActivityTime =
            getJsonObjectEntryNotNull(json, "gainSolarActivityTime").roundToInt()
    }),
    AvgSolarChargePercent({ data, json ->
        data.avgSolarChargePercent =
            getJsonObjectEntryNotNull(json, "avgSolarChargePercent").roundToInt()
    }),
    SurfaceTypeUnpavedPercentage({ data, json ->
        data.surfaceTypeUnpavedPercentage =
            getJsonObjectEntryNotNull(json, "surfaceTypeUnpavedPercentage")
    }),
    CyclingDynamics({ data, json ->
        data.cyclingDynamics =
            CyclingDynamicsData.parseCyclingDynamicsFromGarminJson(json)
    }),
    FTP({ data, json ->
        data.ftp = getJsonObjectEntryNotNull(
            json,
            "functionalThresholdPower"
        ).roundToInt()
    })
}
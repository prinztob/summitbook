package de.drtobiasprinz.summitbook.db.entities

import android.content.res.Resources
import androidx.room.Embedded
import androidx.room.Ignore
import de.drtobiasprinz.summitbook.R
import java.util.Objects

class GarminData(
    var activityIds: MutableList<String>,
    var calories: Float,
    var averageHR: Float,
    var maxHR: Float,
    @Embedded var power: PowerData,
    var ftp: Int,
    var vo2max: Float,
    var aerobicTrainingEffect: Float,
    var anaerobicTrainingEffect: Float,
    var grit: Float,
    var flow: Float,
    var trainingLoad: Float,
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
                trainingLoad
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
                    "garminid; " +
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
                    "trainingsload;\n"
        }

        fun getCsvDescription(resources: Resources): String {
            return "${resources.getString(R.string.required)}; " +
                    "${resources.getString(R.string.required)}; " +
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
            saveSummit: (Boolean, Summit) -> Unit
        ): Boolean {
            val activityId = line.split(";").first().toLong()
            val summit = allSummits.find { it.activityId == activityId }
            if (summit != null) {
                val garminData = parseFromCsvFileLine(line)
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
        ): GarminData? {
            val regex =
                """(?<activityId>(\d+));(?<garminIds>([\d,]+));(?<cal>([\d.]+));(?<averageHR>([\d.]+));(?<maxHR>([\d.]+));(?<power>([\d,.]+));(?<ftp>(\d+));(?<vo2max>([\d.]+));(?<aerobicTrainingEffect>([\d.]+));(?<anaerobicTrainingEffect>([\d.]+));(?<grit>([\d.]+));(?<flow>([\d.]+));(?<trainingLoad>([\d.]+))""".toRegex()
            val matchResult = regex.find(line.replace("\n", ""))
            if (matchResult != null) {
                return GarminData(
                    matchResult.groups["garminIds"]!!.value.split(",") as MutableList<String>,
                    matchResult.groups["cal"]!!.value.toFloat(),
                    matchResult.groups["averageHR"]!!.value.toFloat(),
                    matchResult.groups["maxHR"]!!.value.toFloat(),
                    PowerData.parse(matchResult.groups["power"]!!.value.split(",")),
                    matchResult.groups["ftp"]!!.value.toInt(),
                    matchResult.groups["vo2max"]!!.value.toFloat(),
                    matchResult.groups["aerobicTrainingEffect"]!!.value.toFloat(),
                    matchResult.groups["anaerobicTrainingEffect"]!!.value.toFloat(),
                    matchResult.groups["grit"]!!.value.toFloat(),
                    matchResult.groups["flow"]!!.value.toFloat(),
                    matchResult.groups["trainingLoad"]!!.value.toFloat(),
                )
            } else {
                return null
            }
        }
    }
}
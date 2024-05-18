package de.drtobiasprinz.summitbook.db.entities

import android.content.res.Resources
import androidx.room.Embedded
import androidx.room.Ignore
import de.drtobiasprinz.summitbook.R
import java.util.*

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
            return "garminid; " +
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
                    "trainingsload; "
        }

        fun getCsvDescription(resources: Resources): String {
            return "${resources.getString(R.string.optional)}; " +
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
                    "${resources.getString(R.string.optional)}; "
        }

        fun emptyLine(exportThirdPartyData: Boolean = true): String {
            return if (exportThirdPartyData) {
                ";;;;;;;;;;;"
            } else {
                ""
            }
        }
    }
}
package de.drtobiasprinz.summitbook.models

import androidx.room.Embedded
import androidx.room.Ignore
import java.util.*

class GarminData(
        var activityIds: MutableList<String>, var calories: Float, var averageHR: Float, var maxHR: Float,
        @Embedded var power: PowerData, var ftp: Int, var vo2max: Int,
        var aerobicTrainingEffect: Float, var anaerobicTrainingEffect: Float, var grit: Float, var flow: Float, var trainingLoad: Float,
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
}
package de.drtobiasprinz.summitbook.models

import java.util.*

class GarminActivityData(
        var activityIds: MutableList<String>, var calories: Float, var averageHR: Float, var maxHR: Float,
        var power: PowerData, var ftp: Int, var vo2max: Int,
        var aerobicTrainingEffect: Float, var anaerobicTrainingEffect: Float, var grit: Float, var flow: Float, var trainingLoad: Float) {

    val activityId = activityIds.first()
    val url = "https://connect.garmin.com/modern/activity/$activityId"
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
        val that = other as GarminActivityData?
        return activityIds == that?.activityIds
    }

    override fun hashCode(): Int {
        return Objects.hash(activityIds)
    }

}
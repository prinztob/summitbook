package de.drtobiasprinz.summitbook.models

import java.util.*

class GarminActivityData(
        val activityId: String, var calories: Float, var averageHR: Float, var maxHR: Float,
        var power: PowerData, var ftp: Int, var vo2max: Int,
        var aerobicTrainingEffect: Float, var anaerobicTrainingEffect: Float, var grit: Float, var flow: Float, var trainingLoad: Float) {

    override fun toString(): String {
        return activityId + ';' +
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
        return activityId == that?.activityId
    }

    override fun hashCode(): Int {
        return Objects.hash(activityId)
    }

}
package de.drtobiasprinz.summitbook.models

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.db.entities.CyclingDynamicsData
import de.drtobiasprinz.summitbook.db.entities.GarminData
import de.drtobiasprinz.summitbook.db.entities.GarminData.Companion.parseFromCsvFileLine
import de.drtobiasprinz.summitbook.db.entities.PowerData
import org.junit.Assert
import org.junit.Test
import java.util.*


class GarminDataTest {
    companion object {
        var garminData1 = GarminData(
            mutableListOf("4393181740"),
            952F,
            129F,
            155F,
            PowerData(75F, 100F, 50F, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
            236,
            53f,
            2.7f,
            0f,
            0f,
            0f,
            0f
        )
        var garminDataFromJsonExtracted = GarminData(
            mutableListOf("19618464792"),
            1008f,
            147f,
            172f,
            PowerData(
                211f,
                721f,
                257f,
                721,
                698,
                677,
                574,
                515,
                505,
                439,
                372,
                321,
                263,
                237,
                218,
                210
            ),
            261,
            54f,
            2.3f,
            3.8f,
            0f,
            0f,
            199.4f,
            cyclingDynamics = CyclingDynamicsData(
                49.49f,
                50.51f,
                82.0f,
                79.5f,
                23.5f,
                23f,
                114.3f,
                0.984f,
                4304,
                10,
                563,
                491,
                357,
                218,
                108,
                58,
                117,
                87,
                9,
                354,
                208,
                101,
                59,
                118,
                89,
                16,
                1184,
                908,
                93,
                0.0f
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun parseFromCsvFileLineUsingRegex() {
        Assert.assertEquals(
            garminData1,
            parseFromCsvFileLine(garminData1.getStringRepresentation(123456L))
        )
    }

    @Test
    @Throws(Exception::class)
    fun parseFromGarminJson() {
        val activityJsonResource = this.javaClass.classLoader?.getResource("activity_2.json")
        val activityExerciseSetJsonResource =
            this.javaClass.classLoader?.getResource("activity_2_exercise_set.json")
        if (activityJsonResource != null && activityExerciseSetJsonResource != null) {
            val activityJson = JsonParser.parseString(activityJsonResource.readText()) as JsonObject
            val exerciseSets =
                JsonParser.parseString(activityExerciseSetJsonResource.readText()) as JsonObject
            val garminData = GarminData.parseFromGarminJson(
                mutableListOf("19618464792"),
                activityJson,
                gsonExerciseSet = exerciseSets
            )
            Assert.assertEquals(garminData, garminDataFromJsonExtracted)
            Assert.assertEquals(garminData.cyclingDynamics.toString(), garminDataFromJsonExtracted.cyclingDynamics.toString())
        }
    }

}
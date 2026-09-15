package de.drtobiasprinz.summitbook.data.garmin

import com.google.gson.JsonObject
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import kotlin.math.roundToInt

/**
 * Pure JSON parsing helpers for Garmin Connect activity JSON files.
 * Extracted from GarminPythonExecutor so the data layer does not depend on the sync layer.
 */
object GarminJsonParser {

    fun roundToTwoDigits(value: Float): Float {
        return (value * 100f).roundToInt() / 100f
    }

    fun roundToTwoDigits(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }

    fun parseJsonObjectFromParentActivity(jsonObject: JsonObject): Summit {
        return Summit.parseFromGarminJson(jsonObject)
    }

    fun parseJsonObjectFromChildActivity(json: JsonObject, jsonParent: JsonObject?): Summit {
        val summaryDTO = json.getAsJsonObject("summaryDTO")
        return Summit.parseFromGarminJson(summaryDTO, jsonParent, json)
    }

    fun getJsonObjectEntryNotNull(jsonObject: JsonObject, key: String): Float {
        return if (jsonObject.has(key)) {
            if (jsonObject[key].isJsonNull) 0.0f else jsonObject[key].asFloat
        } else {
            0f
        }
    }
}

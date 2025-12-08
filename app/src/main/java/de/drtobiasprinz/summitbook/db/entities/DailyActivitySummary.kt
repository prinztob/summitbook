package de.drtobiasprinz.summitbook.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
import kotlin.math.roundToInt

@Entity(
    tableName = "daily_report", indices = [Index(value = ["activityId"], unique = true)]
)
data class DailyActivitySummary(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,

    @SerializedName("activityId") val activityId: Long,

    @SerializedName("date") val date: String,

    @SerializedName("distance") val distance: Double,

    @SerializedName("duration") val duration: Double,

    @SerializedName("elevationGain") val elevationGain: Double,

    @SerializedName("sportType") @JsonAdapter(SportTypeDeserializer::class) @ColumnInfo(defaultValue = "Other") val sportType: SportType
)

class SportTypeDeserializer : JsonDeserializer<SportType> {
    override fun deserialize(
        json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?
    ): SportType {
        if (json == null) return SportType.Other

        return when {
            json.isJsonPrimitive && json.asJsonPrimitive.isNumber -> {
                val sportTypeId = json.asInt
                SportType.getSportTypeFromGarminId(sportTypeId)
            }

            else -> SportType.Other
        }
    }
}

object DailyActivityHelper {

    fun findDailyActivitySummariesWhichWasNotAddedToSummits(
        dailyActivitySummaries: List<DailyActivitySummary>, summits: List<Summit>
    ): List<DailyActivitySummary> {
        val summitIds =
            summits.filter { it.garminData != null }.flatMap { it.garminData!!.activityIds }
        return dailyActivitySummaries.filter { it.activityId.toString() !in summitIds }
    }

    fun parseAsSummit(reports: List<DailyActivitySummary>): List<Summit> {
        return reports.map {
            Summit(
                date = Summit.parseDate(it.date),
                sportType = it.sportType,
                kilometers = it.distance / 1000,
                duration = it.duration.roundToInt(),
                elevationData = ElevationData(elevationGain = it.elevationGain.roundToInt())
            )
        }
    }
}
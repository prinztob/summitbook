package de.drtobiasprinz.summitbook.db

import androidx.room.TypeConverter
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.models.RoadType
import de.drtobiasprinz.summitbook.models.Surface
import java.util.Date


class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringArrayList(list: List<String>?): String? {
        return list?.joinToString(",")
    }

    @TypeConverter
    fun stringToStringArrayList(listAsString: String?): List<String>? {
        return listAsString?.split(",")
    }

    @TypeConverter
    fun stringToStringMutableList(listAsString: String?): MutableList<String>? {
        return listAsString?.split(",") as MutableList<String>?
    }

    @TypeConverter
    fun fromIntArrayList(list: MutableList<Int>?): String? {
        return list?.joinToString(",")
    }

    @TypeConverter
    fun stringToIntArrayList(listAsString: String?): MutableList<Int> {
        return if (listAsString == "") mutableListOf() else listAsString?.split(",")
            ?.map { it.toInt() } as MutableList<Int>
    }

    @TypeConverter
    fun fromSportType(sportType: SportType): String {
        return sportType.toString()
    }

    @TypeConverter
    fun stringToSportType(sportType: String?): SportType? {
        return sportType?.let { SportType.valueOf(it) }
    }

    @TypeConverter
    fun fromRoadTypeMap(map: Map<RoadType, Int>?): String? {
        return map?.entries?.joinToString(",") { "${it.key.name}:${it.value}" }
    }

    @TypeConverter
    fun stringToRoadTypeMap(mapAsString: String?): Map<RoadType, Int>? {
        return if (mapAsString.isNullOrEmpty()) {
            emptyMap()
        } else {
            mapAsString.split(",")
                .mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        try {
                            RoadType.valueOf(parts[0]) to parts[1].toInt()
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }
                .toMap()
        }
    }

    @TypeConverter
    fun fromSurfaceMap(map: Map<Surface, Int>?): String? {
        return map?.entries?.joinToString(",") { "${it.key.name}:${it.value}" }
    }

    @TypeConverter
    fun stringToSurfaceMap(mapAsString: String?): Map<Surface, Int>? {
        return if (mapAsString.isNullOrEmpty()) {
            emptyMap()
        } else {
            mapAsString.split(",")
                .mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        try {
                            Surface.valueOf(parts[0]) to parts[1].toInt()
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }
                .toMap()
        }
    }

}

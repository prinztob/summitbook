package de.drtobiasprinz.summitbook.db

import androidx.room.TypeConverter
import de.drtobiasprinz.summitbook.db.entities.SportType
import java.util.*


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
    fun fromIntArrayList(list: MutableList<Int>?): String? {
        return list?.joinToString(",")
    }

    @TypeConverter
    fun stringToIntArrayList(listAsString: String?): MutableList<Int> {
        return if (listAsString == "") mutableListOf() else listAsString?.split(",")?.map { it.toInt() } as MutableList<Int>
    }

    @TypeConverter
    fun fromSportType(sportType: SportType): String {
        return sportType.toString()
    }

    @TypeConverter
    fun stringToSportType(sportType: String?): SportType? {
        return sportType?.let { SportType.valueOf(it) }
    }

}

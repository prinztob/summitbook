package de.drtobiasprinz.summitbook.db

import androidx.room.TypeConverter
import de.drtobiasprinz.summitbook.db.entities.DailyEvent
import de.drtobiasprinz.summitbook.db.entities.SportType
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
    fun fromStringMutableList(list: MutableList<String>?): String? {
        return list?.joinToString(",")
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
    fun fromDailyEvents(dailyEvents: List<DailyEvent>): String {
        return dailyEvents.joinToString(separator = ";") { it.toString() }
    }

    @TypeConverter
    fun stringToDailyEvents(dailyEvents: String?): List<DailyEvent> {
        val events = mutableListOf<DailyEvent>()
        dailyEvents?.split(";")?.forEach {
            val entries = it.split(",")
            if (entries.size == 4) {
                events.add(
                    DailyEvent(
                        SportType.valueOf(entries[0]),
                        entries[1].toInt(),
                        entries[2].toInt(),
                        entries[3].toInt()
                    )
                )
            }
        }
        return events
    }
}

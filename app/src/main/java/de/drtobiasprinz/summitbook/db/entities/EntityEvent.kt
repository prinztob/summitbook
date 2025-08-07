package de.drtobiasprinz.summitbook.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.drtobiasprinz.summitbook.db.entities.Summit.Companion.DATE_FORMAT
import de.drtobiasprinz.summitbook.db.entities.Summit.Companion.parseDate
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity
data class EntityEvent(
    var date: Date,
    var description: String,
    var equipmentName: String,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    fun getDateAsString(): String {
        val dateFormat: DateFormat = SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH)
        return dateFormat.format(date)
    }

    fun getStringRepresentation(): String {
        return "${getDateAsString()};$description;$equipmentName\n"
    }

    fun alreadyExists(entityEvents: List<EntityEvent>): Boolean {
        return entityEvents.any {
            it.date == this.date &&
                    it.description == this.description &&
                    it.equipmentName == this.equipmentName
        }
    }

    companion object {
        private const val NUMBER_OF_ELEMENTS = 3
        fun getCsvHeadline(): String {
            return "Date;Description;EquipmentName\n"
        }

        private fun checkValidNumberOfElements(splitLine: List<String>) {
            if (splitLine.size != NUMBER_OF_ELEMENTS) {
                throw Exception(
                    "Line ${splitLine.joinToString { ";" }} has ${splitLine.size} number " +
                            "of elements. Expected are $NUMBER_OF_ELEMENTS"
                )
            }
        }

        fun parseFromCsvFileLine(
            line: String,
            entityEvents: MutableList<EntityEvent>,
            saveEntityEvent: (EntityEvent) -> Unit
        ): Boolean {
            val splitLine = line.split(";")
            checkValidNumberOfElements(splitLine)
            val entityEvent = EntityEvent(
                parseDate(splitLine[0]),
                splitLine[1],
                splitLine[2]
            )
            return if (entityEvent.alreadyExists(entityEvents)) {
                false
            } else {
                saveEntityEvent(entityEvent)
                entityEvents.add(entityEvent)
                true
            }
        }
    }
}
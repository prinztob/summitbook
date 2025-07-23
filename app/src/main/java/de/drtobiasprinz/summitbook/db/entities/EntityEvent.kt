package de.drtobiasprinz.summitbook.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.drtobiasprinz.summitbook.db.entities.Summit.Companion.DATE_FORMAT
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
}
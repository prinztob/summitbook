package de.drtobiasprinz.summitbook.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class SavedLocation(
    var name: String,
    var lat: Double,
    var lng: Double,
    @ColumnInfo(defaultValue = "-16777216")
    var color: Int = android.graphics.Color.BLACK,
    var createdAt: Long = System.currentTimeMillis()
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}

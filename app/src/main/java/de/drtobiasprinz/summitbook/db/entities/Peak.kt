package de.drtobiasprinz.summitbook.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Peak(
    var name: String,
    var elevation: Int = 0
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
